import { NextResponse } from 'next/server';

import { API_MEDIA_TYPE } from '@/lib/api/media-type';
import {
  PROBLEM_MEDIA_TYPE,
  type ProblemDetail,
} from '@/lib/api/problem-detail';
import { setAuthCookies } from './cookies';
import type { SessionResponse, TokenPair } from './token-pair';

export function postAuth(path: string, body: unknown): Promise<Response> {
  return fetch(`${process.env.BACKEND_URL}/api/auth${path}`, {
    method: 'POST',
    headers: { Accept: API_MEDIA_TYPE, 'Content-Type': API_MEDIA_TYPE },
    body: JSON.stringify(body),
  });
}

export async function exchangeForSession(
  path: string,
  request: Request,
): Promise<NextResponse<SessionResponse | ProblemDetail>> {
  let body: unknown;
  try {
    body = await request.json();
  } catch {
    return problem(400, 'Bad Request', 'VALIDATION_ERROR');
  }

  let backend: Response;
  try {
    backend = await postAuth(path, body);
  } catch {
    return problem(500, 'Internal Server Error', 'INTERNAL_ERROR');
  }

  if (!backend.ok) {
    return new NextResponse<ProblemDetail>(backend.body, {
      status: backend.status,
      headers: {
        'Content-Type':
          backend.headers.get('Content-Type') ?? PROBLEM_MEDIA_TYPE,
      },
    });
  }

  const pair = (await backend.json()) as TokenPair;
  const response = NextResponse.json<SessionResponse>(
    { user: { name: pair.user.name, role: pair.user.role } },
    { status: backend.status },
  );
  setAuthCookies(response, pair);
  return response;
}

function problem(
  status: number,
  title: string,
  code: string,
): NextResponse<ProblemDetail> {
  return NextResponse.json<ProblemDetail>(
    { status, title, code, timestamp: new Date().toISOString() },
    { status, headers: { 'Content-Type': PROBLEM_MEDIA_TYPE } },
  );
}
