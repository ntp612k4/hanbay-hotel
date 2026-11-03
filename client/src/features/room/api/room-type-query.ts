import { keepPreviousData, useQuery } from '@tanstack/react-query';

import type { RoomTypeCatalogParams } from '../types/room-type';
import { getRoomTypeCatalog } from './room-type-api';

export const roomTypeCatalogKeys = {
  all: ['room-type-catalog'] as const,
  list: (params: RoomTypeCatalogParams) => [...roomTypeCatalogKeys.all, params] as const,
};

export function useRoomTypeCatalog(params: RoomTypeCatalogParams) {
  return useQuery({
    queryKey: roomTypeCatalogKeys.list(params),
    queryFn: () => getRoomTypeCatalog(params),
    placeholderData: keepPreviousData,
  });
}
