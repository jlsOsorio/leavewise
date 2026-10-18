import { type NextRequest, NextResponse } from 'next/server';

import { postAuth } from '@/lib/auth/backend';
import { clearAuthCookies, REFRESH_COOKIE } from '@/lib/auth/cookies';

// Always ends the session locally; the backend revoke is best effort (it is idempotent)
export async function POST(request: NextRequest) {
  const refreshToken = request.cookies.get(REFRESH_COOKIE)?.value;
  if (refreshToken) {
    await postAuth('/logout', { refreshToken }).catch(() => undefined);
  }

  const response = new NextResponse(null, { status: 204 });
  clearAuthCookies(response);
  return response;
}
