// @vitest-environment jsdom
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { useActiveHolidays } from '../hooks/use-holiday-calendar';
import { HolidayRuleNotice } from './holiday-rule-notice';
vi.mock('../hooks/use-holiday-calendar', () => ({ useActiveHolidays: vi.fn() }));
afterEach(cleanup);
describe('HolidayRuleNotice', () => {
  it('warns for a non-overlapping calendar and opens calendar management', () => {
    vi.mocked(useActiveHolidays).mockReturnValue({
      data: [{ name: 'Tết', startDate: '2027-02-01', endDate: '2027-02-07' }],
    } as never);
    const onManage = vi.fn();
    render(<HolidayRuleNotice startDate="2027-03-01" endDate="2027-03-02" onManage={onManage} />);
    expect(screen.getByText(/Chưa có lịch lễ đang hoạt động/)).toBeTruthy();
    fireEvent.click(screen.getByText('Quản lý lịch ngày lễ'));
    expect(onManage).toHaveBeenCalledOnce();
  });
  it('includes the last holiday date in overlap detection', () => {
    vi.mocked(useActiveHolidays).mockReturnValue({
      data: [{ name: 'Tết', startDate: '2027-02-01', endDate: '2027-02-07' }],
    } as never);
    render(<HolidayRuleNotice startDate="2027-02-07" endDate="2027-02-08" onManage={vi.fn()} />);
    expect(screen.getByText('Tết')).toBeTruthy();
    expect(screen.queryByText(/Chưa có lịch lễ đang hoạt động/)).toBeNull();
  });
  it('distinguishes a failed lookup from an empty calendar and offers retry', () => {
    const refetch = vi.fn();
    vi.mocked(useActiveHolidays).mockReturnValue({ isError: true, refetch } as never);
    render(<HolidayRuleNotice onManage={vi.fn()} />);
    expect(screen.getByText(/Không kiểm tra được lịch ngày lễ/)).toBeTruthy();
    fireEvent.click(screen.getByText('Thử lại'));
    expect(refetch).toHaveBeenCalledOnce();
  });
});
