import { NextResponse } from 'next/server';
import type * as z from 'zod';

import { PROBLEM_MEDIA_TYPE, type ProblemDetail } from './problem-detail';

// ProblemDetail produced by the BFF itself (not passed through from the backend)
export function problemResponse(
  status: number,
  title: string,
  code: string,
  extra: Pick<ProblemDetail, 'detail' | 'errors'> = {},
): NextResponse<ProblemDetail> {
  return NextResponse.json<ProblemDetail>(
    { status, title, code, timestamp: new Date().toISOString(), ...extra },
    { status, headers: { 'Content-Type': PROBLEM_MEDIA_TYPE } },
  );
}

export function validationProblem(
  error: z.ZodError,
): NextResponse<ProblemDetail> {
  return problemResponse(400, 'Bad Request', 'VALIDATION_ERROR', {
    errors: error.issues.map((issue) => ({
      field: issue.path.join('.'),
      message: issue.message,
    })),
  });
}
