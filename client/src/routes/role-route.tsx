import type { ReactNode } from 'react';
import { Navigate } from 'react-router-dom';

import { useAuth } from '../features/authentication/providers/auth-context';
import type { UserRole } from '../types/common';

interface RoleRouteProps {
  allowedRoles: UserRole[];
  children: ReactNode;
}

export function RoleRoute({ allowedRoles, children }: RoleRouteProps) {
  const { roles } = useAuth();
  const isAllowed = allowedRoles.some((role) => roles.includes(role));

  return isAllowed ? children : <Navigate to="/forbidden" replace />;
}
