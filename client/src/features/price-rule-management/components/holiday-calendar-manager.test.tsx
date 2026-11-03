// @vitest-environment jsdom
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { App } from 'antd';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import { useHolidayCalendar, useHolidayMutations } from '../hooks/use-holiday-calendar';
import { HolidayCalendarManager } from './holiday-calendar-manager';
vi.mock('../hooks/use-holiday-calendar', () => ({
  useHolidayCalendar: vi.fn(),
  useHolidayMutations: vi.fn(),
}));
const save = vi.fn();
const status = vi.fn();
beforeEach(() => {
  vi.clearAllMocks();
  globalThis.ResizeObserver = class {
    observe() {}
    unobserve() {}
    disconnect() {}
  };
  window.matchMedia = vi.fn().mockImplementation(() => ({
    matches: false,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
  }));
  vi.mocked(useHolidayCalendar).mockReturnValue({
    data: {
      content: [
        { id: 'tet', name: 'Tết', startDate: '2027-02-01', endDate: '2027-02-07', active: true },
      ],
      totalElements: 1,
    },
  } as never);
  vi.mocked(useHolidayMutations).mockReturnValue({
    save: { mutateAsync: save },
    status: { mutateAsync: status },
  } as never);
});
afterEach(cleanup);
it('edits an existing holiday and preserves its dates', async () => {
  save.mockResolvedValue(undefined);
  render(
    <App>
      <HolidayCalendarManager onClose={vi.fn()} />
    </App>,
  );
  fireEvent.click(screen.getByText('Sửa'));
  fireEvent.change(screen.getByLabelText('Tên ngày lễ'), { target: { value: 'Tết Nguyên đán' } });
  fireEvent.click(screen.getByText('Lưu ngày lễ'));
  await waitFor(() =>
    expect(save).toHaveBeenCalledWith({
      id: 'tet',
      values: { name: 'Tết Nguyên đán', startDate: '2027-02-01', endDate: '2027-02-07' },
    }),
  );
});
it('retains the form after a failed save so the user can retry', async () => {
  save.mockRejectedValue(new Error('offline'));
  render(
    <App>
      <HolidayCalendarManager onClose={vi.fn()} />
    </App>,
  );
  fireEvent.click(screen.getByText('Sửa'));
  fireEvent.click(screen.getByText('Lưu ngày lễ'));
  await waitFor(() => expect(screen.getByText(/Không thể kết nối đến hệ thống/)).toBeTruthy());
  expect(screen.getByLabelText('Tên ngày lễ')).toBeTruthy();
});
it('requires confirmation before disabling a shared holiday', async () => {
  status.mockResolvedValue(undefined);
  render(
    <App>
      <HolidayCalendarManager onClose={vi.fn()} />
    </App>,
  );
  fireEvent.click(screen.getByText('Ngừng'));
  expect(status).not.toHaveBeenCalled();
  fireEvent.click(await screen.findByText('Xác nhận'));
  await waitFor(() => expect(status).toHaveBeenCalledWith({ id: 'tet', active: false }));
});
