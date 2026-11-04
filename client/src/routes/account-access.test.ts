import { describe, expect, it } from 'vitest';
import { canAccessPostLoginPath } from './route-access';
import type { UserRole } from '../types/common';
describe('account administration access', () => {
  it('allows admin to open the accounts route', () => {
    expect(canAccessPostLoginPath('/admin/users', ['ADMIN'])).toBe(true);
  });
  it.each<UserRole[]>([['MANAGER'], ['RECEPTIONIST'], ['HOUSEKEEPER'], []])(
    'rejects roles %j',
    (...roles) => {
      expect(canAccessPostLoginPath('/admin/users', roles)).toBe(false);
    },
  );
});
