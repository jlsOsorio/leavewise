import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, test, vi } from 'vitest';

import { LoginForm } from './LoginForm';

const router = vi.hoisted(() => ({ replace: vi.fn() }));
vi.mock('next/navigation', () => ({ useRouter: () => router }));

const login = vi.hoisted(() => vi.fn());
vi.mock('@/lib/auth/actions', () => ({ login }));

beforeEach(() => {
  router.replace.mockClear();
  login.mockReset().mockResolvedValue({ ok: true });
});

async function signIn(email = 'ana@example.pt', password = 'secret123') {
  const user = userEvent.setup();
  if (email) await user.type(screen.getByLabelText('Email'), email);
  if (password) await user.type(screen.getByLabelText('Password'), password);
  await user.click(screen.getByRole('button', { name: 'Sign in' }));
}

describe('LoginForm', () => {
  test('navigates to next after signing in', async () => {
    render(<LoginForm next="/vacations?year=2026" />);

    await signIn();

    await vi.waitFor(() =>
      expect(router.replace).toHaveBeenCalledWith('/vacations?year=2026'),
    );
  });

  test('navigates to / when next points to another site', async () => {
    render(<LoginForm next="//evil.example" />);

    await signIn();

    await vi.waitFor(() => expect(router.replace).toHaveBeenCalledWith('/'));
  });

  test('shows the message for wrong credentials', async () => {
    login.mockResolvedValue({
      ok: false,
      error: {
        code: 'INVALID_CREDENTIALS',
        message: 'Incorrect email or password.',
      },
    });
    render(<LoginForm />);

    await signIn();

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Incorrect email or password.',
    );
  });

  test('shows validation errors without submitting', async () => {
    render(<LoginForm />);

    await signIn('ana', '');

    expect(
      await screen.findByText('Enter a valid email address.'),
    ).toBeVisible();
    expect(screen.getByLabelText('Email')).toHaveAccessibleDescription(
      'Enter a valid email address.',
    );
    expect(screen.getByLabelText('Password')).toHaveAccessibleDescription(
      'Enter your password.',
    );
    expect(login).not.toHaveBeenCalled();
  });
  test('shows a field error returned by the server under its field', async () => {
    login.mockResolvedValue({
      ok: false,
      error: {
        code: 'VALIDATION_ERROR',
        message: 'Check the highlighted fields.',
        fieldErrors: { email: 'Email is too long.' },
      },
    });
    render(<LoginForm />);

    await signIn();

    expect(await screen.findByText('Email is too long.')).toBeVisible();
    expect(router.replace).not.toHaveBeenCalled();
  });

  test('shows a generic error when the server cannot be reached', async () => {
    login.mockRejectedValue(new TypeError('Failed to fetch'));
    render(<LoginForm />);

    await signIn();

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Something went wrong. Please try again.',
    );
  });
});
