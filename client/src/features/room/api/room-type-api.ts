import { apiClient } from '../../../lib/axios';

import type { RoomTypeCatalogPage, RoomTypeCatalogParams } from '../types/room-type';

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

export async function getRoomTypeCatalog(
  params: RoomTypeCatalogParams,
): Promise<RoomTypeCatalogPage> {
  const normalizedKeyword = params.keyword?.trim();
  const response = await apiClient.get<ApiResponse<RoomTypeCatalogPage>>('/room-types', {
    params: {
      ...params,
      keyword: normalizedKeyword || undefined,
    },
  });

  return response.data.data;
}
