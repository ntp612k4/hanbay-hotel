import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import {
  createAddOn,
  getManagedAddOn,
  getManagedAddOns,
  updateAddOn,
  updateAddOnStatus,
} from '../api/addon-management-api';
import type {
  AddOnListParams,
  CreateAddOnPayload,
  UpdateAddOnPayload,
} from '../types/addon-management';

export const addOnManagementKeys = {
  all: ['addon-management'] as const,
  list: (params: AddOnListParams) => [...addOnManagementKeys.all, 'list', params] as const,
  detail: (id: string) => [...addOnManagementKeys.all, 'detail', id] as const,
};

export function useManagedAddOns(params: AddOnListParams) {
  return useQuery({
    queryKey: addOnManagementKeys.list(params),
    queryFn: () => getManagedAddOns(params),
  });
}

export function useManagedAddOn(id: string | null) {
  return useQuery({
    queryKey: addOnManagementKeys.detail(id ?? ''),
    queryFn: () => getManagedAddOn(id!),
    enabled: id !== null,
  });
}

export function useAddOnMutations() {
  const queryClient = useQueryClient();
  const refresh = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: addOnManagementKeys.all }),
      queryClient.invalidateQueries({ queryKey: ['booking', 'add-ons'] }),
    ]);
  };

  return {
    createMutation: useMutation({
      mutationFn: (payload: CreateAddOnPayload) => createAddOn(payload),
      onSuccess: refresh,
    }),
    updateMutation: useMutation({
      mutationFn: ({ id, payload }: { id: string; payload: UpdateAddOnPayload }) =>
        updateAddOn(id, payload),
      onSuccess: refresh,
    }),
    statusMutation: useMutation({
      mutationFn: ({ id, active }: { id: string; active: boolean }) =>
        updateAddOnStatus(id, active),
      onSuccess: refresh,
    }),
  };
}
