'use server';

import { cookies } from 'next/headers';
import { redirect } from 'next/navigation';

import { type ActionResult, validationFailure } from '@/lib/api/action-result';
import { type LoginInput, loginSchema } from '@/lib/schemas/auth';
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

// Always ends the session locally; the backend revoke is best effort (it is idempotent)
export async function logout(): Promise<void> {
  const cookieStore = await cookies();
  const refreshToken = cookieStore.get(REFRESH_COOKIE)?.value;
  if (refreshToken) {
    await postAuth('/logout', { refreshToken }).catch(() => undefined);
  }
  clearAuthCookies(cookieStore);
  redirect('/login');
}
