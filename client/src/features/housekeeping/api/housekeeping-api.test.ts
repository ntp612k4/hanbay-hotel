import { beforeEach, describe, expect, it, vi } from 'vitest';

import { apiClient } from '../../../lib/axios';
import { completeCleaning, getDirtyRooms, startCleaning } from './housekeeping-api';

vi.mock('../../../lib/axios', () => ({
  apiClient: { get: vi.fn(), patch: vi.fn() },
}));

describe('getDirtyRooms', () => {
  beforeEach(() => vi.clearAllMocks());

  it('gọi đúng endpoint với phân trang và unwrap response data', async () => {
    const page = {
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: true,
    };
    vi.mocked(apiClient.get).mockResolvedValue({ data: { data: page } });

    await expect(getDirtyRooms({ page: 0, size: 20 })).resolves.toBe(page);

    expect(apiClient.get).toHaveBeenCalledWith('/housekeeping/rooms', {
      params: { page: 0, size: 20 },
    });
  });

  it('gọi đúng mutation bắt đầu và hoàn tất dọn phòng', async () => {
    const room = { roomId: 'room-id', status: 'CLEANING' };
    vi.mocked(apiClient.patch).mockResolvedValue({ data: { data: room } });

    await expect(startCleaning('room/id')).resolves.toBe(room);
    expect(apiClient.patch).toHaveBeenCalledWith('/housekeeping/rooms/room%2Fid/start-cleaning');

    await expect(completeCleaning('room/id')).resolves.toBe(room);
    expect(apiClient.patch).toHaveBeenCalledWith('/housekeeping/rooms/room%2Fid/complete-cleaning');
  });

  it('propagate lỗi đã được API client normalize', async () => {
    const error = new Error('Không thể tải danh sách phòng cần dọn');
    vi.mocked(apiClient.get).mockRejectedValue(error);

    await expect(getDirtyRooms({ page: 1, size: 20 })).rejects.toBe(error);
  });
});
