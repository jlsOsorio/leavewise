import { cookies } from 'next/headers';
import { NextResponse } from 'next/server';

import { type ActionResult, apiFailure } from '@/lib/api/action-result';
import { ApiError } from '@/lib/api/api-error';
import { API_MEDIA_TYPE } from '@/lib/api/media-type';
import { PROBLEM_MEDIA_TYPE } from '@/lib/api/problem-detail';
import { problemResponse } from '@/lib/api/problem-response';
import { setAuthCookies } from './cookies';
import type { TokenPair } from './token-pair';

export function postAuth(path: string, body: unknown): Promise<Response> {
  return fetch(`${process.env.BACKEND_URL}/api/auth${path}`, {
    method: 'POST',
    headers: { Accept: API_MEDIA_TYPE, 'Content-Type': API_MEDIA_TYPE },
    body: JSON.stringify(body),
  });
}

// Posts validated credentials and stores the returned tokens as cookies; for Server Actions only
export async function exchangeForSession(
  path: string,
  body: unknown,
): Promise<ActionResult> {
  let backend: Response;
  try {
    backend = await postAuth(path, body);
  } catch {
    return apiFailure(new ApiError(500, 'INTERNAL_ERROR'));
  }

  if (!backend.ok) {
    return apiFailure(await ApiError.from(backend));
  }

  setAuthCookies(await cookies(), (await backend.json()) as TokenPair);
  return { ok: true };
}

// Proxies a public GET (`/auth/...`, no token) for the register pickers; the response passes
// through as is, errors included
export async function getPublicAuth(
  path: string,
  query: Record<string, string>,
): Promise<NextResponse> {
  const url = new URL(`/api/auth${path}`, process.env.BACKEND_URL);
  for (const [key, value] of Object.entries(query)) {
    url.searchParams.set(key, value);
  }

  let backend: Response;
  try {
    backend = await fetch(url, { headers: { Accept: API_MEDIA_TYPE } });
  } catch {
    return problemResponse(500, 'Internal Server Error', 'INTERNAL_ERROR');
  }
  return new NextResponse(backend.body, {
    status: backend.status,
    headers: {
      'Content-Type': backend.headers.get('Content-Type') ?? PROBLEM_MEDIA_TYPE,
    },
  });
}
