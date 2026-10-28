import { describe, expect, test } from 'vitest';

import { isValidNif } from './nif';

describe('isValidNif', () => {
  test('accepts a NIF with the right check digit', () => {
    expect(isValidNif('123456789')).toBe(true);
  });

  test('accepts check digit 0 when the remainder is 0 or 1', () => {
    expect(isValidNif('500000000')).toBe(true);
  });

  test('rejects a NIF with a wrong check digit', () => {
    expect(isValidNif('123456780')).toBe(false);
  });

  test.each(['12345678', '1234567890', '12345678a', ' 123456789'])(
    'rejects %j, which is not 9 digits',
    (nif) => {
      expect(isValidNif(nif)).toBe(false);
    },
  );
});
