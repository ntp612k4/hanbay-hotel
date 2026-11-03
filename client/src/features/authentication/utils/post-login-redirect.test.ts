import { describe, expect, it } from 'vitest';

import { USER_ROLES, type UserRole } from '../../../types/common';
import { getDefaultPathForRoles, getPostLoginRedirect } from './post-login-redirect';

describe('post-login redirect', () => {
  it.each<[UserRole[], string]>([
    [[USER_ROLES.admin], '/dashboard'],
    [[USER_ROLES.manager], '/dashboard'],
    [[USER_ROLES.receptionist], '/front-desk/check-in'],
    [[USER_ROLES.housekeeper], '/housekeeping'],
    [[], '/'],
  ])('returns the default destination for roles %j', (roles, expected) => {
    expect(getDefaultPathForRoles(roles)).toBe(expected);
  });

  it('uses the documented priority for an account with multiple roles', () => {
    expect(getDefaultPathForRoles([USER_ROLES.housekeeper, USER_ROLES.manager])).toBe('/dashboard');
    expect(getDefaultPathForRoles([USER_ROLES.housekeeper, USER_ROLES.receptionist])).toBe(
      '/front-desk/check-in',
    );
  });

  it('returns to the original route when the role is allowed', () => {
    expect(
      getPostLoginRedirect([USER_ROLES.housekeeper], {
        pathname: '/housekeeping',
        search: '?floor=2',
        hash: '#room-201',
      }),
    ).toBe('/housekeeping?floor=2#room-201');
  });

  it('falls back to the role destination when the original route is forbidden', () => {
    expect(getPostLoginRedirect([USER_ROLES.housekeeper], { pathname: '/dashboard' })).toBe(
      '/housekeeping',
    );
  });

  it.each(['https://example.com/admin', '//example.com/admin', '/unknown'])(
    'rejects an unsafe or unknown redirect: %s',
    (pathname) => {
      expect(getPostLoginRedirect([USER_ROLES.manager], { pathname })).toBe('/dashboard');
    },
  );

  it('lets a guest return to booking history but prevents staff from opening it', () => {
    const requested = { pathname: '/account/bookings/BOOKING-001' };
    expect(getPostLoginRedirect([], requested)).toBe('/account/bookings/BOOKING-001');
    expect(getPostLoginRedirect([USER_ROLES.receptionist], requested)).toBe('/front-desk/check-in');
  });
});

it.each(['/account/profile/', '/ACCOUNT/PROFILE'])('preserves valid path %s', (pathname) => {
  expect(getPostLoginRedirect([USER_ROLES.manager], { pathname, search: '?tab=details' })).toBe(
    `${pathname}?tab=details`,
  );
});
it.each(['/payment-monitoring/refunds/', '/PAYMENT-MONITORING/REFUNDS'])(
  'enforces refund roles for %s',
  (pathname) => {
    expect(getPostLoginRedirect([USER_ROLES.admin], { pathname })).toBe('/dashboard');
    expect(getPostLoginRedirect([USER_ROLES.manager], { pathname })).toBe(pathname);
  },
);
it.each([
  '/payment-monitoring/unknown',
  '/account/bookings-other',
  '/account/bookings/code/unknown',
])('rejects unknown path %s', (pathname) => {
  expect(getPostLoginRedirect([], { pathname })).toBe('/');
  expect(getPostLoginRedirect([USER_ROLES.manager], { pathname })).toBe('/dashboard');
});
it('preserves dynamic parameter case', () => {
  expect(getPostLoginRedirect([], { pathname: '/ACCOUNT/BOOKINGS/AbC123' })).toBe(
    '/ACCOUNT/BOOKINGS/AbC123',
  );
});
