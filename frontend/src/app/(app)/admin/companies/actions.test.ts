// @vitest-environment node
import { http, HttpResponse } from 'msw';
import { beforeEach, describe, expect, test, vi } from 'vitest';

import { server } from '@/test/msw/server';
import { createCompany, deleteCompany, updateCompany } from './actions';

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

const COMPANIES_URL = `${process.env.BACKEND_URL}/api/companies`;
const input = { name: 'Acme Lda' };

const problem = (status: number, body: object) =>
  HttpResponse.json(
    { status, ...body },
    { status, headers: { 'Content-Type': 'application/problem+json' } },
  );

beforeEach(() => {
  revalidatePath.mockClear();
});

describe('createCompany', () => {
  test('creates it and refreshes the list', async () => {
    const result = await createCompany(input);

    expect(result).toEqual({ ok: true });
    expect(revalidatePath).toHaveBeenCalledWith('/admin/companies');
  });

  test('returns field errors for invalid input', async () => {
    const result = await createCompany({ name: ' ' });

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
      http.post(COMPANIES_URL, () => problem(409, { code: 'DUPLICATE' })),
    );

    const result = await createCompany(input);

    expect(result).toMatchObject({
      ok: false,
      error: { code: 'DUPLICATE', message: 'This item already exists.' },
    });
    expect(revalidatePath).not.toHaveBeenCalled();
  });

  test('redirects to the login page when the session has expired', async () => {
    server.use(
      http.post(COMPANIES_URL, () => problem(401, { code: 'UNAUTHENTICATED' })),
    );

    await expect(createCompany(input)).rejects.toThrow(
      'NEXT_REDIRECT /login?session=expired',
    );
  });
});

describe('updateCompany', () => {
  test('updates it and refreshes the list', async () => {
    const result = await updateCompany(3, input);

    expect(result).toEqual({ ok: true });
    expect(revalidatePath).toHaveBeenCalledWith('/admin/companies');
  });

  test('rejects an id that is not a positive integer', async () => {
    await expect(updateCompany(-1, input)).rejects.toThrow();
  });
});

describe('deleteCompany', () => {
  test('deletes it and refreshes the list', async () => {
    const result = await deleteCompany(3);

    expect(result).toEqual({ ok: true });
    expect(revalidatePath).toHaveBeenCalledWith('/admin/companies');
  });

  test('returns the in-use error for a company with users', async () => {
    server.use(
      http.delete(`${COMPANIES_URL}/:id`, () =>
        problem(409, { code: 'RESOURCE_IN_USE' }),
      ),
    );

    const result = await deleteCompany(3);

    expect(result).toMatchObject({
      ok: false,
      error: { code: 'RESOURCE_IN_USE' },
    });
  });
});
