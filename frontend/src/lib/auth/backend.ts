import { cookies } from 'next/headers';
import { type NextResponse } from 'next/server';

import { type ActionResult, apiFailure } from '@/lib/api/action-result';
import { ApiError } from '@/lib/api/api-error';
import { API_MEDIA_TYPE } from '@/lib/api/media-type';
import { forwardGet } from '@/lib/api/proxy-get';
import { setAuthCookies } from './cookies';
import type { TokenPair } from './token-pair';

export function postAuth(path: string, body: unknown): Promise<Response> {
  return fetch(`${process.env.BACKEND_URL}/api/auth${path}`, {
    method: 'POST',
    headers: { Accept: API_MEDIA_TYPE, 'Content-Type': API_MEDIA_TYPE },
    body: JSON.stringify(body),
  });
}

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

export function getPublicAuth(
  path: string,
  query: Record<string, string>,
): Promise<NextResponse> {
  return forwardGet(`/auth${path}`, query);
}
