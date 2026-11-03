import type { PageResponse } from '../../../types/api';
import type { RefundInformation } from '../../payment/types/refund';

export interface TodayDeparture {
  bookingId: string;
  bookingCode: string;
  guestName: string;
  guestPhone: string | null;
  roomTypeName: string;
  checkInDate: string;
  checkOutDate: string;
  adults: number;
  children: number;
  status: 'CHECKED_IN';
  totalAmount: number;
  paidAmount: number;
  outstandingAmount: number;
}

export type TodayDeparturePage = PageResponse<TodayDeparture>;

export interface TodayDepartureParams {
  page: number;
  size: number;
}

export interface CheckOutGuestRequest {
  note?: string;
  additionalChargeAmount?: number;
  additionalChargeReason?: string;
  collectedAmount?: number;
}

export interface CheckedOutRoom {
  roomId: string;
  roomNumber: string;
  floor: number;
  status: 'DIRTY';
}

export interface CheckOutGuestResponse {
  bookingId: string;
  bookingCode: string;
  bookingStatus: 'CHECKED_OUT';
  room: CheckedOutRoom;
  checkedOutAt: string;
  totalAmount: number;
  paidAmount: number;
  outstandingAmount: number;
  additionalChargeAmount: number;
  additionalChargeReason: string | null;
  collectedAmount: number;
}

export interface EarlyCheckoutResponse {
  bookingId: string;
  bookingCode: string;
  bookingStatus: 'CHECKED_IN' | 'CHECKED_OUT';
  originalCheckOutDate: string;
  actualCheckOutDate: string;
  unusedNights: number;
  unusedAmount: number;
  penaltyPercent: number;
  penaltyAmount: number;
  refundAmount: number;
  currency: 'VND';
  refundStatus: 'PENDING' | null;
  room: CheckedOutRoom | null;
}

export interface EarlyCheckoutRequest {
  actualCheckOutDate: string;
  refundInformation?: RefundInformation;
  note?: string;
  additionalChargeAmount?: number;
  additionalChargeReason?: string;
  collectedAmount?: number;
}
