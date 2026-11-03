import { beforeEach, describe, expect, it, vi } from 'vitest';

import { apiClient } from '../../../lib/axios';
import { getRoomTypeCatalog } from './room-type-api';

vi.mock('../../../lib/axios', () => ({
  apiClient: {
    get: vi.fn(),
  },
}));

const catalogPage = {
  content: [
    {
      id: '1718e764-a396-460a-8806-382a603eb90d',
      name: 'Deluxe Ocean View',
      description: 'Phòng hướng biển',
      capacityAdults: 2,
      capacityChildren: 1,
      basePrice: 2450000,
      thumbnailUrl: null,
    },
  ],
  page: 0,
  size: 6,
  totalElements: 1,
  totalPages: 1,
  first: true,
  last: true,
};

describe('getRoomTypeCatalog', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('calls the public catalog endpoint and unwraps the response data', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      data: {
        success: true,
        message: 'Room type catalog retrieved successfully',
        data: catalogPage,
      },
    });

    const result = await getRoomTypeCatalog({
      page: 0,
      size: 6,
      keyword: '  Deluxe  ',
      sortBy: 'basePrice',
      direction: 'asc',
    });

    expect(apiClient.get).toHaveBeenCalledWith('/room-types', {
      params: {
        page: 0,
        size: 6,
        keyword: 'Deluxe',
        sortBy: 'basePrice',
        direction: 'asc',
      },
    });
    expect(result).toEqual(catalogPage);
  });

  it('does not send an empty keyword', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      data: { success: true, message: 'Success', data: catalogPage },
    });

    await getRoomTypeCatalog({
      page: 0,
      size: 6,
      keyword: '   ',
      sortBy: 'name',
      direction: 'asc',
    });

    expect(apiClient.get).toHaveBeenCalledWith('/room-types', {
      params: {
        page: 0,
        size: 6,
        keyword: undefined,
        sortBy: 'name',
        direction: 'asc',
      },
    });
  });
});
