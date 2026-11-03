import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import type { DirtyRoomsParams } from '../types/dirty-rooms';
import { completeCleaning, getDirtyRooms, startCleaning } from './housekeeping-api';

export const housekeepingKeys = {
  all: ['housekeeping'] as const,
  dirtyRooms: (params: DirtyRoomsParams) =>
    [...housekeepingKeys.all, 'dirty-rooms', params] as const,
};

export const useDirtyRooms = (params: DirtyRoomsParams) =>
  useQuery({
    queryKey: housekeepingKeys.dirtyRooms(params),
    queryFn: () => getDirtyRooms(params),
    placeholderData: keepPreviousData,
  });

export function useHousekeepingAction() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ roomId, action }: { roomId: string; action: 'start' | 'complete' }) =>
      action === 'start' ? startCleaning(roomId) : completeCleaning(roomId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: housekeepingKeys.all }),
  });
}
