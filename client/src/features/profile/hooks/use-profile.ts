import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { getCurrentProfile, updateCurrentProfile } from '../api/profile-api';
import type { UpdateUserRequest, User } from '../types/profile';

export const CURRENT_PROFILE_QUERY_KEY = ['account', 'profile'] as const;

export function useCurrentProfile() {
  return useQuery({ queryKey: CURRENT_PROFILE_QUERY_KEY, queryFn: getCurrentProfile });
}

export function useUpdateCurrentProfile() {
  const queryClient = useQueryClient();

  return useMutation<User, Error, UpdateUserRequest>({
    mutationFn: updateCurrentProfile,
    onSuccess: (profile) => queryClient.setQueryData(CURRENT_PROFILE_QUERY_KEY, profile),
  });
}
