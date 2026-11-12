// @vitest-environment node
import { http, HttpResponse } from 'msw';
import { beforeEach, describe, expect, test, vi } from 'vitest';

import { server } from '@/test/msw/server';
import {
  createMunicipality,
  deleteMunicipality,
  updateMunicipality,
} from './actions';

vi.mock('next/headers', () => ({
  cookies: async () => ({
    get: (name: string) =>
      name === 'lw_at' ? { name, value: 'access-jwt' } : undefined,
  }),
}));

const redirect = vi.hoisted(() =>
  vi.fn((url: string) => {
    throw new Error(`NEXT_REDIRECT ${url}`);
  }),
);
vi.mock('next/navigation', () => ({ redirect }));

const revalidatePath = vi.hoisted(() => vi.fn());
vi.mock('next/cache', () => ({ revalidatePath }));

const MUNICIPALITIES_URL = `${process.env.BACKEND_URL}/api/municipalities`;
const input = { name: 'Braga', district: 'Braga', country: '' };

const problem = (status: number, body: object) =>
  HttpResponse.json(
    { status, ...body },
    { status, headers: { 'Content-Type': 'application/problem+json' } },
  );

beforeEach(() => {
  revalidatePath.mockClear();
});

describe('createMunicipality', () => {
  test('creates it and refreshes the list', async () => {
    const result = await createMunicipality(input);

    expect(result).toEqual({ ok: true });
    expect(revalidatePath).toHaveBeenCalledWith('/admin/municipalities');
  });

  test('returns field errors for invalid input', async () => {
    const result = await createMunicipality({ ...input, name: ' ' });

    expect(result).toMatchObject({
      ok: false,
      error: {
        code: 'VALIDATION_ERROR',
        fieldErrors: { name: 'Enter the name.' },
      },
    });
  });

  test('returns the duplicate error without refreshing the list', async () => {
    server.use(
      http.post(MUNICIPALITIES_URL, () => problem(409, { code: 'DUPLICATE' })),
    );

    const result = await createMunicipality(input);

    expect(result).toMatchObject({
      ok: false,
      error: { code: 'DUPLICATE', message: 'This item already exists.' },
    });
    expect(revalidatePath).not.toHaveBeenCalled();
  });

  test('redirects to the login page when the session has expired', async () => {
    server.use(
      http.post(MUNICIPALITIES_URL, () =>
        problem(401, { code: 'UNAUTHENTICATED' }),
      ),
    );

    await expect(createMunicipality(input)).rejects.toThrow(
      'NEXT_REDIRECT /login?session=expired',
    );
  });
});

describe('updateMunicipality', () => {
  test('updates it and refreshes the list', async () => {
    const result = await updateMunicipality(5, input);

    expect(result).toEqual({ ok: true });
  });

  test('rejects an id that is not a positive integer', async () => {
    await expect(updateMunicipality(-1, input)).rejects.toThrow();
  });
});

describe('deleteMunicipality', () => {
  test('deletes it and refreshes the list', async () => {
    const result = await deleteMunicipality(5);

    expect(result).toEqual({ ok: true });
    expect(revalidatePath).toHaveBeenCalledWith('/admin/municipalities');
  });

  test('returns the in-use error', async () => {
    server.use(
      http.delete(`${MUNICIPALITIES_URL}/:id`, () =>
        problem(409, { code: 'RESOURCE_IN_USE' }),
      ),
    );

    const result = await deleteMunicipality(5);

    expect(result).toMatchObject({
      ok: false,
      error: { code: 'RESOURCE_IN_USE' },
    });
  });
});
