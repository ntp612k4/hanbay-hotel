import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import {
  createPriceRule,
  getManagedPriceRule,
  getManagedPriceRules,
  updatePriceRule,
  updatePriceRuleStatus,
} from '../api/price-rule-management-api';
import type {
  CreatePriceRulePayload,
  PriceRuleListParams,
  UpdatePriceRulePayload,
} from '../types/price-rule-management';

export const priceRuleManagementKeys = {
  all: ['price-rule-management'] as const,
  list: (params: PriceRuleListParams) => [...priceRuleManagementKeys.all, 'list', params] as const,
  detail: (id: string) => [...priceRuleManagementKeys.all, 'detail', id] as const,
};

export function useManagedPriceRules(params: PriceRuleListParams) {
  return useQuery({
    queryKey: priceRuleManagementKeys.list(params),
    queryFn: () => getManagedPriceRules(params),
  });
}

export function useManagedPriceRule(id: string | null) {
  return useQuery({
    queryKey: priceRuleManagementKeys.detail(id ?? ''),
    queryFn: () => getManagedPriceRule(id!),
    enabled: id !== null,
  });
}

export function usePriceRuleMutations() {
  const queryClient = useQueryClient();
  const refresh = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: priceRuleManagementKeys.all }),
      queryClient.invalidateQueries({ queryKey: ['available-room-search'] }),
      queryClient.invalidateQueries({ queryKey: ['stay-price'] }),
    ]);
  };

  return {
    createMutation: useMutation({
      mutationFn: (payload: CreatePriceRulePayload) => createPriceRule(payload),
      onSuccess: refresh,
    }),
    updateMutation: useMutation({
      mutationFn: ({ id, payload }: { id: string; payload: UpdatePriceRulePayload }) =>
        updatePriceRule(id, payload),
      onSuccess: refresh,
    }),
    statusMutation: useMutation({
      mutationFn: ({ id, active }: { id: string; active: boolean }) =>
        updatePriceRuleStatus(id, active),
      onSuccess: refresh,
    }),
  };
}
