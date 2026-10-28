import { NextResponse } from 'next/server';
import type * as z from 'zod';

import { API_MEDIA_TYPE } from '@/lib/api/media-type';
import {
  PROBLEM_MEDIA_TYPE,
  type ProblemDetail,
} from '@/lib/api/problem-detail';
import { problemResponse, validationProblem } from '@/lib/api/problem-response';
import { setAuthCookies } from './cookies';
import type { SessionResponse, TokenPair } from './token-pair';

export function postAuth(path: string, body: unknown): Promise<Response> {
  return fetch(`${process.env.BACKEND_URL}/api/auth${path}`, {
    method: 'POST',
    headers: { Accept: API_MEDIA_TYPE, 'Content-Type': API_MEDIA_TYPE },
    body: JSON.stringify(body),
  });
}

// `schema` becomes required once registration has its schema (M1 register substep)
export async function exchangeForSession(
  path: string,
  request: Request,
  schema?: z.ZodType,
): Promise<NextResponse<SessionResponse | ProblemDetail>> {
  let body: unknown;
  try {
    body = await request.json();
  } catch {
    return problemResponse(400, 'Bad Request', 'VALIDATION_ERROR');
  }

  if (schema) {
    const parsed = schema.safeParse(body);
    if (!parsed.success) {
      return validationProblem(parsed.error);
    }
    body = parsed.data;
  }

  let backend: Response;
  try {
    backend = await postAuth(path, body);
  } catch {
    return problemResponse(500, 'Internal Server Error', 'INTERNAL_ERROR');
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
