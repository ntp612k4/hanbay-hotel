import { canAccessPostLoginPath } from '../../../routes/route-access';
export { canAccessPostLoginPath } from '../../../routes/route-access';
import { ROUTE_PATHS } from '../../../routes/routePaths';
import { USER_ROLES, type UserRole } from '../../../types/common';

export type LoginRedirectLocation = {
  pathname?: string;
  search?: string;
  hash?: string;
};

const hasAnyRole = (roles: UserRole[], allowedRoles: UserRole[]) =>
  allowedRoles.some((role) => roles.includes(role));

export function getDefaultPathForRoles(roles: UserRole[]): string {
  if (hasAnyRole(roles, [USER_ROLES.admin, USER_ROLES.manager])) {
    return ROUTE_PATHS.dashboard;
  }
  if (roles.includes(USER_ROLES.receptionist)) return ROUTE_PATHS.frontDeskCheckIn;
  if (roles.includes(USER_ROLES.housekeeper)) return ROUTE_PATHS.housekeeping;
  return ROUTE_PATHS.home;
}

export function getPostLoginRedirect(
  roles: UserRole[],
  requestedLocation?: LoginRedirectLocation,
): string {
  const pathname = requestedLocation?.pathname;
  if (!pathname || !canAccessPostLoginPath(pathname, roles)) {
    return getDefaultPathForRoles(roles);
  }

  return `${pathname}${requestedLocation.search ?? ''}${requestedLocation.hash ?? ''}`;
}
