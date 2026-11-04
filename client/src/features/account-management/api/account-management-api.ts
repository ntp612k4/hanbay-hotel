import { apiClient } from '../../../lib/axios';
import type { ApiResponse, PageResponse } from '../../../types/api';
import type { UserRole } from '../../../types/common';

export type AccountStatus = 'ACTIVE' | 'INACTIVE' | 'LOCKED';
export interface ManagedAccount {
  id: string;
  fullName: string;
  email: string;
  phone: string | null;
  roles: UserRole[];
  status: AccountStatus;
  createdAt: string;
  updatedAt: string;
}
export interface AccountFilters {
  page: number;
  size: number;
  search?: string;
  role?: UserRole | 'GUEST';
  status?: AccountStatus;
}
export interface CreateAccountPayload {
  fullName: string;
  email: string;
  phone: string;
  password: string;
  roles: UserRole[];
}
const BASE_PATH = '/admin/users';
export async function getManagedAccounts(params: AccountFilters) {
  const response = await apiClient.get<ApiResponse<PageResponse<ManagedAccount>>>(BASE_PATH, {
    params,
  });
  return response.data.data;
}
export async function createAccount(payload: CreateAccountPayload) {
  const response = await apiClient.post<ApiResponse<ManagedAccount>>(BASE_PATH, payload);
  return response.data.data;
}
export async function updateAccountRoles(id: string, roles: UserRole[]) {
  const response = await apiClient.patch<ApiResponse<ManagedAccount>>(`${BASE_PATH}/${id}/roles`, {
    roles,
  });
  return response.data.data;
}
export async function updateAccountStatus(id: string, status: AccountStatus) {
  const response = await apiClient.patch<ApiResponse<ManagedAccount>>(`${BASE_PATH}/${id}/status`, {
    status,
  });
  return response.data.data;
}
