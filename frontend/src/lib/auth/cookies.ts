import type { NextResponse } from 'next/server';

import type { TokenPair } from './token-pair';

export const ACCESS_COOKIE = 'lw_at';
export const REFRESH_COOKIE = 'lw_rt';

type CookieOptions = NonNullable<Parameters<NextResponse['cookies']['set']>[2]>;

function cookieOptions(maxAge: number): CookieOptions {
  return {
    httpOnly: true,
    sameSite: 'lax',
    secure: process.env.COOKIE_SECURE === 'true',
    path: '/',
    maxAge,
  };
}

// Seconds until `expiresAt`, so the cookie lives exactly as long as its token
function secondsUntil(expiresAt: string): number {
  return Math.max(0, Math.floor((Date.parse(expiresAt) - Date.now()) / 1000));
}

export function setAuthCookies(response: NextResponse, pair: TokenPair): void {
  response.cookies.set(
    ACCESS_COOKIE,
    pair.accessToken,
    cookieOptions(secondsUntil(pair.accessTokenExpiresAt)),
  );
  response.cookies.set(
    REFRESH_COOKIE,
    pair.refreshToken,
    cookieOptions(secondsUntil(pair.refreshTokenExpiresAt)),
  );
}

export function clearAuthCookies(response: NextResponse): void {
  response.cookies.set(ACCESS_COOKIE, '', cookieOptions(0));
  response.cookies.set(REFRESH_COOKIE, '', cookieOptions(0));
}
