import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  createMaintenanceRequest,
  getMaintenanceDetail,
  getMaintenanceRequests,
  getPhysicalRooms,
  updateMaintenanceStatus,
} from '../api/maintenance-api';
import type { CreateMaintenanceRequest, MaintenanceRequestStatus } from '../types/maintenance';
export const maintenanceKeys = {
  all: ['maintenance'] as const,
  list: (page: number) => [...maintenanceKeys.all, 'list', page] as const,
  detail: (id: string) => [...maintenanceKeys.all, 'detail', id] as const,
  rooms: () => [...maintenanceKeys.all, 'rooms'] as const,
};
export const useMaintenanceRequests = (page: number) =>
  useQuery({
    queryKey: maintenanceKeys.list(page),
    queryFn: () => getMaintenanceRequests(page, 20),
  });
export const usePhysicalRooms = (enabled = true) =>
  useQuery({ queryKey: maintenanceKeys.rooms(), queryFn: getPhysicalRooms, enabled });
export const useMaintenanceDetail = (id: string | null) =>
  useQuery({
    queryKey: maintenanceKeys.detail(id ?? ''),
    queryFn: () => getMaintenanceDetail(id!),
    enabled: !!id,
  });
export function useMaintenanceMutations() {
  const client = useQueryClient();
  const refresh = async () => {
    await Promise.all(
      [
        'maintenance',
        'housekeeping',
        'manager-dashboard',
        'available-physical-rooms',
        'room-type-catalog',
      ].map((key) => client.invalidateQueries({ queryKey: [key] })),
    );
  };
  return {
    create: useMutation({
      mutationFn: (p: CreateMaintenanceRequest) => createMaintenanceRequest(p),
      onSettled: refresh,
    }),
    status: useMutation({
      mutationFn: ({ id, status }: { id: string; status: MaintenanceRequestStatus }) =>
        updateMaintenanceStatus(id, status),
      onSettled: refresh,
    }),
  };
}
