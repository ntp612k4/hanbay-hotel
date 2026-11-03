import { apiClient } from '../../../lib/axios';
import type { ApiResponse } from '../../../types/api';
import type {
  CreatePriceRulePayload,
  PriceRuleListParams,
  PriceRuleManagementItem,
  PriceRuleManagementPage,
  UpdatePriceRulePayload,
} from '../types/price-rule-management';

const BASE_PATH = '/management/price-rules';

export async function getManagedPriceRules(
  params: PriceRuleListParams,
): Promise<PriceRuleManagementPage> {
  const search = params.search?.trim();
  const response = await apiClient.get<ApiResponse<PriceRuleManagementPage>>(BASE_PATH, {
    params: {
      page: params.page,
      size: params.size,
      sort: params.sort,
      ...(search ? { search } : {}),
      ...(params.roomTypeId ? { roomTypeId: params.roomTypeId } : {}),
      ...(params.ruleType ? { ruleType: params.ruleType } : {}),
      ...(params.active === undefined ? {} : { active: params.active }),
    },
  });
  return response.data.data;
}

export async function getManagedPriceRule(id: string): Promise<PriceRuleManagementItem> {
  const response = await apiClient.get<ApiResponse<PriceRuleManagementItem>>(`${BASE_PATH}/${id}`);
  return response.data.data;
}

export async function createPriceRule(
  payload: CreatePriceRulePayload,
): Promise<PriceRuleManagementItem> {
  const response = await apiClient.post<ApiResponse<PriceRuleManagementItem>>(BASE_PATH, payload);
  return response.data.data;
}

export async function updatePriceRule(
  id: string,
  payload: UpdatePriceRulePayload,
): Promise<PriceRuleManagementItem> {
  const response = await apiClient.put<ApiResponse<PriceRuleManagementItem>>(
    `${BASE_PATH}/${id}`,
    payload,
  );
  return response.data.data;
}

export async function updatePriceRuleStatus(
  id: string,
  active: boolean,
): Promise<PriceRuleManagementItem> {
  const response = await apiClient.patch<ApiResponse<PriceRuleManagementItem>>(
    `${BASE_PATH}/${id}/status`,
    { active },
  );
  return response.data.data;
}
