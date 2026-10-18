import { http, HttpResponse, type RequestHandler } from 'msw';

import type { TokenPair } from '@/lib/auth/token-pair';

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

// Default happy-path mocks of the backend
export const handlers: RequestHandler[] = [
  http.post(backend('/auth/login'), () => HttpResponse.json(tokenPair())),
  http.post(backend('/auth/registration'), () =>
    HttpResponse.json(tokenPair(), { status: 201 }),
  ),
  http.post(
    backend('/auth/logout'),
    () => new HttpResponse(null, { status: 204 }),
  ),
];
