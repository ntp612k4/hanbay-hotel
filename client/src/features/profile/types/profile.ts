import type { UserRole } from '../../../types/common';

export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'LOCKED';

export interface User {
  id: string;
  email: string;
  fullName: string;
  phone: string | null;
  status: UserStatus;
  roles: UserRole[];
  createdAt: string;
  updatedAt: string;
}

export interface UpdateUserRequest {
  fullName?: string;
  phone?: string;
}
