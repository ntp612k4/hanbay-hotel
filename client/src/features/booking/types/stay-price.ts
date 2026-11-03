import type { ApiResponse } from '../../../types/api';

export interface CalculateStayPriceRequest {
  roomTypeId: string;
  checkInDate: string;
  checkOutDate: string;
}

export interface StayPriceRoomType {
  id: string;
  code: string;
  name: string;
}

export interface NightlyStayPrice {
  date: string;
  finalPrice: number;
}

export interface CalculateStayPriceResponse {
  roomType: StayPriceRoomType;
  checkInDate: string;
  checkOutDate: string;
  numberOfNights: number;
  nightlyPrices: NightlyStayPrice[];
  totalPrice: number;
}

export type CalculateStayPriceApiResponse = ApiResponse<CalculateStayPriceResponse>;
