import type { Role } from './token-pair';

export type AccessClaims = {
  exp: number;
  role: Role;
};

// Reads the access token's payload WITHOUT verifying it: for UX decisions only
// (refresh timing, redirects)
export function decodeAccessToken(token: string): AccessClaims | null {
  try {
    const payload: unknown = JSON.parse(
      Buffer.from(token.split('.')[1] ?? '', 'base64url').toString('utf8'),
    );
    if (
      typeof payload === 'object' &&
      payload !== null &&
      'exp' in payload &&
      typeof payload.exp === 'number' &&
      'role' in payload &&
      (payload.role === 'USER' || payload.role === 'ADMIN')
    ) {
      return { exp: payload.exp, role: payload.role };
    }
    return null;
  } catch {
    return null;
  }
}

// Milliseconds until the token expires (negative when already expired)
export function msUntilExpiry(claims: AccessClaims, now = Date.now()): number {
  return claims.exp * 1000 - now;
}
