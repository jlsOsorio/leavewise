import { render, screen } from '@testing-library/react';
import { expect, test, vi } from 'vitest';

import { HeaderUser } from './HeaderUser';

vi.mock('next/headers', () => ({
  cookies: async () => ({
    get: (name: string) => ({ name, value: 'access-jwt' }),
  }),
}));

test('shows the signed-in user name and company from /me', async () => {
  render(await HeaderUser());

  expect(screen.getByText('Ana Silva')).toBeInTheDocument();
  expect(screen.getByText('Acme Lda')).toBeInTheDocument();
});
