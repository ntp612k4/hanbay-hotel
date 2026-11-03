import type { PageResponse } from '../../../types/api';

export interface AddOnManagementItem {
  id: string;
  code: string;
  name: string;
  description: string | null;
  price: number;
  active: boolean;
  createdAt: string;
  updatedAt: string | null;
}

export type AddOnManagementPage = PageResponse<AddOnManagementItem>;

export interface AddOnListParams {
  page: number;
  size: number;
  search?: string;
  active?: boolean;
  sort: string;
}

export interface CreateAddOnPayload {
  code: string;
  name: string;
  description?: string | null;
  price: number;
}

export interface UpdateAddOnPayload {
  name: string;
  description?: string | null;
  price: number;
}

export interface AddOnFormValues {
  code: string;
  name: string;
  description?: string;
  price: number;
}
