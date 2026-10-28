// @vitest-environment node
import { http, HttpResponse } from 'msw';
import { getRedirectUrl } from 'next/experimental/testing/server';
import { NextRequest, type NextResponse } from 'next/server';
import { describe, expect, test } from 'vitest';

import { fakeJwt } from '@/test/jwt';
import { refreshedPair } from '@/test/msw/handlers';
import { server } from '@/test/msw/server';
import { proxy } from './proxy';

const ORIGIN = 'http://localhost:3000';
const REFRESH_URL = `${process.env.BACKEND_URL}/api/auth/refresh`;

// Each test gets its own refresh token: successful refreshes stay cached for 10 s
let tokenSeq = 0;
const newRefreshToken = () => `refresh-${++tokenSeq}`;

function request(
  path: string,
  {
    cookies = {},
    method = 'GET',
    origin,
  }: {
    cookies?: Record<string, string>;
    method?: string;
    origin?: string;
  } = {},
): NextRequest {
  const headers = new Headers({ host: 'localhost:3000' });
  const cookie = Object.entries(cookies)
    .map(([name, value]) => `${name}=${value}`)
    .join('; ');
  if (cookie) headers.set('cookie', cookie);
  if (origin) headers.set('origin', origin);
  return new NextRequest(`${ORIGIN}${path}`, { method, headers });
}

const failRefresh = () =>
  server.use(
    http.post(REFRESH_URL, () =>
      HttpResponse.json(
        { status: 401, code: 'INVALID_REFRESH_TOKEN' },
        {
          status: 401,
          headers: { 'Content-Type': 'application/problem+json' },
        },
      ),
    ),
  );

const continues = (response: NextResponse) =>
  response.headers.get('x-middleware-next') === '1';
const forwardedCookie = (response: NextResponse) =>
  response.headers.get('x-middleware-request-cookie') ?? '';

describe('proxy refresh', () => {
  test('continues without refreshing while the access token has more than 60 s left', async () => {
    const response = await proxy(
      request('/calendar', {
        cookies: {
          lw_at: fakeJwt('USER', 5 * 60_000),
          lw_rt: newRefreshToken(),
        },
      }),
    );

    expect([continues(response), response.cookies.getAll()]).toEqual([
      true,
      [],
    ]);
  });

  test('refreshes an access token that expires within 60 s, setting and forwarding both cookies', async () => {
    const response = await proxy(
      request('/calendar', {
        cookies: { lw_at: fakeJwt('USER', 30_000), lw_rt: newRefreshToken() },
      }),
    );

    expect(response.cookies.get('lw_rt')?.value).toBe('rotated-refresh-token');
    expect(forwardedCookie(response)).toContain('lw_rt=rotated-refresh-token');
  });

  test('refreshes when only the refresh token is present', async () => {
    const response = await proxy(
      request('/calendar', { cookies: { lw_rt: newRefreshToken() } }),
    );

    expect(continues(response)).toBe(true);
    expect(response.cookies.get('lw_at')?.value).toBeTruthy();
  });

  test('keeps the cookies untouched when the refresh fails but the access token is still valid', async () => {
    failRefresh();

    const response = await proxy(
      request('/calendar', {
        cookies: { lw_at: fakeJwt('USER', 30_000), lw_rt: newRefreshToken() },
      }),
    );

    expect([continues(response), response.cookies.getAll()]).toEqual([
      true,
      [],
    ]);
  });

  test('redirects to /login and clears the cookies when the refresh fails with an expired access token', async () => {
    failRefresh();

    const response = await proxy(
      request('/calendar?year=2026', {
        cookies: { lw_at: fakeJwt('USER', -1000), lw_rt: newRefreshToken() },
      }),
    );

    expect(getRedirectUrl(response)).toBe(
      `${ORIGIN}/login?next=%2Fcalendar%3Fyear%3D2026`,
    );
    expect(response.cookies.get('lw_at')?.maxAge).toBe(0);
  });

  test('shares one refresh between concurrent requests with the same refresh token', async () => {
    let calls = 0;
    server.use(
      http.post(REFRESH_URL, () =>
        ++calls === 1
          ? HttpResponse.json(refreshedPair())
          : HttpResponse.json(
              { code: 'INVALID_REFRESH_TOKEN' },
              { status: 401 },
            ),
      ),
    );
    const cookies = { lw_rt: newRefreshToken() };

    const responses = await Promise.all([
      proxy(request('/a', { cookies })),
      proxy(request('/b', { cookies })),
    ]);

    expect(responses.map((r) => r.cookies.get('lw_rt')?.value)).toEqual([
      'rotated-refresh-token',
      'rotated-refresh-token',
    ]);
  });

  test('reuses a successful refresh for a later request carrying the old refresh token', async () => {
    const cookies = { lw_rt: newRefreshToken() };
    await proxy(request('/a', { cookies }));
    failRefresh();

    const response = await proxy(request('/b', { cookies }));

    expect(response.cookies.get('lw_rt')?.value).toBe('rotated-refresh-token');
  });
});

