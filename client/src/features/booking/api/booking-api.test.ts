import { beforeEach, describe, expect, it, vi } from 'vitest';

import { apiClient } from '../../../lib/axios';
import {
  cancelBooking,
  checkInGuest,
  getCancellationEligibility,
  getAvailablePhysicalRooms,
  getBooking,
  getMyBookings,
  getPaymentStatus,
} from './booking-api';

vi.mock('../../../lib/axios', () => ({
  apiClient: {
    get: vi.fn(),
    post: vi.fn(),
    patch: vi.fn(),
  },
}));

describe('getMyBookings', () => {
  beforeEach(() => vi.clearAllMocks());

  it('gọi endpoint lịch sử với phân trang và trạng thái', async () => {
    const page = {
      content: [],
      page: 0,
      size: 6,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: true,
    };
    vi.mocked(apiClient.get).mockResolvedValue({ data: { data: page } });

    await expect(getMyBookings({ page: 0, size: 6, status: 'CONFIRMED' })).resolves.toBe(page);

    expect(apiClient.get).toHaveBeenCalledWith('/bookings/my-bookings', {
      params: { page: 0, size: 6, status: 'CONFIRMED' },
    });
  });

  it('không gửi status rỗng', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: { data: { content: [] } } });

    await getMyBookings({ page: 1, size: 6 });

    expect(apiClient.get).toHaveBeenCalledWith('/bookings/my-bookings', {
      params: { page: 1, size: 6, status: undefined },
    });
  });
});

describe('getBooking', () => {
  it('mã hóa booking code và unwrap chi tiết', async () => {
    const booking = { bookingCode: 'BK/001' };
    vi.mocked(apiClient.get).mockResolvedValue({ data: { data: booking } });

    await expect(getBooking('BK/001')).resolves.toBe(booking);
    expect(apiClient.get).toHaveBeenCalledWith('/bookings/BK%2F001');
  });
});

describe('checkInGuest', () => {
  beforeEach(() => vi.clearAllMocks());

  it('gửi command check-in theo booking code và room ID', async () => {
    const result = {
      bookingCode: 'BK20260819000001',
      bookingStatus: 'CHECKED_IN',
      room: { roomId: 'room-id', roomNumber: '0501', floor: 5, status: 'OCCUPIED' },
    };
    vi.mocked(apiClient.post).mockResolvedValue({ data: { data: result } });

    await expect(
      checkInGuest('BK20260819/000001', '30000000-0000-4000-8000-000000000001'),
    ).resolves.toBe(result);

    expect(apiClient.post).toHaveBeenCalledWith('/bookings/BK20260819%2F000001/check-in', {
      roomId: '30000000-0000-4000-8000-000000000001',
    });
  });
});

describe('getAvailablePhysicalRooms', () => {
  beforeEach(() => vi.clearAllMocks());

  it('lấy phòng vật lý khả dụng theo booking code', async () => {
    const rooms = [{ roomId: 'room-id', roomNumber: '0501', floor: 5 }];
    vi.mocked(apiClient.get).mockResolvedValue({ data: { data: rooms } });

    await expect(getAvailablePhysicalRooms('BK20260819/000001')).resolves.toBe(rooms);

    expect(apiClient.get).toHaveBeenCalledWith('/bookings/BK20260819%2F000001/available-rooms');
  });
});

describe('booking cancellation', () => {
  beforeEach(() => vi.clearAllMocks());

  it('kiểm tra chính sách hủy theo mã đặt phòng', async () => {
    const eligibility = { canCancel: true, refundPercent: 50, refundAmount: 1_000_000 };
    vi.mocked(apiClient.get).mockResolvedValue({ data: { data: eligibility } });

    await expect(getCancellationEligibility('BK20260810000001')).resolves.toBe(eligibility);

    expect(apiClient.get).toHaveBeenCalledWith('/bookings/BK20260810000001/cancellation');
  });

  it('gửi yêu cầu hủy với lý do', async () => {
    const result = { bookingCode: 'BK20260810000001', status: 'CANCELLED' };
    vi.mocked(apiClient.post).mockResolvedValue({ data: { data: result } });

    await expect(cancelBooking('BK20260810000001', 'Thay đổi kế hoạch')).resolves.toBe(result);

    expect(apiClient.post).toHaveBeenCalledWith('/bookings/BK20260810000001/cancel', {
      reason: 'Thay đổi kế hoạch',
    });
  });
});

describe('getPaymentStatus', () => {
  beforeEach(() => vi.clearAllMocks());

  it('gửi booking access token khi lấy kết quả thanh toán', async () => {
    const paymentStatus = {
      paymentId: 'payment-id',
      bookingId: 'booking-id',
      bookingCode: 'BK202608140001',
      transactionRef: 'PAY-202608140001',
      paymentStatus: 'SUCCESS',
      bookingStatus: 'CONFIRMED',
      amount: 2_450_000,
      paidAmount: 2_450_000,
      currency: 'VND',
      refundStatus: null,
      refundAmount: null,
      refundReason: null,
      paymentUrl: 'https://qr.example/payment.png',
      bankCode: 'Vietcombank',
      accountNumber: '0000000002',
      accountName: 'HANBAY HOTEL',
      transferContent: 'PAY202608140001',
      expiredAt: '2026-08-14T08:30:00Z',
      paidAt: '2026-08-14T08:20:00Z',
    };
    vi.mocked(apiClient.get).mockResolvedValue({ data: { data: paymentStatus } });

    await expect(getPaymentStatus('payment-id', 'booking-access-token')).resolves.toBe(
      paymentStatus,
    );

    expect(apiClient.get).toHaveBeenCalledWith('/payments/payment-id/status', {
      headers: { 'X-Booking-Access-Token': 'booking-access-token' },
    });
  });

  it('dùng JWT của owner khi không có booking access token', async () => {
    const paymentStatus = { paymentId: 'payment-id', paymentStatus: 'SUCCESS' };
    vi.mocked(apiClient.get).mockResolvedValue({ data: { data: paymentStatus } });

    await expect(getPaymentStatus('payment-id')).resolves.toBe(paymentStatus);

    expect(apiClient.get).toHaveBeenCalledWith('/payments/payment-id/status', undefined);
  });
});
