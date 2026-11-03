import { apiClient } from '../../../lib/axios';
import type { ApiResponse } from '../../../types/api';
import type {
  CreateMaintenanceRequest,
  MaintenanceDetail,
  MaintenancePage,
  MaintenanceRequestStatus,
  PhysicalRoomPage,
} from '../types/maintenance';
const maintenancePath = '/maintenance-requests';
export const getMaintenanceRequests = async (page: number, size: number) =>
  (await apiClient.get<ApiResponse<MaintenancePage>>(maintenancePath, { params: { page, size } }))
    .data.data;
export const getMaintenanceDetail = async (id: string) =>
  (
    await apiClient.get<ApiResponse<MaintenanceDetail>>(
      `${maintenancePath}/${encodeURIComponent(id)}`,
    )
  ).data.data;
export const getPhysicalRooms = async () => {
  const first = (
    await apiClient.get<ApiResponse<PhysicalRoomPage>>('/rooms', { params: { page: 0, size: 100 } })
  ).data.data;
  const rooms = [...first.content];
  for (let page = 1; page < first.totalPages; page++) {
    const next = (
      await apiClient.get<ApiResponse<PhysicalRoomPage>>('/rooms', { params: { page, size: 100 } })
    ).data.data;
    rooms.push(...next.content);
  }
  return rooms;
};
export const createMaintenanceRequest = async (payload: CreateMaintenanceRequest) =>
  (await apiClient.post<ApiResponse<MaintenanceDetail>>(maintenancePath, payload)).data.data;
export const updateMaintenanceStatus = async (id: string, status: MaintenanceRequestStatus) =>
  (
    await apiClient.patch<ApiResponse<MaintenanceDetail>>(
      `${maintenancePath}/${encodeURIComponent(id)}/status`,
      {
        status,
      },
    )
  ).data.data;
