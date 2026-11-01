import { render, screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, test, vi } from 'vitest';

import { server } from '@/test/msw/server';
import { HeaderUser } from './HeaderUser';

vi.mock('next/headers', () => ({
  cookies: async () => ({
    get: (name: string) => ({ name, value: 'access-jwt' }),
  }),
}));

const ME_URL = `${process.env.BACKEND_URL}/api/me`;

function withName(name: string) {
  server.use(
    http.get(ME_URL, () =>
      HttpResponse.json({ name, company: { id: 1, name: 'Acme Lda' } }),
    ),
  );
}

describe('HeaderUser', () => {
  test('shows the signed-in user name and company from /me', async () => {
    render(await HeaderUser());

    expect(screen.getByText('Ana Silva')).toBeInTheDocument();
    expect(screen.getByText('Acme Lda')).toBeInTheDocument();
  });

  test('uses the first and last name for the initials', async () => {
    withName('ana maria  silva');

    render(await HeaderUser());

    expect(screen.getByText('AS')).toBeInTheDocument();
  });

  test('uses a single initial for a single name', async () => {
    withName('Ana');

    render(await HeaderUser());

    expect(screen.getByText('A')).toBeInTheDocument();
  });
});
