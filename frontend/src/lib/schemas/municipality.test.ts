import { describe, expect, test } from 'vitest';

import { municipalitySchema } from './municipality';

describe('municipalitySchema', () => {
  test('trims every field', () => {
    const result = municipalitySchema.parse({
      name: ' Braga ',
      district: ' Braga',
      country: 'Portugal ',
    });

    expect(result).toEqual({
      name: 'Braga',
      district: 'Braga',
      country: 'Portugal',
    });
  });

  test('accepts a blank country, which the backend defaults to Portugal', () => {
    const result = municipalitySchema.safeParse({
      name: 'Braga',
      district: 'Braga',
      country: '  ',
    });

    expect(result.data?.country).toBe('');
  });

  test('rejects a name made only of spaces', () => {
    const result = municipalitySchema.safeParse({
      name: '   ',
      district: 'Braga',
      country: '',
    });

    expect(result.error?.issues[0]).toMatchObject({
      path: ['name'],
      message: 'Enter the name.',
    });
  });

  test('rejects a district longer than the backend allows', () => {
    const result = municipalitySchema.safeParse({
      name: 'Braga',
      district: 'a'.repeat(151),
      country: '',
    });

    expect(result.error?.issues[0]).toMatchObject({
      path: ['district'],
      message: 'District is too long.',
    });
  });
});
