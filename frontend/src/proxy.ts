import { type NextRequest, NextResponse } from 'next/server';

import { problemResponse } from '@/lib/api/problem-response';
import {
  ACCESS_COOKIE,
  clearAuthCookies,
  REFRESH_COOKIE,
  setAuthCookies,
} from '@/lib/auth/cookies';
import {
  type AccessClaims,
  decodeAccessToken,
  msUntilExpiry,
} from '@/lib/auth/jwt';
import { refreshTokens } from '@/lib/auth/refresh';
import type { TokenPair } from '@/lib/auth/token-pair';

// Refresh when the access token is missing or expires within this margin
const REFRESH_MARGIN_MS = 60_000;
const PUBLIC_PAGES = new Set(['/login', '/register']);
const SAFE_METHODS = new Set(['GET', 'HEAD']);

type Session = { claims: AccessClaims; refreshed?: TokenPair };

export async function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const isApi = pathname.startsWith('/api/');

  if (isApi && !SAFE_METHODS.has(request.method) && !isSameOrigin(request)) {
    return problemResponse(403, 'Forbidden', 'FORBIDDEN');
  }
  if (pathname.startsWith('/api/auth/') || PUBLIC_PAGES.has(pathname)) {
    const response = NextResponse.next();
    if (pathname === '/login') {
      clearAuthCookies(response); // the login page always starts a clean session
    }
    return response;
  }

  const session = await resolveSession(request);
  if (!session) {
    const response = isApi
      ? problemResponse(401, 'Unauthorized', 'UNAUTHENTICATED')
      : NextResponse.redirect(loginUrl(request));
    clearAuthCookies(response);
    return response;
  }

  if (pathname.startsWith('/admin') && session.claims.role !== 'ADMIN') {
    return withRefreshedCookies(
      NextResponse.redirect(new URL('/', request.url)),
      session,
    );
  }

  if (!session.refreshed) {
    return NextResponse.next();
  }
  // Forward the new tokens to this request's Server Components and Route Handlers
  request.cookies.set(ACCESS_COOKIE, session.refreshed.accessToken);
  request.cookies.set(REFRESH_COOKIE, session.refreshed.refreshToken);
  return withRefreshedCookies(
    NextResponse.next({ request: { headers: request.headers } }),
    session,
  );
}

async function resolveSession(request: NextRequest): Promise<Session | null> {
  const accessToken = request.cookies.get(ACCESS_COOKIE)?.value;
  const refreshToken = request.cookies.get(REFRESH_COOKIE)?.value;
  const claims = accessToken ? decodeAccessToken(accessToken) : null;
  const remainingMs = claims ? msUntilExpiry(claims) : 0;

  if (refreshToken && remainingMs < REFRESH_MARGIN_MS) {
    const refreshed = await refreshTokens(refreshToken);
    const refreshedClaims = refreshed
      ? decodeAccessToken(refreshed.accessToken)
      : null;
    if (refreshed && refreshedClaims) {
      return { claims: refreshedClaims, refreshed };
    }
  }
  // No refresh needed, or it failed: the current access token is used while still valid
  return claims && remainingMs > 0 ? { claims } : null;
}

function withRefreshedCookies(
  response: NextResponse,
  session: Session,
): NextResponse {
  if (session.refreshed) {
    setAuthCookies(response, session.refreshed);
  }
  return response;
}

function isSameOrigin(request: NextRequest): boolean {
  const origin = request.headers.get('origin');
  if (!origin) {
    return false;
  }
  const host =
    request.headers.get('x-forwarded-host') ??
    request.headers.get('host') ??
    request.nextUrl.host;
  try {
    return new URL(origin).host === host;
  } catch {
    return false;
  }
}

function loginUrl(request: NextRequest): URL {
  const url = new URL('/login', request.url);
  url.searchParams.set(
    'next',
    request.nextUrl.pathname + request.nextUrl.search,
  );
  return url;
}

export const config = {
  // Everything except Next internals and static files
  matcher: [
    '/((?!_next/static|_next/image|favicon.ico|.*\\.(?:svg|png|jpg|jpeg|gif|webp|ico)$).*)',
  ],
};
