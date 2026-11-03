import type { PageResponse } from '../../../types/api';

export type MaintenanceRequestStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED';
export type RoomStatus =
  'AVAILABLE' | 'OCCUPIED' | 'DIRTY' | 'CLEANING' | 'MAINTENANCE' | 'OUT_OF_SERVICE';

export interface PhysicalRoom {
  roomId: string;
  roomNumber: string;
  floor: number;
  status: RoomStatus;
}
export interface MaintenanceItem {
  id: string;
  roomId: string;
  roomNumber: string;
  roomStatus: RoomStatus;
  title: string;
  status: MaintenanceRequestStatus;
  createdById: string;
  createdByName: string;
  createdAt: string;
  startedAt: string | null;
  completedAt: string | null;
}
export interface MaintenanceDetail extends MaintenanceItem {
  description: string | null;
  updatedAt: string;
}
export interface CreateMaintenanceRequest {
  roomId: string;
  title: string;
  description?: string;
}
export type MaintenancePage = PageResponse<MaintenanceItem>;
export type PhysicalRoomPage = PageResponse<PhysicalRoom>;
