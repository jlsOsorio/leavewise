import { cookies } from 'next/headers';
import { redirect } from 'next/navigation';

import { ACCESS_COOKIE } from '@/lib/auth/cookies';
import { ApiError } from './api-error';
import { API_MEDIA_TYPE } from './media-type';

type Query = Record<string, string | number | boolean | undefined>;

type ServerApiOptions = {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  body?: unknown;
  query?: Query;
};

export async function serverApi<T>(
  path: string,
  { method = 'GET', body, query }: ServerApiOptions = {},
): Promise<T> {
  const accessToken = (await cookies()).get(ACCESS_COOKIE)?.value;
  const headers = new Headers({ Accept: API_MEDIA_TYPE });
  if (accessToken) {
    headers.set('Authorization', `Bearer ${accessToken}`);
  }
  if (body !== undefined) {
    headers.set('Content-Type', API_MEDIA_TYPE);
  }

  const response = await fetch(backendUrl(path, query), {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });

  if (response.status === 401) {
    redirect('/login');
  }
  if (!response.ok) {
    throw await ApiError.from(response);
  }
  if (response.status === 204) {
    return undefined as T; // callers of 204 endpoints use serverApi<void>
  }
  return (await response.json()) as T;
}

function backendUrl(path: string, query: Query = {}): URL {
  const url = new URL(`/api${path}`, process.env.BACKEND_URL);
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined) {
      url.searchParams.set(key, String(value));
    }
  }
  return url;
}
