import { NextResponse } from 'next/server';

import { PROBLEM_MEDIA_TYPE, type ProblemDetail } from './problem-detail';

// ProblemDetail produced by the BFF itself (not passed through from the backend)
export function problemResponse(
  status: number,
  title: string,
  code: string,
): NextResponse<ProblemDetail> {
  return NextResponse.json<ProblemDetail>(
    { status, title, code, timestamp: new Date().toISOString() },
    { status, headers: { 'Content-Type': PROBLEM_MEDIA_TYPE } },
  );
}
