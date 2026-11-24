import { cookies } from 'next/headers';
import { NextResponse } from 'next/server';

import { ACCESS_COOKIE, clearAuthCookies } from '@/lib/auth/cookies';
import { API_MEDIA_TYPE } from './media-type';
import { PROBLEM_MEDIA_TYPE } from './problem-detail';
import { problemResponse } from './problem-response';

type Query = Record<string, string>;

// Proxies a backend GET for a Route Handler, passing the response (or error) through
export async function forwardGet(
  path: string,
  query: Query,
  headers: Headers = new Headers(),
): Promise<NextResponse> {
  const url = new URL(`/api${path}`, process.env.BACKEND_URL);
  for (const [key, value] of Object.entries(query)) {
    url.searchParams.set(key, value);
  }
  headers.set('Accept', API_MEDIA_TYPE);

  let backend: Response;
  try {
    backend = await fetch(url, { headers });
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

export async function getWithSession(
  path: string,
  query: Query,
): Promise<NextResponse> {
  const accessToken = (await cookies()).get(ACCESS_COOKIE)?.value;
  const headers = new Headers();
  if (accessToken) {
    headers.set('Authorization', `Bearer ${accessToken}`);
  }
  const response = await forwardGet(path, query, headers);
  if (response.status === 401) {
    clearAuthCookies(response.cookies);
  }
  return response;
}
