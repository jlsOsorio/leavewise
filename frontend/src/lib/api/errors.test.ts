import { describe, expect, test } from 'vitest';

import { errorMessage } from './errors';

describe('errorMessage', () => {
  test('maps a known code to its message, ignoring the detail', () => {
    expect(errorMessage('EMAIL_TAKEN', 'email already registered')).toBe(
      'An account with this email already exists.',
    );
  });

  test('falls back to the detail for an unknown code', () => {
    expect(errorMessage('SOMETHING_NEW', 'A new backend error')).toBe(
      'A new backend error',
    );
  });

  test('falls back to a generic message for an unknown code without detail', () => {
    expect(errorMessage('SOMETHING_NEW')).toBe(
      'Something went wrong. Please try again.',
    );
  });

  test('does not treat inherited object keys as codes', () => {
    expect(errorMessage('toString', 'detail')).toBe('detail');
  });
});
