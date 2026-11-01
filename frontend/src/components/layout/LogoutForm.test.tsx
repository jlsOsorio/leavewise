import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, test, vi } from 'vitest';

import { LogoutForm } from './LogoutForm';

const logout = vi.hoisted(() => vi.fn());
vi.mock('@/lib/auth/actions', () => ({ logout }));

beforeEach(() => {
  logout.mockReset().mockReturnValue(new Promise(() => {}));
});

describe('LogoutForm', () => {
  test('logs the user out', async () => {
    render(<LogoutForm />);
    const user = userEvent.setup();

    await user.click(screen.getByRole('button', { name: 'Log out' }));

    expect(logout).toHaveBeenCalledOnce();
  });

  test('disables the button while logging out', async () => {
    render(<LogoutForm />);
    const user = userEvent.setup();

    await user.click(screen.getByRole('button', { name: 'Log out' }));

    expect(screen.getByRole('button', { name: 'Log out' })).toBeDisabled();
  });
});
