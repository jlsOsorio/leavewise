// @vitest-environment node
import { http, HttpResponse } from 'msw';
import { NextRequest } from 'next/server';
import { describe, expect, test, vi } from 'vitest';

import { page } from '@/test/msw/handlers';
import { server } from '@/test/msw/server';
import { GET } from './route';

vi.mock('next/headers', () => ({
  cookies: async () => ({
    get: (name: string) =>
      name === 'lw_at' ? { name, value: 'access-jwt' } : undefined,
  }),
}));

const MUNICIPALITIES_URL = `${process.env.BACKEND_URL}/api/municipalities`;
const braga = { id: 5, name: 'Braga', district: 'Braga', country: 'Portugal' };

const search = (query = '') =>
  GET(
    new NextRequest(`http://localhost:3000/api/admin/municipalities${query}`),
  );

describe('GET /api/admin/municipalities', () => {
  test('returns the first 20 matches for q sorted by name, with the session token', async () => {
    server.use(
      http.get(MUNICIPALITIES_URL, ({ request }) => {
        const params = new URL(request.url).searchParams;
        const expected =
          request.headers.get('Authorization') === 'Bearer access-jwt' &&
          params.get('q') === 'bra' &&
          params.get('page') === '0' &&
          params.get('size') === '20' &&
          params.get('sort') === 'name,asc';
        return HttpResponse.json(page(expected ? [braga] : []));
      }),
    );

    const response = await search('?q=bra');

    expect(await response.json()).toMatchObject({ content: [braga] });
  });

  test('passes a backend error through', async () => {
    server.use(
      http.get(MUNICIPALITIES_URL, () =>
        HttpResponse.json(
          { status: 403, code: 'FORBIDDEN' },
          {
            status: 403,
            headers: { 'Content-Type': 'application/problem+json' },
          },
        ),
      ),
    );

    const response = await search('?q=x');

    expect(response.status).toBe(403);
    expect(await response.json()).toMatchObject({ code: 'FORBIDDEN' });
  });

  test('clears both session cookies when the backend returns 401', async () => {
    server.use(
      http.get(MUNICIPALITIES_URL, () =>
        HttpResponse.json(
          { status: 401, code: 'UNAUTHENTICATED' },
          {
            status: 401,
            headers: { 'Content-Type': 'application/problem+json' },
          },
        ),
      ),
    );

    const response = await search('?q=x');

    expect(response.status).toBe(401);
    expect(response.cookies.get('lw_at')).toMatchObject({
      value: '',
      maxAge: 0,
    });
    expect(response.cookies.get('lw_rt')).toMatchObject({
      value: '',
      maxAge: 0,
    });
  });

  test('returns 500 INTERNAL_ERROR when the backend is unreachable', async () => {
    server.use(http.get(MUNICIPALITIES_URL, () => HttpResponse.error()));

    const response = await search('?q=x');

    expect(response.status).toBe(500);
    expect(await response.json()).toMatchObject({ code: 'INTERNAL_ERROR' });
  });
});
