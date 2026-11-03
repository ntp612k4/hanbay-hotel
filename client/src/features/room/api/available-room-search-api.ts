import { apiClient } from '../../../lib/axios';

import type {
  AvailableRoomType,
  AvailableRoomTypeSearchParams,
  AvailableRoomTypeSearchResponse,
} from '../types/available-room-search';
import type { PageResponse } from '../../../types/api';

export async function searchAvailableRoomTypes(
  params: AvailableRoomTypeSearchParams,
): Promise<PageResponse<AvailableRoomType>> {
  const response = await apiClient.get<AvailableRoomTypeSearchResponse>('/room-types/available', {
    params,
  });

  return response.data.data;
}
