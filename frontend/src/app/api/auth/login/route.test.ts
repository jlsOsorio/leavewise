// @vitest-environment node
import { http, HttpResponse } from 'msw';
import { NextRequest } from 'next/server';
import { describe, expect, test } from 'vitest';

import { server } from '@/test/msw/server';
import { POST } from './route';

const loginRequest = () =>
  new NextRequest('http://localhost:3000/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email: 'ana@example.pt', password: 'secret123' }),
  });

describe('POST /api/auth/login', () => {
  test('sets both httpOnly cookies and returns only display fields', async () => {
    const response = await POST(loginRequest());

    expect(response.status).toBe(200);
    expect(await response.json()).toEqual({
      user: { name: 'Ana Silva', role: 'USER' },
    });
    const at = response.cookies.get('lw_at');
    const rt = response.cookies.get('lw_rt');
    expect(at).toMatchObject({
      value: 'access-jwt',
      httpOnly: true,
      sameSite: 'lax',
      path: '/',
    });
    expect(rt).toMatchObject({ value: 'refresh-token', httpOnly: true });
    expect(at?.maxAge).toBeGreaterThan(0);
    expect(rt?.maxAge).toBeGreaterThan(at?.maxAge ?? 0);
  });

  test('passes a backend 401 through without setting cookies', async () => {
    server.use(
      http.post(`${process.env.BACKEND_URL}/api/auth/login`, () =>
        HttpResponse.json(
          { status: 401, code: 'INVALID_CREDENTIALS' },
          {
            status: 401,
            headers: { 'Content-Type': 'application/problem+json' },
          },
        ),
      ),
    );

    const response = await POST(loginRequest());

    expect(response.status).toBe(401);
    expect(await response.json()).toMatchObject({
      code: 'INVALID_CREDENTIALS',
    });
    expect(response.cookies.getAll()).toHaveLength(0);
  });

  test('returns 500 INTERNAL_ERROR when the backend is unreachable', async () => {
    server.use(
      http.post(`${process.env.BACKEND_URL}/api/auth/login`, () =>
        HttpResponse.error(),
      ),
    );

    const response = await POST(loginRequest());

    expect(response.status).toBe(500);
    expect(await response.json()).toMatchObject({ code: 'INTERNAL_ERROR' });
  });

  test('returns 400 VALIDATION_ERROR for a body that is not JSON', async () => {
    const request = new NextRequest('http://localhost:3000/api/auth/login', {
      method: 'POST',
      body: 'not json',
    });

    const response = await POST(request);

    expect(response.status).toBe(400);
    expect(await response.json()).toMatchObject({ code: 'VALIDATION_ERROR' });
  });

  test('rejects an invalid body with field errors before calling the backend', async () => {
    const request = new NextRequest('http://localhost:3000/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email: 'ana', password: '' }),
    });

    const response = await POST(request);

    expect(response.status).toBe(400);
    expect(await response.json()).toMatchObject({
      code: 'VALIDATION_ERROR',
      errors: [
        { field: 'email', message: 'Enter a valid email address.' },
        { field: 'password', message: 'Enter your password.' },
      ],
    });
  });
});
