import { describe, expect, it, vi } from 'vitest';
import { apiClient } from '../../../lib/axios';
import { getPhysicalRooms } from './maintenance-api';
vi.mock('../../../lib/axios', () => ({ apiClient: { get: vi.fn() } }));

describe('getPhysicalRooms', () => {
  it('loads rooms beyond the first 100 results', async () => {
    const first = Array.from({ length: 100 }, (_, index) => ({ roomId: String(index) }));
    vi.mocked(apiClient.get)
      .mockResolvedValueOnce({ data: { data: { content: first, totalPages: 2 } } })
      .mockResolvedValueOnce({ data: { data: { content: [{ roomId: '101' }], totalPages: 2 } } });
    expect(await getPhysicalRooms()).toHaveLength(101);
    expect(apiClient.get).toHaveBeenLastCalledWith('/rooms', { params: { page: 1, size: 100 } });
  });
  it('does not return an incomplete room list when a later page fails', async () => {
    vi.mocked(apiClient.get)
      .mockResolvedValueOnce({ data: { data: { content: [], totalPages: 2 } } })
      .mockRejectedValueOnce(new Error('Mất kết nối'));
    await expect(getPhysicalRooms()).rejects.toThrow('Mất kết nối');
  });
});
