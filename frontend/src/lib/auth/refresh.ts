import { createHash } from 'node:crypto';

import { postAuth } from './backend';
import type { TokenPair } from './token-pair';

const REUSE_WINDOW_MS = 10_000;

type Entry = { pair: Promise<TokenPair | null>; settledAt?: number };

// Single-flight refresh per token, in memory: requests racing with the same
// refresh token share one backend call, and its result is reused for the
// grace window. Relies on a single Next.js instance (SPEC §0).
const entries = new Map<string, Entry>();

export function refreshTokens(refreshToken: string): Promise<TokenPair | null> {
  evictExpired();
  const key = createHash('sha256').update(refreshToken).digest('hex');

  const existing = entries.get(key);
  if (existing) {
    return existing.pair;
  }

  const entry: Entry = { pair: callRefresh(refreshToken) };
  entries.set(key, entry);
  void entry.pair.then((pair) => {
    if (pair) {
      entry.settledAt = Date.now();
    } else {
      entries.delete(key); // never cache a failure
    }
  });
  return entry.pair;
}

async function callRefresh(refreshToken: string): Promise<TokenPair | null> {
  try {
    const response = await postAuth('/refresh', { refreshToken });
    return response.ok ? ((await response.json()) as TokenPair) : null;
  } catch {
    return null;
  }
}

function evictExpired(): void {
  const now = Date.now();
  for (const [key, entry] of entries) {
    if (
      entry.settledAt !== undefined &&
      now - entry.settledAt > REUSE_WINDOW_MS
    ) {
      entries.delete(key);
    }
  }
}
