import { describe, expect, test } from 'vitest';

import { safeNext } from './safe-next';

describe('safeNext', () => {
  test('keeps a same-site path with its query', () => {
    expect(safeNext('/vacations?year=2026')).toBe('/vacations?year=2026');
  });

  test.each([
    undefined,
    '',
    'https://evil.example',
    '//evil.example',
    '/\\evil.example',
  ])('falls back to / for %j', (next) => {
    expect(safeNext(next)).toBe('/');
  });
});
