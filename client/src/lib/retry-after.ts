/** Retry-After can be delta seconds or an HTTP date. Ignore malformed headers. */
export function parseRetryAfter(value: unknown, now = Date.now()): number | undefined {
  if (typeof value !== 'string' && typeof value !== 'number') return undefined;
  const text = String(value).trim();
  if (/^\d+$/.test(text)) {
    const seconds = Number(text);
    return Number.isSafeInteger(seconds) && seconds <= Number.MAX_SAFE_INTEGER / 1000
      ? seconds
      : undefined;
  }
  // Require a date-shaped value so a negative/fractional delta isn't parsed as a date.
  if (!/[a-z]{3}/i.test(text)) return undefined;
  const date = Date.parse(text);
  return Number.isFinite(date) ? Math.max(0, Math.ceil((date - now) / 1000)) : undefined;
}
