// @vitest-environment node
import { http, HttpResponse } from 'msw';
import { NextRequest, type NextResponse } from 'next/server';
import { describe, expect, test } from 'vitest';

import { server } from '@/test/msw/server';
import { POST } from './route';

const logoutRequest = (cookie?: string) =>
  new NextRequest('http://localhost:3000/api/auth/logout', {
    method: 'POST',
    headers: cookie ? { Cookie: cookie } : {},
  });

const authCookies = (response: NextResponse) =>
  ['lw_at', 'lw_rt'].map((name) => {
    const cookie = response.cookies.get(name);
    return { name, value: cookie?.value, maxAge: cookie?.maxAge };
  });

const cleared = [
  { name: 'lw_at', value: '', maxAge: 0 },
  { name: 'lw_rt', value: '', maxAge: 0 },
];

describe('POST /api/auth/logout', () => {
  test('clears both cookies after revoking the refresh token', async () => {
    const response = await POST(
      logoutRequest('lw_at=access-jwt; lw_rt=refresh-token'),
    );

    expect(authCookies(response)).toEqual(cleared);
  });

  test('responds 204 with no body', async () => {
    const response = await POST(logoutRequest('lw_rt=refresh-token'));

    expect(response.status).toBe(204);
  });

  test('clears both cookies when the backend is unreachable', async () => {
    server.use(
      http.post(`${process.env.BACKEND_URL}/api/auth/logout`, () =>
        HttpResponse.error(),
      ),
    );

    const response = await POST(
      logoutRequest('lw_at=access-jwt; lw_rt=refresh-token'),
    );

    expect(authCookies(response)).toEqual(cleared);
  });

  test('clears both cookies without a refresh token', async () => {
    const response = await POST(logoutRequest());

    expect(authCookies(response)).toEqual(cleared);
  });
});
