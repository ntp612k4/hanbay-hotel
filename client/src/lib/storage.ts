import { userRoleSchema, type UserRole } from '../types/common';

export const AUTH_SESSION_UPDATED_EVENT = 'auth-session-updated';

const ACCESS_TOKEN_KEY = 'access_token';
const REFRESH_TOKEN_KEY = 'refresh_token';
const USER_ROLES_KEY = 'user_roles';
const EXPIRES_AT_KEY = 'expires_at';
const USER_PROFILE_KEY = 'user_profile';

export interface StoredAuthUser {
  id: string;
  fullName: string;
  email: string;
  phone: string | null;
}

export interface StoredAuthSession {
  accessToken: string;
  roles: UserRole[];
  expiresAt: number;
  user: StoredAuthUser | null;
}

const getLocalStorage = (): Storage | undefined => {
  if (typeof window === 'undefined') return undefined;

  return window.localStorage;
};

const getSessionStorage = (): Storage | undefined => {
  if (typeof window === 'undefined') return undefined;

  return window.sessionStorage;
};

const getAuthStorage = (): Storage | undefined => {
  const sessionStorage = getSessionStorage();
  return sessionStorage?.getItem(ACCESS_TOKEN_KEY) || sessionStorage?.getItem(REFRESH_TOKEN_KEY)
    ? sessionStorage
    : getLocalStorage();
};

const readRoles = (storage = getAuthStorage()): UserRole[] => {
  const value = storage?.getItem(USER_ROLES_KEY);

  if (!value) return [];

  try {
    const result = userRoleSchema.array().safeParse(JSON.parse(value));
    return result.success ? result.data : [];
  } catch {
    return [];
  }
};

const readUser = (storage = getAuthStorage()): StoredAuthUser | null => {
  const value = storage?.getItem(USER_PROFILE_KEY);
  if (!value) return null;
  try {
    const user = JSON.parse(value) as Partial<StoredAuthUser>;
    return typeof user.id === 'string' &&
      typeof user.fullName === 'string' &&
      typeof user.email === 'string' &&
      (typeof user.phone === 'string' || user.phone === null)
      ? (user as StoredAuthUser)
      : null;
  } catch {
    return null;
  }
};

const clearAuthSession = (): void => {
  tokenStorage.clearTokens();
  getLocalStorage()?.removeItem(USER_ROLES_KEY);
  getSessionStorage()?.removeItem(USER_ROLES_KEY);
  getLocalStorage()?.removeItem(EXPIRES_AT_KEY);
  getSessionStorage()?.removeItem(EXPIRES_AT_KEY);
  getLocalStorage()?.removeItem(USER_PROFILE_KEY);
  getSessionStorage()?.removeItem(USER_PROFILE_KEY);
};

export const tokenStorage = {
  getAccessToken: (): string | null => getAuthStorage()?.getItem(ACCESS_TOKEN_KEY) ?? null,

  getRefreshToken: (): string | null => getAuthStorage()?.getItem(REFRESH_TOKEN_KEY) ?? null,

  setAccessToken: (accessToken: string, remember = true): void => {
    const targetStorage = remember ? getLocalStorage() : getSessionStorage();
    const otherStorage = remember ? getSessionStorage() : getLocalStorage();

    otherStorage?.removeItem(ACCESS_TOKEN_KEY);
    targetStorage?.setItem(ACCESS_TOKEN_KEY, accessToken);
  },

  setRefreshToken: (refreshToken: string, remember = true): void => {
    const targetStorage = remember ? getLocalStorage() : getSessionStorage();
    const otherStorage = remember ? getSessionStorage() : getLocalStorage();

    otherStorage?.removeItem(REFRESH_TOKEN_KEY);
    targetStorage?.setItem(REFRESH_TOKEN_KEY, refreshToken);
  },

  setTokens: (accessToken: string, refreshToken: string, remember = true): void => {
    tokenStorage.setAccessToken(accessToken, remember);
    tokenStorage.setRefreshToken(refreshToken, remember);
  },

  removeAccessToken: (): void => {
    getLocalStorage()?.removeItem(ACCESS_TOKEN_KEY);
    getSessionStorage()?.removeItem(ACCESS_TOKEN_KEY);
  },

  removeRefreshToken: (): void => {
    getLocalStorage()?.removeItem(REFRESH_TOKEN_KEY);
    getSessionStorage()?.removeItem(REFRESH_TOKEN_KEY);
  },

  clearTokens: (): void => {
    tokenStorage.removeAccessToken();
    tokenStorage.removeRefreshToken();
  },
};

export const authStorage = {
  getSession: (): StoredAuthSession | null => {
    const storage = getAuthStorage();
    const accessToken = storage?.getItem(ACCESS_TOKEN_KEY) ?? null;
    const roles = readRoles(storage);
    const expiresAtValue = storage?.getItem(EXPIRES_AT_KEY) ?? null;
    const expiresAt = expiresAtValue ? Number(expiresAtValue) : 0;

    if (!accessToken || !Number.isFinite(expiresAt) || expiresAt <= Date.now()) {
      return null;
    }

    return { accessToken, roles, expiresAt, user: readUser(storage) };
  },

  getRoles: readRoles,

  hasRefreshToken: (): boolean => Boolean(tokenStorage.getRefreshToken()),

  setSession: (
    accessToken: string,
    roles: UserRole[],
    expiresInSeconds: number,
    remember = true,
    refreshToken?: string,
    user?: StoredAuthUser,
  ): void => {
    const targetStorage = remember ? getLocalStorage() : getSessionStorage();
    const otherStorage = remember ? getSessionStorage() : getLocalStorage();

    tokenStorage.setAccessToken(accessToken, remember);
    if (refreshToken) tokenStorage.setRefreshToken(refreshToken, remember);
    otherStorage?.removeItem(USER_ROLES_KEY);
    otherStorage?.removeItem(EXPIRES_AT_KEY);
    otherStorage?.removeItem(USER_PROFILE_KEY);
    targetStorage?.setItem(USER_ROLES_KEY, JSON.stringify(roles));
    targetStorage?.setItem(EXPIRES_AT_KEY, String(Date.now() + expiresInSeconds * 1000));
    if (user) targetStorage?.setItem(USER_PROFILE_KEY, JSON.stringify(user));
  },

  updateTokens: (accessToken: string, refreshToken: string, expiresInSeconds: number): void => {
    const sessionStorage = getSessionStorage();
    const remember = !sessionStorage?.getItem(ACCESS_TOKEN_KEY);
    const targetStorage = remember ? getLocalStorage() : sessionStorage;

    tokenStorage.setTokens(accessToken, refreshToken, remember);
    targetStorage?.setItem(EXPIRES_AT_KEY, String(Date.now() + expiresInSeconds * 1000));
    try {
      const payload = accessToken.split('.')[1];
      const claims = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/'))) as {
        roles?: unknown;
      };
      const roles = userRoleSchema.array().safeParse(claims.roles);
      if (roles.success) targetStorage?.setItem(USER_ROLES_KEY, JSON.stringify(roles.data));
    } catch {
      /* A malformed payload must not replace the cached role list. */
    }
    if (typeof window !== 'undefined') window.dispatchEvent(new Event(AUTH_SESSION_UPDATED_EVENT));
  },

  updateUser: (user: StoredAuthUser): void => {
    getAuthStorage()?.setItem(USER_PROFILE_KEY, JSON.stringify(user));
  },

  clearSession: clearAuthSession,
};
