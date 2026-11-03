import { z } from 'zod';

export const USER_ROLES = {
  admin: 'ADMIN',
  manager: 'MANAGER',
  receptionist: 'RECEPTIONIST',
  housekeeper: 'HOUSEKEEPER',
} as const;

export type UserRole = (typeof USER_ROLES)[keyof typeof USER_ROLES];

export const userRoleSchema = z.enum([
  USER_ROLES.admin,
  USER_ROLES.manager,
  USER_ROLES.receptionist,
  USER_ROLES.housekeeper,
]);
