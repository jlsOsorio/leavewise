import { describe, expect, test } from 'vitest';

import { loginSchema, passwordSchema, registrationSchema } from './auth';

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

describe('passwordSchema', () => {
  test('rejects a password shorter than 8 characters', () => {
    const result = passwordSchema.safeParse('1234567');

    expect(result.error?.issues[0]?.message).toBe('Use at least 8 characters.');
  });

  test('accepts 72 UTF-8 bytes', () => {
    expect(passwordSchema.safeParse('é'.repeat(36)).success).toBe(true);
  });

  test('rejects more than 72 UTF-8 bytes even when under 72 characters', () => {
    const result = passwordSchema.safeParse('é'.repeat(36) + 'a');

    expect(result.error?.issues[0]?.message).toBe('Password is too long.');
  });
});

describe('registrationSchema', () => {
  const valid = {
    name: 'Ana Ferreira',
    email: 'ana@example.pt',
    password: 'secret123',
    nif: '',
    contact: '',
    companyId: 1,
    companyLocationId: 2,
  };

  test('turns empty optional fields into undefined', () => {
    const result = registrationSchema.parse(valid);

    expect(result).toMatchObject({ nif: undefined, contact: undefined });
  });

  test('accepts its own output, as the action re-validates what the form sends', () => {
    const output = registrationSchema.parse(valid);

    expect(registrationSchema.safeParse(output).success).toBe(true);
  });

  test('rejects an invalid NIF', () => {
    const result = registrationSchema.safeParse({ ...valid, nif: '123456780' });

    expect(result.error?.issues[0]).toMatchObject({
      path: ['nif'],
      message: 'Enter a valid NIF.',
    });
  });

  test('asks for a company and a location until both are chosen', () => {
    const result = registrationSchema.safeParse({
      ...valid,
      companyId: undefined,
      companyLocationId: undefined,
    });

    expect(result.error?.issues.map((issue) => issue.message)).toEqual([
      'Choose your company.',
      'Choose your location.',
    ]);
  });
});
