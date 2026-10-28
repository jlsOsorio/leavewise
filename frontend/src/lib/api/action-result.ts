import type * as z from 'zod';

import type { ApiError } from './api-error';
import { errorMessage } from './errors';

export type ActionError = {
  code: string;
  message: string;
  fieldErrors?: Record<string, string>;
};

export type ActionResult = { ok: true } | { ok: false; error: ActionError };

export function apiFailure(error: ApiError): ActionResult {
  return {
    ok: false,
    error: {
      code: error.code,
      message: errorMessage(error.code, error.detail),
      fieldErrors: toFieldErrors(error.errors ?? []),
    },
  };
}

export function validationFailure(error: z.ZodError): ActionResult {
  return {
    ok: false,
    error: {
      code: 'VALIDATION_ERROR',
      message: errorMessage('VALIDATION_ERROR'),
      fieldErrors: toFieldErrors(
        error.issues.map((issue) => ({
          field: issue.path.join('.'),
          message: issue.message,
        })),
      ),
    },
  };
}

function toFieldErrors(
  errors: { field: string; message: string }[],
): Record<string, string> | undefined {
  if (errors.length === 0) {
    return undefined;
  }
  const fieldErrors: Record<string, string> = {};
  for (const { field, message } of errors) {
    fieldErrors[field] ??= message;
  }
  return fieldErrors;
}
