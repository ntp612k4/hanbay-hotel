import { apiClient } from '../../../lib/axios';

import type { ApiEnvelope, RoomTypeDetail } from '../types/room-detail';

export async function getRoomTypeDetail(roomTypeId: string): Promise<RoomTypeDetail> {
  const response = await apiClient.get<ApiEnvelope<RoomTypeDetail>>(`/room-types/${roomTypeId}`);
  return response.data.data;
}
