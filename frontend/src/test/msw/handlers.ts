import { http, HttpResponse, type RequestHandler } from 'msw';

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

  // Route Handlers
  http.get('/api/auth/companies', () =>
    HttpResponse.json(page([{ id: 1, name: 'Acme Lda' }])),
  ),
];
