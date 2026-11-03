import { beforeEach, describe, expect, it, vi } from 'vitest';

import { apiClient } from '../../../lib/axios';
import { checkOutGuest, getTodayDepartures } from './today-departure-api';

vi.mock('../../../lib/axios', () => ({
  apiClient: { get: vi.fn(), patch: vi.fn() },
}));

const page = {
  content: [],
  page: 0,
  size: 20,
  totalElements: 0,
  totalPages: 0,
  first: true,
  last: true,
};

describe('getTodayDepartures', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('gửi phân trang và trả về danh sách khách trả phòng hôm nay', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: { data: page } });
    const params = { page: 0, size: 20 };

    await expect(getTodayDepartures(params)).resolves.toBe(page);
    expect(apiClient.get).toHaveBeenCalledWith('/front-desk/departures/today', { params });
  });

  it('gửi request check-out đúng bookingId và note', async () => {
    const result = {
      bookingId: '10000000-0000-0000-0000-000000000001',
      bookingCode: 'HAN-20260819-001',
      bookingStatus: 'CHECKED_OUT',
      room: {
        roomId: '20000000-0000-0000-0000-000000000001',
        roomNumber: '0501',
        floor: 5,
        status: 'DIRTY',
      },
      checkedOutAt: '2026-08-24T08:30:00Z',
      totalAmount: 5_000_000,
      paidAmount: 5_000_000,
      outstandingAmount: 0,
      additionalChargeAmount: 100_000,
      additionalChargeReason: 'Mất thẻ phòng',
      collectedAmount: 100_000,
    };
    vi.mocked(apiClient.patch).mockResolvedValue({ data: { data: result } });

    await expect(
      checkOutGuest('10000000-0000-0000-0000-000000000001', {
        note: 'Khách đã trả đủ chìa khóa',
        additionalChargeAmount: 100_000,
        additionalChargeReason: 'Mất thẻ phòng',
        collectedAmount: 100_000,
      }),
    ).resolves.toBe(result);
    expect(apiClient.patch).toHaveBeenCalledWith(
      '/bookings/10000000-0000-0000-0000-000000000001/check-out',
      {
        note: 'Khách đã trả đủ chìa khóa',
        additionalChargeAmount: 100_000,
        additionalChargeReason: 'Mất thẻ phòng',
        collectedAmount: 100_000,
      },
    );
  });

  it('cho phép check-out không có body', async () => {
    const result = {
      bookingId: '10000000-0000-0000-0000-000000000001',
      bookingCode: 'HAN-20260819-001',
      bookingStatus: 'CHECKED_OUT',
      room: {
        roomId: '20000000-0000-0000-0000-000000000001',
        roomNumber: '0501',
        floor: 5,
        status: 'DIRTY',
      },
      checkedOutAt: '2026-08-24T08:30:00Z',
      totalAmount: 5_000_000,
      paidAmount: 5_000_000,
      outstandingAmount: 0,
      additionalChargeAmount: 0,
      additionalChargeReason: null,
      collectedAmount: 0,
    };
    vi.mocked(apiClient.patch).mockResolvedValue({ data: { data: result } });

    await expect(checkOutGuest('10000000-0000-0000-0000-000000000001')).resolves.toBe(result);
    expect(apiClient.patch).toHaveBeenCalledWith(
      '/bookings/10000000-0000-0000-0000-000000000001/check-out',
      undefined,
    );
  });
});
