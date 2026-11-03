import { apiClient } from '../../../lib/axios';
import type { ApiResponse } from '../../../types/api';
import type { DirtyRoom, DirtyRoomsPage, DirtyRoomsParams } from '../types/dirty-rooms';

export async function getDirtyRooms(params: DirtyRoomsParams): Promise<DirtyRoomsPage> {
  const response = await apiClient.get<ApiResponse<DirtyRoomsPage>>('/housekeeping/rooms', {
    params,
  });

  return response.data.data;
}

export async function startCleaning(roomId: string): Promise<DirtyRoom> {
  const response = await apiClient.patch<ApiResponse<DirtyRoom>>(
    `/housekeeping/rooms/${encodeURIComponent(roomId)}/start-cleaning`,
  );
  return response.data.data;
}

export async function completeCleaning(roomId: string): Promise<DirtyRoom> {
  const response = await apiClient.patch<ApiResponse<DirtyRoom>>(
    `/housekeeping/rooms/${encodeURIComponent(roomId)}/complete-cleaning`,
  );
  return response.data.data;
}
