// @vitest-environment node
import { http, HttpResponse } from 'msw';
import { NextRequest } from 'next/server';
import { describe, expect, test } from 'vitest';

import { page } from '@/test/msw/handlers';
import { server } from '@/test/msw/server';
import { GET } from './route';

const LOCATIONS_URL = `${process.env.BACKEND_URL}/api/auth/companies/:id/locations`;

function getLocations(id: string) {
  return GET(
    new NextRequest(`http://localhost:3000/api/auth/companies/${id}/locations`),
    { params: Promise.resolve({ id }) },
  );
}

describe('GET /api/auth/companies/[id]/locations', () => {
  test('returns all locations of the company in one page sorted by name', async () => {
    server.use(
      http.get(LOCATIONS_URL, ({ params, request }) => {
        const query = new URL(request.url).searchParams;
        const expected =
          params.id === '7' &&
          query.get('size') === '100' &&
          query.get('sort') === 'name,asc';
        return HttpResponse.json(
          page(expected ? [{ id: 3, name: 'Braga' }] : []),
        );
      }),
    );

    const response = await getLocations('7');

    expect(await response.json()).toMatchObject({
      content: [{ id: 3, name: 'Braga' }],
    });
  });

  test('passes a backend 404 through', async () => {
    server.use(
      http.get(LOCATIONS_URL, () =>
        HttpResponse.json(
          { status: 404, code: 'NOT_FOUND' },
          {
            status: 404,
            headers: { 'Content-Type': 'application/problem+json' },
          },
        ),
      ),
    );

    const response = await getLocations('99');

    expect(response.status).toBe(404);
    expect(await response.json()).toMatchObject({ code: 'NOT_FOUND' });
  });

  test('returns 500 INTERNAL_ERROR when the backend is unreachable', async () => {
    server.use(http.get(LOCATIONS_URL, () => HttpResponse.error()));

    const response = await getLocations('7');

    expect(response.status).toBe(500);
    expect(await response.json()).toMatchObject({ code: 'INTERNAL_ERROR' });
  });
});
