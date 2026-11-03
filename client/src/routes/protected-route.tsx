import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { LoadingPage } from '../pages/loading-page';

import { useAuth } from '../features/authentication/providers/auth-context';

export function ProtectedRoute() {
  const location = useLocation();
  const { isAuthenticated, isInitializing } = useAuth();

  if (isInitializing) return <LoadingPage />;

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  return <Outlet />;
}
