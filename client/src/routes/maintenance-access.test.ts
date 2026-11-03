import { describe, expect, it } from 'vitest';
import { canAccessPostLoginPath } from './route-access';
import { USER_ROLES } from '../types/common';

describe('maintenance return paths', () => {
  it.each(['/maintenance', '/rooms/maintenance'])('preserves manager access for %s', (path) => {
    expect(canAccessPostLoginPath(path, [USER_ROLES.manager])).toBe(true);
    expect(canAccessPostLoginPath(path, [USER_ROLES.admin])).toBe(false);
    expect(canAccessPostLoginPath(path, [USER_ROLES.receptionist])).toBe(false);
    expect(canAccessPostLoginPath(path, [])).toBe(false);
  });
});
