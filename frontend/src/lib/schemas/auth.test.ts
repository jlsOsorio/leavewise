import { describe, expect, test } from 'vitest';

import { loginSchema } from './auth';

describe('loginSchema', () => {
  test('trims the email', () => {
    const result = loginSchema.parse({
      email: '  ana@example.pt ',
      password: 'x',
    });

    expect(result.email).toBe('ana@example.pt');
  });

  test('rejects an invalid email', () => {
    const result = loginSchema.safeParse({ email: 'ana', password: 'x' });

    expect(result.error?.issues[0]).toMatchObject({
      path: ['email'],
      message: 'Enter a valid email address.',
    });
  });

  test('rejects an empty password', () => {
    const result = loginSchema.safeParse({
      email: 'ana@example.pt',
      password: '',
    });

    expect(result.error?.issues[0]).toMatchObject({
      path: ['password'],
      message: 'Enter your password.',
    });
  });

  test('accepts a short password, since login applies no password policy', () => {
    const result = loginSchema.safeParse({
      email: 'ana@example.pt',
      password: 'abc',
    });

    expect(result.success).toBe(true);
  });
});
