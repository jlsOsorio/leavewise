import { http, HttpResponse, type RequestHandler } from 'msw';

import type { Me } from '@/lib/api/me';
import type { Page } from '@/lib/api/page';
import type { TokenPair } from '@/lib/auth/token-pair';
import { fakeJwt } from '@/test/jwt';

const backend = (path: string) => `${process.env.BACKEND_URL}/api${path}`;

export function tokenPair(): TokenPair {
  const now = Date.now();
  return {
    accessToken: 'access-jwt',
    accessTokenExpiresAt: new Date(now + 15 * 60_000).toISOString(),
    refreshToken: 'refresh-token',
    refreshTokenExpiresAt: new Date(now + 7 * 86_400_000).toISOString(),
    user: {
      id: 1,
      name: 'Ana Silva',
      email: 'ana@example.pt',
      role: 'USER',
      companyId: 1,
      companyLocationId: 1,
    },
  };
}

export function refreshedPair(): TokenPair {
  return {
    ...tokenPair(),
    accessToken: fakeJwt('USER', 15 * 60_000),
    refreshToken: 'rotated-refresh-token',
  };
}

export function page<T>(content: T[], totalElements = content.length): Page<T> {
  return { content, page: 0, size: 20, totalElements, totalPages: 1 };
}

const LOCATIONS = [
  { id: 1, name: 'Lisboa' },
  { id: 2, name: 'Porto' },
];

// Default happy-path mocks of the backend
export const handlers: RequestHandler[] = [
  http.post(backend('/auth/refresh'), () => HttpResponse.json(refreshedPair())),
  http.post(backend('/auth/login'), () => HttpResponse.json(tokenPair())),
  http.post(backend('/auth/registration'), () =>
    HttpResponse.json(tokenPair(), { status: 201 }),
  ),
  http.post(
    backend('/auth/logout'),
    () => new HttpResponse(null, { status: 204 }),
  ),
  http.get(backend('/auth/companies'), () =>
    HttpResponse.json(page([{ id: 1, name: 'Acme Lda' }])),
  ),
  http.get(backend('/auth/companies/:id/locations'), () =>
    HttpResponse.json(page(LOCATIONS)),
  ),
  http.get(backend('/me'), () =>
    HttpResponse.json({
      id: 1,
      name: 'Ana Silva',
      email: 'ana@example.pt',
      nif: null,
      contact: null,
      role: 'USER',
      company: { id: 1, name: 'Acme Lda' },
      companyLocation: LOCATIONS[0],
    } satisfies Me),
  ),

  http.post(backend('/municipalities'), async ({ request }) =>
    HttpResponse.json(
      { id: 10, ...((await request.json()) as object) },
      { status: 201 },
    ),
  ),
  http.put(backend('/municipalities/:id'), async ({ params, request }) =>
    HttpResponse.json({
      id: Number(params.id),
      ...((await request.json()) as object),
    }),
  ),
  http.delete(
    backend('/municipalities/:id'),
    () => new HttpResponse(null, { status: 204 }),
  ),

  // Route Handlers
  http.get('/api/auth/companies', () =>
    HttpResponse.json(page([{ id: 1, name: 'Acme Lda' }])),
  ),
  http.get('/api/auth/companies/:id/locations', () =>
    HttpResponse.json(page(LOCATIONS)),
  ),
];
