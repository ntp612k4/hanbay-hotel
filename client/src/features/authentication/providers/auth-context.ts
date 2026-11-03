import { createContext, useContext } from 'react';

import type { StoredAuthSession, StoredAuthUser } from '../../../lib/storage';
import type { UserRole } from '../../../types/common';

export interface AuthContextValue {
  session: StoredAuthSession | null;
  isAuthenticated: boolean;
  isInitializing: boolean;
  roles: UserRole[];
  startSession: (
    accessToken: string,
    refreshToken: string,
    roles: UserRole[],
    expiresInSeconds: number,
    remember: boolean,
    user: StoredAuthUser,
  ) => void;
  logout: () => void;
  updateUser: (user: StoredAuthUser) => void;
}

export const AuthContext = createContext<AuthContextValue | null>(null);

export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }

  return context;
}
