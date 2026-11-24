// @vitest-environment node
import { http, HttpResponse } from 'msw';
import { beforeEach, describe, expect, test, vi } from 'vitest';

import { server } from '@/test/msw/server';
import { createLocation, deleteLocation, updateLocation } from './actions';

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

const BACKEND = `${process.env.BACKEND_URL}/api`;
const input = { name: 'Sede', address: '', municipalityId: 5 };

const problem = (status: number, body: object) =>
  HttpResponse.json(
    { status, ...body },
    { status, headers: { 'Content-Type': 'application/problem+json' } },
  );

beforeEach(() => {
  revalidatePath.mockClear();
});

describe('createLocation', () => {
  test('creates it in the company and refreshes the company page', async () => {
    const result = await createLocation(3, input);

    expect(result).toEqual({ ok: true });
    expect(revalidatePath).toHaveBeenCalledWith('/admin/companies/3');
  });

  test('returns field errors for invalid input', async () => {
    const result = await createLocation(3, { ...input, name: ' ' });

    expect(result).toMatchObject({
      ok: false,
      error: {
        code: 'VALIDATION_ERROR',
        fieldErrors: { name: 'Enter the name.' },
      },
    });
  });

  test('returns the duplicate error without refreshing the page', async () => {
    server.use(
      http.post(`${BACKEND}/companies/:companyId/locations`, () =>
        problem(409, { code: 'DUPLICATE' }),
      ),
    );

    const result = await createLocation(3, input);

    expect(result).toMatchObject({
      ok: false,
      error: { code: 'DUPLICATE', message: 'This item already exists.' },
    });
    expect(revalidatePath).not.toHaveBeenCalled();
  });

  test('returns not found when the municipality no longer exists', async () => {
    server.use(
      http.post(`${BACKEND}/companies/:companyId/locations`, () =>
        problem(404, { code: 'NOT_FOUND' }),
      ),
    );

    const result = await createLocation(3, input);

    expect(result).toMatchObject({ ok: false, error: { code: 'NOT_FOUND' } });
  });

  test('redirects to the login page when the session has expired', async () => {
    server.use(
      http.post(`${BACKEND}/companies/:companyId/locations`, () =>
        problem(401, { code: 'UNAUTHENTICATED' }),
      ),
    );

    await expect(createLocation(3, input)).rejects.toThrow(
      'NEXT_REDIRECT /login?session=expired',
    );
  });
});

describe('updateLocation', () => {
  test('updates it and refreshes the company page', async () => {
    const result = await updateLocation(3, 11, input);

    expect(result).toEqual({ ok: true });
    expect(revalidatePath).toHaveBeenCalledWith('/admin/companies/3');
  });

  test('rejects an id that is not a positive integer', async () => {
    await expect(updateLocation(3, -1, input)).rejects.toThrow();
  });
});

describe('deleteLocation', () => {
  test('deletes it and refreshes the company page', async () => {
    const result = await deleteLocation(3, 11);

    expect(result).toEqual({ ok: true });
    expect(revalidatePath).toHaveBeenCalledWith('/admin/companies/3');
  });

  test('returns the in-use error for a location with users', async () => {
    server.use(
      http.delete(`${BACKEND}/locations/:id`, () =>
        problem(409, { code: 'RESOURCE_IN_USE' }),
      ),
    );

    const result = await deleteLocation(3, 11);

    expect(result).toMatchObject({
      ok: false,
      error: { code: 'RESOURCE_IN_USE' },
    });
  });
});
