import { describe, expect, test } from 'vitest';

import { locationSchema } from './location';

describe('locationSchema', () => {
  test('trims the name and address', () => {
    const result = locationSchema.parse({
      name: ' Sede ',
      address: ' Rua do Souto, 1 ',
      municipalityId: 5,
    });

    expect(result).toEqual({
      name: 'Sede',
      address: 'Rua do Souto, 1',
      municipalityId: 5,
    });
  });

  test('accepts a blank address', () => {
    const result = locationSchema.safeParse({
      name: 'Sede',
      address: '  ',
      municipalityId: 5,
    });

    expect(result.data?.address).toBe('');
  });

  test('rejects a name made only of spaces', () => {
    const result = locationSchema.safeParse({
      name: '   ',
      address: '',
      municipalityId: 5,
    });

    expect(result.error?.issues[0]).toMatchObject({
      path: ['name'],
      message: 'Enter the name.',
    });
  });

  test('rejects an address longer than the backend allows', () => {
    const result = locationSchema.safeParse({
      name: 'Sede',
      address: 'a'.repeat(501),
      municipalityId: 5,
    });

    expect(result.error?.issues[0]).toMatchObject({
      path: ['address'],
      message: 'Address is too long.',
    });
  });

  test('asks for a municipality when none was chosen', () => {
    const result = locationSchema.safeParse({ name: 'Sede', address: '' });

    expect(result.error?.issues[0]).toMatchObject({
      path: ['municipalityId'],
      message: 'Choose the municipality.',
    });
  });
});
