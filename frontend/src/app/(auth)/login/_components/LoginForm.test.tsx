import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { beforeEach, describe, expect, test, vi } from 'vitest';

import { server } from '@/test/msw/server';
import { LoginForm } from './LoginForm';

const router = vi.hoisted(() => ({ replace: vi.fn(), refresh: vi.fn() }));
vi.mock('next/navigation', () => ({ useRouter: () => router }));

beforeEach(() => {
  router.replace.mockClear();
  router.refresh.mockClear();
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
    server.use(
      http.post('/api/auth/login', () =>
        HttpResponse.json(
          { status: 401, code: 'INVALID_CREDENTIALS' },
          { status: 401 },
        ),
      ),
    );
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
    expect(router.replace).not.toHaveBeenCalled();
  });
});
