import type { ProblemDetail } from './problem-detail';

type FieldError = NonNullable<ProblemDetail['errors']>[number];

// A backend error response, parsed from its ProblemDetail body
export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly detail?: string;
  readonly errors?: FieldError[];

  constructor(
    status: number,
    code: string,
    detail?: string,
    errors?: FieldError[],
  ) {
    super(detail ?? code);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.detail = detail;
    this.errors = errors;
  }

  // Falls back to INTERNAL_ERROR when the body is not a ProblemDetail
  static async from(response: Response): Promise<ApiError> {
    try {
      const problem = (await response.json()) as Partial<ProblemDetail>;
      if (typeof problem.code === 'string') {
        return new ApiError(
          response.status,
          problem.code,
          problem.detail,
          problem.errors,
        );
      }
    } catch {
      // not JSON
    }
    return new ApiError(response.status, 'INTERNAL_ERROR');
  }
}
