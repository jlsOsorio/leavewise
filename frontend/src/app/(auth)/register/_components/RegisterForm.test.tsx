import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, expect, test, vi } from 'vitest';

import { RegisterForm } from './RegisterForm';

vi.mock('next/navigation', () => ({ useRouter: () => ({ replace: vi.fn() }) }));

const register = vi.hoisted(() => vi.fn());
vi.mock('@/lib/auth/actions', () => ({ register }));

beforeEach(() => {
  register.mockReset();
});

test('shows every validation error without submitting', async () => {
  render(<RegisterForm />);
  const user = userEvent.setup();

  await user.type(screen.getByLabelText('NIF'), '123456780');
  await user.click(screen.getByRole('button', { name: 'Create account' }));

  for (const message of [
    'Enter your name.',
    'Enter a valid email address.',
    'Use at least 8 characters.',
    'Enter a valid NIF.',
    'Choose your company.',
    'Choose your location.',
  ]) {
    expect(await screen.findByText(message)).toBeVisible();
  }
  expect(register).not.toHaveBeenCalled();
});

test('marks every field except NIF and contact as required', () => {
  render(<RegisterForm />);

  for (const label of [
    /^Full name/,
    /^Email/,
    /^Password/,
    /^Company/,
    /^Location/,
  ]) {
    expect(screen.getByLabelText(label)).toBeRequired();
  }
  for (const label of [/^NIF/, /^Contact/]) {
    expect(screen.getByLabelText(label)).not.toBeRequired();
  }
});
