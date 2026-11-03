import type { PageResponse } from '../../../types/api';

export interface DirtyRoomTypeSummary {
  id: string;
  code: string;
  name: string;
}

export interface DirtyRoom {
  roomId: string;
  roomNumber: string;
  floor: number;
  status: 'DIRTY' | 'CLEANING' | 'AVAILABLE';
  note: string | null;
  roomType: DirtyRoomTypeSummary;
}

export interface DirtyRoomsParams {
  page: number;
  size: number;
}

export type DirtyRoomsPage = PageResponse<DirtyRoom>;
