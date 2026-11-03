import { beforeEach, describe, expect, it, vi } from 'vitest';
import { apiClient } from '../../../lib/axios';
import { getActiveHolidays, saveHoliday, setHolidayStatus } from './holiday-calendar-api';
vi.mock('../../../lib/axios', () => ({
  apiClient: { get: vi.fn(), post: vi.fn(), put: vi.fn(), patch: vi.fn() },
}));
beforeEach(() => vi.clearAllMocks());
describe('holiday calendar API', () => {
  it('loads every active page so later holidays are not reported missing', async () => {
    vi.mocked(apiClient.get)
      .mockResolvedValueOnce({ data: { data: { content: [{ id: 'first' }], last: false } } })
      .mockResolvedValueOnce({ data: { data: { content: [{ id: 'second' }], last: true } } });
    expect(await getActiveHolidays()).toEqual([{ id: 'first' }, { id: 'second' }]);
    expect(apiClient.get).toHaveBeenLastCalledWith('/management/holiday-calendar', {
      params: { page: 1, size: 100, sort: 'startDate,desc', active: true },
    });
  });
  it('does not return an incomplete calendar when a later page fails', async () => {
    vi.mocked(apiClient.get)
      .mockResolvedValueOnce({ data: { data: { content: [], last: false } } })
      .mockRejectedValueOnce(new Error('offline'));
    await expect(getActiveHolidays()).rejects.toThrow('offline');
  });
  it('creates, updates and disables a calendar entry', async () => {
    const values = { name: 'Tết', startDate: '2027-02-01', endDate: '2027-02-07' };
    await saveHoliday(values);
    await saveHoliday(values, 'holiday');
    await setHolidayStatus('holiday', false);
    expect(apiClient.post).toHaveBeenCalledWith('/management/holiday-calendar', values);
    expect(apiClient.put).toHaveBeenCalledWith('/management/holiday-calendar/holiday', values);
    expect(apiClient.patch).toHaveBeenCalledWith('/management/holiday-calendar/holiday/status', {
      active: false,
    });
  });
});
