import { describe, expect, test } from 'vitest';

import { companySchema } from './company';

describe('companySchema', () => {
  test('trims the name', () => {
    const result = companySchema.parse({ name: ' Acme Lda ' });

    expect(result).toEqual({ name: 'Acme Lda' });
  });

  test('rejects a name made only of spaces', () => {
    const result = companySchema.safeParse({ name: '   ' });

    expect(result.error?.issues[0]).toMatchObject({
      path: ['name'],
      message: 'Enter the name.',
    });
  });

  test('rejects a name longer than the backend allows', () => {
    const result = companySchema.safeParse({ name: 'a'.repeat(201) });

    expect(result.error?.issues[0]).toMatchObject({
      path: ['name'],
      message: 'Name is too long.',
    });
  });
});
