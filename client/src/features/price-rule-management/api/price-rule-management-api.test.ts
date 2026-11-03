import { beforeEach, describe, expect, it, vi } from 'vitest';

import { apiClient } from '../../../lib/axios';
import {
  createPriceRule,
  getManagedPriceRule,
  getManagedPriceRules,
  updatePriceRule,
  updatePriceRuleStatus,
} from './price-rule-management-api';
import type { CreatePriceRulePayload } from '../types/price-rule-management';

vi.mock('../../../lib/axios', () => ({
  apiClient: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
  },
}));

const item = {
  id: 'rule-id',
  roomType: {
    id: 'room-type-id',
    code: 'DLX',
    name: 'Deluxe',
  },
  name: 'Weekend surcharge Deluxe',
  ruleType: 'WEEKEND',
  adjustmentType: 'PERCENTAGE',
  adjustmentValue: 20,
  adjustmentDirection: 'INCREASE' as const,
  startDate: '2026-09-01',
  endDate: '2026-12-31',
  priority: 10,
  active: true,
  createdAt: '2026-08-29T08:00:00Z',
  updatedAt: null,
};

beforeEach(() => vi.clearAllMocks());

describe('price rule management API', () => {
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
      getManagedPriceRules({
        page: 0,
        size: 10,
        search: '  weekend  ',
        roomTypeId: 'room-type-id',
        ruleType: 'WEEKEND',
        active: false,
        sort: 'startDate,desc',
      }),
    ).resolves.toEqual(page);

    expect(apiClient.get).toHaveBeenCalledWith('/management/price-rules', {
      params: {
        page: 0,
        size: 10,
        search: 'weekend',
        roomTypeId: 'room-type-id',
        ruleType: 'WEEKEND',
        active: false,
        sort: 'startDate,desc',
      },
    });
  });

  it('omits blank and undefined list filters', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: { data: { content: [] } } });

    await getManagedPriceRules({ page: 1, size: 10, search: ' ', sort: 'name,asc' });

    expect(apiClient.get).toHaveBeenCalledWith('/management/price-rules', {
      params: { page: 1, size: 10, sort: 'name,asc' },
    });
  });

  it('uses the contract paths and payloads for detail and mutations', async () => {
    const payload: CreatePriceRulePayload = {
      roomTypeId: item.roomType.id,
      name: item.name,
      ruleType: 'WEEKEND',
      adjustmentType: 'PERCENTAGE',
      adjustmentValue: item.adjustmentValue,
      adjustmentDirection: item.adjustmentDirection,
      startDate: item.startDate,
      endDate: item.endDate,
      priority: item.priority,
    };
    vi.mocked(apiClient.get).mockResolvedValue({ data: { data: item } });
    vi.mocked(apiClient.post).mockResolvedValue({ data: { data: item } });
    vi.mocked(apiClient.put).mockResolvedValue({ data: { data: item } });
    vi.mocked(apiClient.patch).mockResolvedValue({ data: { data: item } });

    await getManagedPriceRule('rule-id');
    await createPriceRule(payload);
    await updatePriceRule('rule-id', payload);
    await updatePriceRuleStatus('rule-id', false);

    expect(apiClient.get).toHaveBeenCalledWith('/management/price-rules/rule-id');
    expect(apiClient.post).toHaveBeenCalledWith('/management/price-rules', payload);
    expect(apiClient.put).toHaveBeenCalledWith('/management/price-rules/rule-id', payload);
    expect(apiClient.patch).toHaveBeenCalledWith('/management/price-rules/rule-id/status', {
      active: false,
    });
  });

  it('sends a seasonal discount direction without converting it to a promotion', async () => {
    const payload: CreatePriceRulePayload = {
      roomTypeId: item.roomType.id,
      name: item.name,
      ruleType: 'SEASONAL',
      adjustmentType: 'PERCENTAGE',
      adjustmentValue: item.adjustmentValue,
      adjustmentDirection: 'DISCOUNT',
      startDate: item.startDate,
      endDate: item.endDate,
      priority: item.priority,
    };
    vi.mocked(apiClient.post).mockResolvedValue({ data: { data: item } });

    await createPriceRule(payload);

    expect(apiClient.post).toHaveBeenCalledWith('/management/price-rules', payload);
  });
});
