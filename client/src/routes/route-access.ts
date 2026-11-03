import { matchPath } from 'react-router-dom';
import { USER_ROLES, type UserRole } from '../types/common';
import { ROUTE_PATHS } from './routePaths';

export const ROUTE_ROLES = {
  management: [USER_ROLES.admin, USER_ROLES.manager],
  reception: [USER_ROLES.receptionist],
  housekeeping: [USER_ROLES.housekeeper],
  frontDesk: [USER_ROLES.admin, USER_ROLES.manager, USER_ROLES.receptionist],
  refunds: [USER_ROLES.manager],
  maintenance: [USER_ROLES.manager],
} satisfies Record<string, UserRole[]>;

const staffRoutes: Array<[string, UserRole[]]> = [
  ['/management/rooms', ROUTE_ROLES.management],
  ['/management/room-types', ROUTE_ROLES.management],
  [ROUTE_PATHS.dashboard, ROUTE_ROLES.management],
  [ROUTE_PATHS.frontDeskCheckIn, ROUTE_ROLES.reception],
  [ROUTE_PATHS.housekeeping, ROUTE_ROLES.housekeeping],
  [ROUTE_PATHS.maintenance, ROUTE_ROLES.maintenance],
  [ROUTE_PATHS.roomMaintenance, ROUTE_ROLES.maintenance],
  [ROUTE_PATHS.addOnServices, ROUTE_ROLES.management],
  [ROUTE_PATHS.priceRules, ROUTE_ROLES.management],
  [ROUTE_PATHS.arrivals, ROUTE_ROLES.frontDesk],
  [ROUTE_PATHS.departures, ROUTE_ROLES.frontDesk],
  [ROUTE_PATHS.payments, ROUTE_ROLES.management],
  [ROUTE_PATHS.webhookEvents, ROUTE_ROLES.management],
  [ROUTE_PATHS.refunds, ROUTE_ROLES.refunds],
];

export function canAccessPostLoginPath(pathname: string, roles: UserRole[]): boolean {
  if (!pathname.startsWith('/') || pathname.startsWith('//') || /[\\?#]/.test(pathname))
    return false;
  const matches = (path: string) => matchPath({ path, end: true }, pathname) !== null;
  if (matches(ROUTE_PATHS.profile)) return true;
  if (matches(ROUTE_PATHS.guestBookings) || matches(`${ROUTE_PATHS.guestBookings}/:bookingCode`)) {
    return roles.length === 0;
  }
  return staffRoutes.some(
    ([path, allowed]) => matches(path) && allowed.some((role) => roles.includes(role)),
  );
}
