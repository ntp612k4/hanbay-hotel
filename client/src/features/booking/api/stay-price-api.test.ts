import { describe, expect, it, vi } from 'vitest';

import { apiClient } from '../../../lib/axios';
import { calculateStayPrice } from './stay-price-api';

vi.mock('../../../lib/axios', () => ({
  apiClient: { post: vi.fn() },
}));

describe('calculateStayPrice', () => {
  it('gửi đúng request và trả data từ ApiResponse', async () => {
    const request = {
      roomTypeId: '10000000-0000-0000-0000-000000000001',
      checkInDate: '2026-08-20',
      checkOutDate: '2026-08-23',
    };
    const data = {
      roomType: { id: request.roomTypeId, code: 'DLX', name: 'Deluxe' },
      checkInDate: request.checkInDate,
      checkOutDate: request.checkOutDate,
      numberOfNights: 3,
      nightlyPrices: [],
      totalPrice: 3_300_000,
    };
    const controller = new AbortController();
    vi.mocked(apiClient.post).mockResolvedValue({ data: { data } });

    await expect(calculateStayPrice(request, controller.signal)).resolves.toBe(data);
    expect(apiClient.post).toHaveBeenCalledWith('/pricing/stay-price-calculations', request, {
      signal: controller.signal,
    });
  });
});
