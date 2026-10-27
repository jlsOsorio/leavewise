const GENERIC_MESSAGE = 'Something went wrong. Please try again.';
const SESSION_EXPIRED = 'Your session has expired. Please sign in again.';

const MESSAGES = {
  VALIDATION_ERROR: 'Some fields are invalid. Check them and try again.',
  INVALID_FILE: 'The file is not a valid vacation import file.',
  UNAUTHENTICATED: SESSION_EXPIRED,
  INVALID_CREDENTIALS: 'Incorrect email or password.',
  INVALID_REFRESH_TOKEN: SESSION_EXPIRED,
  FORBIDDEN: "You don't have permission to do that.",
  ACCOUNT_DISABLED: 'Your account is disabled. Contact an administrator.',
  NOT_FOUND: 'The requested item was not found.',
  EMAIL_TAKEN: 'An account with this email already exists.',
  NIF_TAKEN: 'An account with this NIF already exists.',
  DUPLICATE: 'This item already exists.',
  RESOURCE_IN_USE: "This item is in use and can't be deleted.",
  LAST_ADMIN: 'There must always be at least one active administrator.',
  MULTI_YEAR_RANGE: "A vacation can't span more than one calendar year.",
  NO_WORKING_DAYS: 'The selected dates include no working days.',
  VACATION_OVERLAP: 'These dates overlap another vacation.',
  NO_ENTITLEMENT: 'There is no vacation entitlement for that year.',
  INSUFFICIENT_BALANCE: 'Not enough vacation days available.',
  PAST_DATE: "Dates in the past can't be used.",
  INVALID_STATUS_TRANSITION:
    "This vacation can't be changed in its current status.",
  LOCATION_COMPANY_MISMATCH:
    "The selected location doesn't belong to the selected company.",
  INTERNAL_ERROR: GENERIC_MESSAGE,
  // Spring MVC rejections: a BFF bug, not something the user can fix
  UNSUPPORTED_API_VERSION: GENERIC_MESSAGE,
  METHOD_NOT_ALLOWED: GENERIC_MESSAGE,
  NOT_ACCEPTABLE: GENERIC_MESSAGE,
  UNSUPPORTED_MEDIA_TYPE: GENERIC_MESSAGE,
} as const satisfies Record<string, string>;

export type ErrorCode = keyof typeof MESSAGES;

function isKnownCode(code: string): code is ErrorCode {
  return Object.hasOwn(MESSAGES, code);
}

// Unknown codes fall back to the backend's `detail`
export function errorMessage(code: string, detail?: string): string {
  return isKnownCode(code) ? MESSAGES[code] : (detail ?? GENERIC_MESSAGE);
}
