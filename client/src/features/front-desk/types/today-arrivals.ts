import type { PageResponse } from '../../../types/api';

export interface PrimaryGuestSummary {
  fullName: string;
  phone: string | null;
  email: string | null;
}

export interface ArrivalRoomType {
  id: string;
  code: string;
  name: string;
}

export interface TodayArrival {
  bookingId: string;
  bookingCode: string;
  primaryGuest: PrimaryGuestSummary | null;
  roomType: ArrivalRoomType;
  checkInDate: string;
  checkOutDate: string;
  adultCount: number;
  childCount: number;
  bookingStatus: 'CONFIRMED';
  createdAt: string;
}

export interface TodayArrivalsParams {
  page: number;
  size: number;
}

export type TodayArrivalsPage = PageResponse<TodayArrival>;
