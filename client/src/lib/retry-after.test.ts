import { expect, it } from 'vitest';
import { parseRetryAfter } from './retry-after';
it.each([
  ['42', 42],
  ['0', 0],
  ['-1', undefined],
  ['1oops', undefined],
  ['', undefined],
  ['NaN', undefined],
])('parses Retry-After %s', (input, expected) => {
  expect(parseRetryAfter(input)).toBe(expected);
});
it('supports HTTP dates and rounds the remaining time up', () => {
  expect(
    parseRetryAfter('Mon, 07 Sep 2026 00:01:00 GMT', Date.parse('2026-09-07T00:00:55.500Z')),
  ).toBe(5);
});
