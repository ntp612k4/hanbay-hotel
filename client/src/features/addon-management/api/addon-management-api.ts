import { apiClient } from '../../../lib/axios';
import type { ApiResponse } from '../../../types/api';
import type {
  AddOnListParams,
  AddOnManagementItem,
  AddOnManagementPage,
  CreateAddOnPayload,
  UpdateAddOnPayload,
} from '../types/addon-management';

const BASE_PATH = '/management/add-ons';

export async function getManagedAddOns(params: AddOnListParams): Promise<AddOnManagementPage> {
  const search = params.search?.trim();
  const response = await apiClient.get<ApiResponse<AddOnManagementPage>>(BASE_PATH, {
    params: {
      page: params.page,
      size: params.size,
      sort: params.sort,
      ...(search ? { search } : {}),
      ...(params.active === undefined ? {} : { active: params.active }),
    },
  });
  return response.data.data;
}

export async function getManagedAddOn(id: string): Promise<AddOnManagementItem> {
  const response = await apiClient.get<ApiResponse<AddOnManagementItem>>(`${BASE_PATH}/${id}`);
  return response.data.data;
}

export async function createAddOn(payload: CreateAddOnPayload): Promise<AddOnManagementItem> {
  const response = await apiClient.post<ApiResponse<AddOnManagementItem>>(BASE_PATH, payload);
  return response.data.data;
}

export async function updateAddOn(
  id: string,
  payload: UpdateAddOnPayload,
): Promise<AddOnManagementItem> {
  const response = await apiClient.put<ApiResponse<AddOnManagementItem>>(
    `${BASE_PATH}/${id}`,
    payload,
  );
  return response.data.data;
}

export async function updateAddOnStatus(id: string, active: boolean): Promise<AddOnManagementItem> {
  const response = await apiClient.patch<ApiResponse<AddOnManagementItem>>(
    `${BASE_PATH}/${id}/status`,
    { active },
  );
  return response.data.data;
}
