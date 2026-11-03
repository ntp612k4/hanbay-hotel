import { beforeEach, describe, expect, it, vi } from 'vitest';

import { apiClient } from '../../../lib/axios';
import {
  createAddOn,
  getManagedAddOn,
  getManagedAddOns,
  updateAddOn,
  updateAddOnStatus,
} from './addon-management-api';

vi.mock('../../../lib/axios', () => ({
  apiClient: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
  },
}));

const item = {
  id: 'addon-id',
  code: 'AIRPORT_PICKUP',
  name: 'Đón sân bay',
  description: null,
  price: 350000,
  active: true,
  createdAt: '2026-08-20T01:00:00Z',
  updatedAt: null,
};

beforeEach(() => vi.clearAllMocks());

describe('add-on management API', () => {
  it('sends trimmed list filters and unwraps page data', async () => {
    const page = {
      content: [item],
      page: 0,
      size: 10,
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true,
    };
    vi.mocked(apiClient.get).mockResolvedValue({ data: { data: page } });

    await expect(
      getManagedAddOns({
        page: 0,
        size: 10,
        search: '  airport  ',
        active: false,
        sort: 'price,desc',
      }),
    ).resolves.toEqual(page);

    expect(apiClient.get).toHaveBeenCalledWith('/management/add-ons', {
      params: { page: 0, size: 10, search: 'airport', active: false, sort: 'price,desc' },
    });
  });

  it('omits blank search and undefined active filters', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: { data: { content: [] } } });

    await getManagedAddOns({ page: 1, size: 10, search: ' ', sort: 'name,asc' });

    expect(apiClient.get).toHaveBeenCalledWith('/management/add-ons', {
      params: { page: 1, size: 10, sort: 'name,asc' },
    });
  });

  it('uses the contract paths and payloads for detail and mutations', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: { data: item } });
    vi.mocked(apiClient.post).mockResolvedValue({ data: { data: item } });
    vi.mocked(apiClient.put).mockResolvedValue({ data: { data: item } });
    vi.mocked(apiClient.patch).mockResolvedValue({ data: { data: item } });

    await getManagedAddOn('addon-id');
    await createAddOn({ code: item.code, name: item.name, price: item.price });
    await updateAddOn('addon-id', { name: item.name, description: null, price: item.price });
    await updateAddOnStatus('addon-id', false);

    expect(apiClient.get).toHaveBeenCalledWith('/management/add-ons/addon-id');
    expect(apiClient.post).toHaveBeenCalledWith('/management/add-ons', {
      code: item.code,
      name: item.name,
      price: item.price,
    });
    expect(apiClient.put).toHaveBeenCalledWith('/management/add-ons/addon-id', {
      name: item.name,
      description: null,
      price: item.price,
    });
    expect(apiClient.patch).toHaveBeenCalledWith('/management/add-ons/addon-id/status', {
      active: false,
    });
  });
});
