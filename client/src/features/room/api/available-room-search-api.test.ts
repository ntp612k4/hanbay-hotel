import { describe, expect, it, vi } from 'vitest';

import { apiClient } from '../../../lib/axios';
import { searchAvailableRoomTypes } from './available-room-search-api';

vi.mock('../../../lib/axios', () => ({
  apiClient: { get: vi.fn() },
}));

describe('searchAvailableRoomTypes', () => {
  it('gửi tiêu chí tìm kiếm và phân trang rồi trả về page response', async () => {
    const page = {
      content: [],
      page: 1,
      size: 6,
      totalElements: 8,
      totalPages: 2,
      first: false,
      last: true,
    };
    vi.mocked(apiClient.get).mockResolvedValue({ data: { data: page } });
    const params = {
      checkIn: '2026-08-12',
      checkOut: '2026-08-15',
      adults: 2,
      children: 0,
      page: 1,
      size: 6,
    };

    await expect(searchAvailableRoomTypes(params)).resolves.toBe(page);
    expect(apiClient.get).toHaveBeenCalledWith('/room-types/available', { params });
  });

  it('gửi roomTypeId khi kiểm tra lại loại phòng đã chọn', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: { data: { content: [] } } });
    const params = {
      checkIn: '2026-08-12',
      checkOut: '2026-08-15',
      adults: 2,
      children: 0,
      roomTypeId: '10000000-0000-0000-0000-000000000001',
      page: 0,
      size: 1,
    };

    await searchAvailableRoomTypes(params);
    expect(apiClient.get).toHaveBeenCalledWith('/room-types/available', { params });
  });
});