describe('proxy access', () => {
  test('answers an unauthenticated /api request with 401 UNAUTHENTICATED', async () => {
    const response = await proxy(request('/api/imports'));

    expect(response.status).toBe(401);
    expect(await response.json()).toMatchObject({ code: 'UNAUTHENTICATED' });
  });

  test('redirects an unauthenticated page request to /login with next', async () => {
    const response = await proxy(request('/vacations'));

    expect(getRedirectUrl(response)).toBe(`${ORIGIN}/login?next=%2Fvacations`);
  });

  test('clears both cookies when /login is opened', async () => {
    const response = await proxy(
      request('/login', {
        cookies: {
          lw_at: fakeJwt('USER', 5 * 60_000),
          lw_rt: newRefreshToken(),
        },
      }),
    );

    expect([
      response.cookies.get('lw_at')?.maxAge,
      response.cookies.get('lw_rt')?.maxAge,
    ]).toEqual([0, 0]);
  });

  test('keeps the cookies the login action sets on its POST to /login', async () => {
    const response = await proxy(
      request('/login', { method: 'POST', origin: ORIGIN }),
    );

    expect(continues(response)).toBe(true);
    expect(response.cookies.getAll()).toHaveLength(0);
  });

  test('lets an anonymous user reach /register', async () => {
    const response = await proxy(request('/register'));

    expect(continues(response)).toBe(true);
  });

  test('redirects a USER away from /admin', async () => {
    const response = await proxy(
      request('/admin/users', {
        cookies: { lw_at: fakeJwt('USER', 5 * 60_000) },
      }),
    );

    expect(getRedirectUrl(response)).toBe(`${ORIGIN}/`);
  });

  test('lets an ADMIN reach /admin', async () => {
    const response = await proxy(
      request('/admin/users', {
        cookies: { lw_at: fakeJwt('ADMIN', 5 * 60_000) },
      }),
    );

    expect(continues(response)).toBe(true);
  });
});

describe('proxy CSRF', () => {
  test('rejects a POST to /api from another origin with 403 FORBIDDEN', async () => {
    const response = await proxy(
      request('/api/imports', {
        method: 'POST',
        origin: 'https://evil.example',
      }),
    );

    expect(response.status).toBe(403);
    expect(await response.json()).toMatchObject({ code: 'FORBIDDEN' });
  });

  test('rejects a POST to /api without an Origin header', async () => {
    const response = await proxy(request('/api/imports', { method: 'POST' }));

    expect(response.status).toBe(403);
  });

  test('lets a same-origin POST to /api through', async () => {
    const response = await proxy(
      request('/api/imports', {
        method: 'POST',
        origin: ORIGIN,
        cookies: { lw_at: fakeJwt('USER', 5 * 60_000) },
      }),
    );

    expect(continues(response)).toBe(true);
  });

  test('does not check the Origin of a GET to /api', async () => {
    const response = await proxy(
      request('/api/auth/companies', { origin: 'https://evil.example' }),
    );

    expect(continues(response)).toBe(true);
  });
});
