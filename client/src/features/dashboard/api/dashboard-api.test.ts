import { beforeEach, describe, expect, it, vi } from 'vitest';

import { apiClient } from '../../../lib/axios';
import { getDashboardReport } from './dashboard-api';

vi.mock('../../../lib/axios', () => ({
  apiClient: { get: vi.fn() },
}));

describe('getDashboardReport', () => {
  beforeEach(() => vi.clearAllMocks());

  it('calls the authenticated report endpoint and unwraps data', async () => {
    const report = { period: { from: '2026-08-01', to: '2026-08-04', currency: 'VND' } };
    vi.mocked(apiClient.get).mockResolvedValue({ data: { data: report } });

    await expect(getDashboardReport({ from: '2026-08-01', to: '2026-08-04' })).resolves.toBe(
      report,
    );
    expect(apiClient.get).toHaveBeenCalledWith('/reports/dashboard', {
      params: { from: '2026-08-01', to: '2026-08-04', currency: 'VND' },
    });
  });

  it('propagates normalized API errors', async () => {
    const error = new Error('Không thể tải báo cáo');
    vi.mocked(apiClient.get).mockRejectedValue(error);

    await expect(
      getDashboardReport({ from: '2026-08-01', to: '2026-08-04', currency: 'VND' }),
    ).rejects.toBe(error);
  });
});
