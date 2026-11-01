import { render, screen } from '@testing-library/react';
import userEvent, { type UserEvent } from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { toast } from 'sonner';
import { beforeEach, expect, test, vi } from 'vitest';

import { Toaster } from '@/components/ui/sonner';
import { page } from '@/test/msw/handlers';
import { server } from '@/test/msw/server';
import { RegisterForm } from './RegisterForm';

const router = vi.hoisted(() => ({ replace: vi.fn() }));
vi.mock('next/navigation', () => ({ useRouter: () => router }));

const register = vi.hoisted(() => vi.fn());
vi.mock('@/lib/auth/actions', () => ({ register }));

beforeEach(() => {
  router.replace.mockClear();
  register.mockReset().mockResolvedValue({ ok: true });
  toast.dismiss();
});

async function chooseCompany(user: UserEvent, name: string) {
  await user.click(screen.getByRole('combobox', { name: /^Company/ }));
  await user.click(await screen.findByRole('option', { name }));
}

async function chooseLocation(user: UserEvent, name: string) {
  await user.click(await screen.findByRole('combobox', { name: /^Location/ }));
  await user.click(await screen.findByRole('option', { name }));
}

async function fillValidForm(user: UserEvent) {
  await user.type(screen.getByLabelText(/^Full name/), 'Ana Ferreira');
  await user.type(screen.getByLabelText(/^Email/), 'ana@example.pt');
  await user.type(screen.getByLabelText(/^Password/), 'secret123');
  await chooseCompany(user, 'Acme Lda');
  await chooseLocation(user, 'Porto');
}

const submit = (user: UserEvent) =>
  user.click(screen.getByRole('button', { name: 'Create account' }));

test('creates the account and goes to the home page', async () => {
  render(<RegisterForm />);
  const user = userEvent.setup();
  await fillValidForm(user);

  await submit(user);

  await vi.waitFor(() => expect(router.replace).toHaveBeenCalledWith('/'));
  expect(register).toHaveBeenCalledWith(
    expect.objectContaining({ companyId: 1, companyLocationId: 2 }),
  );
});

test('confirms the new account with a toast', async () => {
  render(
    <>
      <RegisterForm />
      <Toaster />
    </>,
  );
  const user = userEvent.setup();
  await fillValidForm(user);

  await submit(user);

  expect(
    await screen.findByText('Account created. Welcome to LeaveWise!'),
  ).toBeInTheDocument();
});

test('shows a taken email under the email field', async () => {
  register.mockResolvedValue({
    ok: false,
    error: {
      code: 'EMAIL_TAKEN',
      message: 'An account with this email already exists.',
      fieldErrors: { email: 'An account with this email already exists.' },
    },
  });
  render(<RegisterForm />);
  const user = userEvent.setup();
  await fillValidForm(user);

  await submit(user);

  expect(screen.getByLabelText(/^Email/)).toHaveAccessibleDescription(
    'An account with this email already exists.',
  );
  expect(router.replace).not.toHaveBeenCalled();
});

test('clears the location when another company is chosen', async () => {
  server.use(
    http.get('/api/auth/companies', () =>
      HttpResponse.json(
        page([
          { id: 1, name: 'Acme Lda' },
          { id: 2, name: 'Beta SA' },
        ]),
      ),
    ),
  );
  render(<RegisterForm />);
  const user = userEvent.setup();
  await chooseCompany(user, 'Acme Lda');
  await chooseLocation(user, 'Porto');

  await chooseCompany(user, 'Beta SA');

  expect(
    await screen.findByRole('combobox', { name: /^Location/ }),
  ).toHaveTextContent('Choose your location');
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
