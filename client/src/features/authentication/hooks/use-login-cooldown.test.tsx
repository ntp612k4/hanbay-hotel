// @vitest-environment jsdom
import { act, renderHook, cleanup } from '@testing-library/react';
import { afterEach, expect, it, vi } from 'vitest';
import { useLoginCooldown } from './use-login-cooldown';
afterEach(() => {
  cleanup();
  vi.useRealTimers();
});
it('uses elapsed wall time when a background tab regains focus', () => {
  vi.useFakeTimers();
  vi.setSystemTime(0);
  const { result } = renderHook(useLoginCooldown);
  act(() => result.current.start(60));
  expect(result.current.seconds).toBe(60);
  vi.setSystemTime(65000);
  act(() => window.dispatchEvent(new Event('focus')));
  expect(result.current.seconds).toBe(0);
  expect(vi.getTimerCount()).toBe(0);
});
it('counts down, then releases the interval and accepts a new cooldown', () => {
  vi.useFakeTimers();
  vi.setSystemTime(0);
  const { result } = renderHook(useLoginCooldown);
  act(() => result.current.start(2));
  act(() => vi.advanceTimersByTime(1000));
  expect(result.current.seconds).toBe(1);
  act(() => vi.advanceTimersByTime(1000));
  expect(result.current.seconds).toBe(0);
  act(() => result.current.start(5));
  expect(result.current.seconds).toBe(5);
});
