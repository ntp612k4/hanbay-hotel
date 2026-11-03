import { apiClient } from '../../../lib/axios';
import type { ApiResponse, PageResponse } from '../../../types/api';
import type {
  BookingHistoryItem,
  BookingHistoryParams,
  AvailablePhysicalRoom,
  CancellationEligibility,
  CancelBookingResponse,
  CheckInGuestResponse,
  CreatePaymentRequestResponse,
  CreatePendingBookingRequest,
  PendingBookingSummary,
  PaymentStatusResponse,
  RefundInformation,
  ExtendStayResponse,
} from '../types/booking';

export async function extendStay(
  bookingId: string,
  newCheckOutDate: string,
): Promise<ExtendStayResponse> {
  const response = await apiClient.post<ApiResponse<ExtendStayResponse>>(
    `/bookings/${encodeURIComponent(bookingId)}/extend`,
    { newCheckOutDate },
  );
  return response.data.data;
}

export async function getAvailablePhysicalRooms(
  bookingCode: string,
): Promise<AvailablePhysicalRoom[]> {
  const response = await apiClient.get<ApiResponse<AvailablePhysicalRoom[]>>(
    `/bookings/${encodeURIComponent(bookingCode)}/available-rooms`,
  );
  return response.data.data;
}

export async function checkInGuest(
  bookingCode: string,
  roomId: string,
): Promise<CheckInGuestResponse> {
  const response = await apiClient.post<ApiResponse<CheckInGuestResponse>>(
    `/bookings/${encodeURIComponent(bookingCode)}/check-in`,
    { roomId },
  );
  return response.data.data;
}

export async function getMyBookings(
  params: BookingHistoryParams,
): Promise<PageResponse<BookingHistoryItem>> {
  const response = await apiClient.get<ApiResponse<PageResponse<BookingHistoryItem>>>(
    '/bookings/my-bookings',
    { params: { ...params, status: params.status || undefined } },
  );
  return response.data.data;
}

export async function getBooking(bookingCode: string): Promise<BookingHistoryItem> {
  const response = await apiClient.get<ApiResponse<BookingHistoryItem>>(
    `/bookings/${encodeURIComponent(bookingCode)}`,
  );
  return response.data.data;
}

export async function getCancellationEligibility(
  bookingCode: string,
): Promise<CancellationEligibility> {
  const response = await apiClient.get<ApiResponse<CancellationEligibility>>(
    `/bookings/${bookingCode}/cancellation`,
  );
  return response.data.data;
}

export async function cancelBooking(
  bookingId: string,
  reason?: string,
  refundInformation?: RefundInformation,
): Promise<CancelBookingResponse> {
  const response = await apiClient.post<ApiResponse<CancelBookingResponse>>(
    `/bookings/${bookingId}/cancel`,
    { reason, refundInformation },
  );
  return response.data.data;
}

export async function createPendingBooking(
  payload: CreatePendingBookingRequest,
): Promise<PendingBookingSummary> {
  const response = await apiClient.post<ApiResponse<PendingBookingSummary>>('/bookings', payload);

  return response.data.data;
}

export async function updatePendingBooking(
  bookingId: string,
  payload: CreatePendingBookingRequest,
  bookingAccessToken: string,
): Promise<PendingBookingSummary> {
  const response = await apiClient.patch<ApiResponse<PendingBookingSummary>>(
    `/bookings/${bookingId}`,
    payload,
    { headers: { 'X-Booking-Access-Token': bookingAccessToken } },
  );
  return response.data.data;
}

export async function createPaymentRequest(
  bookingId: string,
  bookingAccessToken?: string,
): Promise<CreatePaymentRequestResponse> {
  const response = await apiClient.post<ApiResponse<CreatePaymentRequestResponse>>(
    '/payments/requests',
    { bookingId },
    bookingAccessToken ? { headers: { 'X-Booking-Access-Token': bookingAccessToken } } : undefined,
  );
  return response.data.data;
}

export async function getPaymentStatus(
  paymentId: string,
  bookingAccessToken?: string,
): Promise<PaymentStatusResponse> {
  const response = await apiClient.get<ApiResponse<PaymentStatusResponse>>(
    `/payments/${paymentId}/status`,
    bookingAccessToken ? { headers: { 'X-Booking-Access-Token': bookingAccessToken } } : undefined,
  );
  return response.data.data;
}
