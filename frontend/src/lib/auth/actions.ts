'use server';

import { cookies } from 'next/headers';
import { redirect } from 'next/navigation';

import { type ActionResult, validationFailure } from '@/lib/api/action-result';
import {
  type LoginInput,
  loginSchema,
  type RegistrationInput,
  registrationSchema,
} from '@/lib/schemas/auth';
import { exchangeForSession, postAuth } from './backend';
import { clearAuthCookies, REFRESH_COOKIE } from './cookies';

// Returns instead of redirecting: the form navigates to `next` itself, since an action's
// redirect rejects the promise the form awaits
export async function login(input: LoginInput): Promise<ActionResult> {
  const parsed = loginSchema.safeParse(input);
  if (!parsed.success) {
    return validationFailure(parsed.error);
  }
  return exchangeForSession('/login', parsed.data);
}

const CODE_FIELDS: Partial<Record<string, keyof RegistrationInput>> = {
  EMAIL_TAKEN: 'email',
  NIF_TAKEN: 'nif',
  LOCATION_COMPANY_MISMATCH: 'companyLocationId',
};

export async function register(
  input: RegistrationInput,
): Promise<ActionResult> {
  const parsed = registrationSchema.safeParse(input);
  if (!parsed.success) {
    return validationFailure(parsed.error);
  }

  const result = await exchangeForSession('/registration', parsed.data);
  const field = result.ok ? undefined : CODE_FIELDS[result.error.code];
  if (result.ok || !field) {
    return result;
  }
  return {
    ok: false,
    error: { ...result.error, fieldErrors: { [field]: result.error.message } },
  };
}

export async function logout(): Promise<void> {
  const cookieStore = await cookies();
  const refreshToken = cookieStore.get(REFRESH_COOKIE)?.value;
  if (refreshToken) {
    await postAuth('/logout', { refreshToken }).catch(() => undefined);
  }
  clearAuthCookies(cookieStore);
  redirect('/login');
}
