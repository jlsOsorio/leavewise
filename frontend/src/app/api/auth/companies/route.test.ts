// @vitest-environment node
import { http, HttpResponse } from 'msw';
import { NextRequest } from 'next/server';
import { describe, expect, test } from 'vitest';

import { page } from '@/test/msw/handlers';
import { server } from '@/test/msw/server';
import { GET } from './route';

const COMPANIES_URL = `${process.env.BACKEND_URL}/api/auth/companies`;

const search = (query = '') =>
  GET(new NextRequest(`http://localhost:3000/api/auth/companies${query}`));

describe('GET /api/auth/companies', () => {
  test('returns the first 20 matches for q sorted by name', async () => {
    server.use(
      http.get(COMPANIES_URL, ({ request }) => {
        const params = new URL(request.url).searchParams;
        const expected =
          params.get('q') === 'acme' &&
          params.get('page') === '0' &&
          params.get('size') === '20' &&
          params.get('sort') === 'name,asc';
        return HttpResponse.json(
          page(expected ? [{ id: 1, name: 'Acme Lda' }] : []),
        );
      }),
    );

    const response = await search('?q=acme');

    expect(await response.json()).toMatchObject({
      content: [{ id: 1, name: 'Acme Lda' }],
    });
  });

  test('lists all companies when q is missing', async () => {
    server.use(
      http.get(COMPANIES_URL, ({ request }) =>
        HttpResponse.json(
          page(
            new URL(request.url).searchParams.get('q') === ''
              ? [{ id: 2, name: 'Beta SA' }]
              : [],
          ),
        ),
      ),
    );

    const response = await search();

    expect(await response.json()).toMatchObject({
      content: [{ id: 2, name: 'Beta SA' }],
    });
  });

  test('passes a backend error through', async () => {
    server.use(
      http.get(COMPANIES_URL, () =>
        HttpResponse.json(
          { status: 400, code: 'VALIDATION_ERROR' },
          {
            status: 400,
            headers: { 'Content-Type': 'application/problem+json' },
          },
        ),
      ),
    );

    const response = await search('?q=x');

    expect(response.status).toBe(400);
    expect(await response.json()).toMatchObject({ code: 'VALIDATION_ERROR' });
  });

  test('returns 500 INTERNAL_ERROR when the backend is unreachable', async () => {
    server.use(http.get(COMPANIES_URL, () => HttpResponse.error()));

    const response = await search('?q=x');

    expect(response.status).toBe(500);
    expect(await response.json()).toMatchObject({ code: 'INTERNAL_ERROR' });
  });
});
