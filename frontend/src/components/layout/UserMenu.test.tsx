import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, test, vi } from 'vitest';

import { UserMenu } from './UserMenu';

const logout = vi.hoisted(() => vi.fn());
vi.mock('@/lib/auth/actions', () => ({ logout }));

beforeEach(() => {
  logout.mockReset().mockReturnValue(new Promise(() => {}));
});

function renderMenu(name = 'Ana Silva') {
  render(<UserMenu name={name} email="ana@acme.pt" company="Acme Lda" />);
}

async function openMenu() {
  const user = userEvent.setup();
  await user.click(screen.getByRole('button', { name: /user menu/i }));
  return user;
}

describe('UserMenu', () => {
  test('uses the first and last name for the initials', () => {
    renderMenu('ana maria  silva');

    expect(screen.getByText('AS')).toBeInTheDocument();
  });

  test('uses a single initial for a single name', () => {
    renderMenu('Ana');

    expect(screen.getByText('A')).toBeInTheDocument();
  });

  test('shows the email when opened', async () => {
    renderMenu();

    await openMenu();

    expect(screen.getByText('ana@acme.pt')).toBeInTheDocument();
  });

  test('logs the user out', async () => {
    renderMenu();
    const user = await openMenu();

    await user.click(screen.getByRole('menuitem', { name: 'Log out' }));

    expect(logout).toHaveBeenCalledOnce();
  });

  test('disables log out while logging out', async () => {
    renderMenu();
    const user = await openMenu();

    await user.click(screen.getByRole('menuitem', { name: 'Log out' }));

    expect(screen.getByRole('menuitem', { name: 'Log out' })).toHaveAttribute(
      'aria-disabled',
      'true',
    );
  });
});
