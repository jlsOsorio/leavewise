// @vitest-environment node
import { http, HttpResponse } from 'msw';
import { beforeEach, describe, expect, test, vi } from 'vitest';

import { server } from '@/test/msw/server';
import { login, logout, register } from './actions';

type StoredCookie = { value: string; maxAge?: number; httpOnly?: boolean };

const cookieJar = vi.hoisted(() => new Map<string, StoredCookie>());
vi.mock('next/headers', () => ({
  cookies: async () => ({
    get: (name: string) => {
      const cookie = cookieJar.get(name);
      return cookie && { name, value: cookie.value };
    },
    set: (name: string, value: string, options: Omit<StoredCookie, 'value'>) =>
      cookieJar.set(name, { value, ...options }),
  }),
}));

const redirect = vi.hoisted(() =>
  vi.fn((url: string) => {
    throw new Error(`NEXT_REDIRECT ${url}`);
  }),
);
vi.mock('next/navigation', () => ({ redirect }));

const LOGIN_URL = `${process.env.BACKEND_URL}/api/auth/login`;
const REGISTRATION_URL = `${process.env.BACKEND_URL}/api/auth/registration`;
const LOGOUT_URL = `${process.env.BACKEND_URL}/api/auth/logout`;
const credentials = { email: 'ana@example.pt', password: 'secret123' };

const problem = (status: number, body: object) =>
  HttpResponse.json(
    { status, ...body },
    { status, headers: { 'Content-Type': 'application/problem+json' } },
  );

beforeEach(() => {
  cookieJar.clear();
});

describe('login', () => {
  test('stores both tokens in httpOnly cookies that live as long as the tokens', async () => {
    const result = await login(credentials);

    expect(result).toEqual({ ok: true });
    const at = cookieJar.get('lw_at');
    const rt = cookieJar.get('lw_rt');
    expect(at).toMatchObject({ value: 'access-jwt', httpOnly: true });
    expect(rt).toMatchObject({ value: 'refresh-token', httpOnly: true });
    expect(rt?.maxAge).toBeGreaterThan(at?.maxAge ?? 0);
  });

  test('returns the backend error message without setting cookies', async () => {
    server.use(
      http.post(LOGIN_URL, () => problem(401, { code: 'INVALID_CREDENTIALS' })),
    );

    const result = await login(credentials);

    expect(result).toMatchObject({
      ok: false,
      error: {
        code: 'INVALID_CREDENTIALS',
        message: 'Incorrect email or password.',
      },
    });
    expect(cookieJar.size).toBe(0);
  });

  test('returns field errors for invalid input before calling the backend', async () => {
    server.use(
      http.post(LOGIN_URL, () => problem(500, { code: 'INTERNAL_ERROR' })),
    );

    const result = await login({ email: 'ana', password: '' });

    expect(result).toMatchObject({
      ok: false,
      error: {
        code: 'VALIDATION_ERROR',
        fieldErrors: {
          email: 'Enter a valid email address.',
          password: 'Enter your password.',
        },
      },
    });
  });

  test('returns INTERNAL_ERROR when the backend is unreachable', async () => {
    server.use(http.post(LOGIN_URL, () => HttpResponse.error()));

    const result = await login(credentials);

    expect(result).toMatchObject({
      ok: false,
      error: { code: 'INTERNAL_ERROR' },
    });
  });
});

describe('register', () => {
  const registration = {
    name: 'Ana Ferreira',
    email: 'ana@example.pt',
    password: 'secret123',
    companyId: 1,
    companyLocationId: 2,
  };

  test('stores both tokens in cookies', async () => {
    const result = await register(registration);

    expect(result).toEqual({ ok: true });
    expect(cookieJar.get('lw_at')?.value).toBe('access-jwt');
    expect(cookieJar.get('lw_rt')?.value).toBe('refresh-token');
  });

  test.each([
    ['EMAIL_TAKEN', 'email', 'An account with this email already exists.'],
    ['NIF_TAKEN', 'nif', 'An account with this NIF already exists.'],
    ['LOCATION_COMPANY_MISMATCH', 'companyLocationId', expect.any(String)],
  ])('returns %s as an error on %s', async (code, field, message) => {
    server.use(http.post(REGISTRATION_URL, () => problem(409, { code })));

    const result = await register(registration);

    expect(result).toMatchObject({
      ok: false,
      error: { code, fieldErrors: { [field]: message } },
    });
    expect(cookieJar.size).toBe(0);
  });

  test('returns an unknown company as a form error', async () => {
    server.use(
      http.post(REGISTRATION_URL, () => problem(404, { code: 'NOT_FOUND' })),
    );

    const result = await register(registration);

    expect(result).toEqual({
      ok: false,
      error: {
        code: 'NOT_FOUND',
        message: 'The requested item was not found.',
        fieldErrors: undefined,
      },
    });
  });

  test('returns field errors for invalid input before calling the backend', async () => {
    server.use(
      http.post(REGISTRATION_URL, () =>
        problem(500, { code: 'INTERNAL_ERROR' }),
      ),
    );

    const result = await register({ ...registration, password: 'short' });

    expect(result).toMatchObject({
      ok: false,
      error: {
        code: 'VALIDATION_ERROR',
        fieldErrors: { password: 'Use at least 8 characters.' },
      },
    });
  });
});

describe('logout', () => {
  test('clears both cookies and redirects to /login', async () => {
    cookieJar.set('lw_at', { value: 'access-jwt' });
    cookieJar.set('lw_rt', { value: 'refresh-token' });

    await expect(logout()).rejects.toThrow('NEXT_REDIRECT /login');

    expect(cookieJar.get('lw_at')).toMatchObject({ value: '', maxAge: 0 });
    expect(cookieJar.get('lw_rt')).toMatchObject({ value: '', maxAge: 0 });
  });

  test('clears both cookies when the backend is unreachable', async () => {
    cookieJar.set('lw_rt', { value: 'refresh-token' });
    server.use(http.post(LOGOUT_URL, () => HttpResponse.error()));

    await expect(logout()).rejects.toThrow('NEXT_REDIRECT /login');

    expect(cookieJar.get('lw_rt')).toMatchObject({ value: '', maxAge: 0 });
  });
});
