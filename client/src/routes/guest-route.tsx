import type { ReactNode } from 'react';
import { Navigate } from 'react-router-dom';

import { useAuth } from '../features/authentication/providers/auth-context';

export function GuestRoute({ children }: { children: ReactNode }) {
  const { roles } = useAuth();
  return roles.length === 0 ? children : <Navigate to="/forbidden" replace />;
}
