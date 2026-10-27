// @vitest-environment node
import { http, HttpResponse } from 'msw';
import { beforeEach, describe, expect, test, vi } from 'vitest';

import { server } from '@/test/msw/server';
import { ApiError } from './api-error';
import { API_MEDIA_TYPE } from './media-type';
import { serverApi } from './server';

const cookieJar = vi.hoisted(() => new Map<string, string>());
vi.mock('next/headers', () => ({
  cookies: async () => ({
    get: (name: string) => {
      const value = cookieJar.get(name);
      return value === undefined ? undefined : { name, value };
    },
  }),
}));

const ME_URL = `${process.env.BACKEND_URL}/api/me`;

const problem = (status: number, body: object) =>
  HttpResponse.json(
    { status, ...body },
    { status, headers: { 'Content-Type': 'application/problem+json' } },
  );

beforeEach(() => {
  cookieJar.clear();
  cookieJar.set('lw_at', 'access-jwt');
});

describe('serverApi', () => {
  test('returns the JSON of an authenticated, versioned request', async () => {
    server.use(
      http.get(ME_URL, ({ request }) =>
        request.headers.get('authorization') === 'Bearer access-jwt' &&
        request.headers.get('accept') === API_MEDIA_TYPE
          ? HttpResponse.json({ name: 'Ana Silva' })
          : problem(400, { code: 'VALIDATION_ERROR' }),
      ),
    );

    await expect(serverApi('/me')).resolves.toEqual({ name: 'Ana Silva' });
  });

  test('sends the body as versioned JSON', async () => {
    server.use(
      http.patch(ME_URL, async ({ request }) =>
        request.headers.get('content-type') === API_MEDIA_TYPE
          ? HttpResponse.json(await request.json())
          : problem(415, { code: 'UNSUPPORTED_MEDIA_TYPE' }),
      ),
    );

    const result = await serverApi('/me', {
      method: 'PATCH',
      body: { contact: '912345678' },
    });

    expect(result).toEqual({ contact: '912345678' });
  });

  test('adds query parameters and skips undefined ones', async () => {
    server.use(
      http.get(`${process.env.BACKEND_URL}/api/holidays`, ({ request }) =>
        HttpResponse.json(new URL(request.url).search),
      ),
    );

    const search = await serverApi('/holidays', {
      query: { year: 2026, page: 0, q: undefined },
    });

    expect(search).toBe('?year=2026&page=0');
  });

  test('returns undefined for 204 No Content', async () => {
    server.use(
      http.delete(ME_URL, () => new HttpResponse(null, { status: 204 })),
    );

    await expect(
      serverApi<void>('/me', { method: 'DELETE' }),
    ).resolves.toBeUndefined();
  });

  test('redirects to /login on 401', async () => {
    server.use(
      http.get(ME_URL, () => problem(401, { code: 'UNAUTHENTICATED' })),
    );

    await expect(serverApi('/me')).rejects.toMatchObject({
      digest: expect.stringContaining('/login'),
    });
  });

  test('throws an ApiError built from the ProblemDetail', async () => {
    const errors = [{ field: 'contact', message: 'size must be at most 12' }];
    server.use(
      http.patch(ME_URL, () =>
        problem(400, {
          code: 'VALIDATION_ERROR',
          detail: 'Invalid request content.',
          errors,
        }),
      ),
    );

    const call = serverApi('/me', {
      method: 'PATCH',
      body: { contact: '9'.repeat(13) },
    });

    await expect(call).rejects.toEqual(
      new ApiError(400, 'VALIDATION_ERROR', 'Invalid request content.', errors),
    );
  });

  test('throws INTERNAL_ERROR when the error body is not a ProblemDetail', async () => {
    server.use(
      http.get(ME_URL, () => new HttpResponse('Bad Gateway', { status: 502 })),
    );

    await expect(serverApi('/me')).rejects.toMatchObject({
      status: 502,
      code: 'INTERNAL_ERROR',
    });
  });
});
