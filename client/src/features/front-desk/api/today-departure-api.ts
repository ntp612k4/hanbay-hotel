import { apiClient } from '../../../lib/axios';
import type { ApiResponse } from '../../../types/api';
import type {
  CheckOutGuestRequest,
  CheckOutGuestResponse,
  TodayDeparturePage,
  TodayDepartureParams,
  EarlyCheckoutRequest,
  EarlyCheckoutResponse,
} from '../types/today-departure';

export async function getTodayDepartures(
  params: TodayDepartureParams,
): Promise<TodayDeparturePage> {
  const response = await apiClient.get<ApiResponse<TodayDeparturePage>>(
    '/front-desk/departures/today',
    { params },
  );

  return response.data.data;
}

export async function quoteEarlyCheckout(
  bookingId: string,
  actualCheckOutDate: string,
): Promise<EarlyCheckoutResponse> {
  const response = await apiClient.get<ApiResponse<EarlyCheckoutResponse>>(
    `/bookings/${encodeURIComponent(bookingId)}/early-checkout`,
    { params: { actualCheckOutDate } },
  );
  return response.data.data;
}

export async function earlyCheckout(
  bookingId: string,
  payload: EarlyCheckoutRequest,
): Promise<EarlyCheckoutResponse> {
  const response = await apiClient.post<ApiResponse<EarlyCheckoutResponse>>(
    `/bookings/${encodeURIComponent(bookingId)}/early-checkout`,
    payload,
  );
  return response.data.data;
}

export async function checkOutGuest(
  bookingId: string,
  payload?: CheckOutGuestRequest,
): Promise<CheckOutGuestResponse> {
  const response = await apiClient.patch<ApiResponse<CheckOutGuestResponse>>(
    `/bookings/${encodeURIComponent(bookingId)}/check-out`,
    payload,
  );

  return response.data.data;
}
