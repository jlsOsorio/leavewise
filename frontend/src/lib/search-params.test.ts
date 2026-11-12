import { describe, expect, test } from 'vitest';

import { pageParam, textParam } from './search-params';

describe('pageParam', () => {
  test('reads a page index', () => {
    expect(pageParam('3')).toBe(3);
  });

  test.each([undefined, '', '-1', '1.5', 'abc'])(
    'falls back to the first page for %j',
    (value) => {
      expect(pageParam(value)).toBe(0);
    },
  );

  test('uses the first value of a repeated parameter', () => {
    expect(pageParam(['2', '5'])).toBe(2);
  });
});

describe('textParam', () => {
  test('trims the value and cuts it to the maximum length', () => {
    expect(textParam('  Braga  ', 3)).toBe('Bra');
  });

  test('returns an empty string when absent', () => {
    expect(textParam(undefined, 10)).toBe('');
  });
});
