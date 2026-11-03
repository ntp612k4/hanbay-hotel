// @vitest-environment jsdom

import '@testing-library/jest-dom/vitest';

import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { useDashboardReport } from '../api/dashboard-query';
import type { DashboardReport } from '../types/dashboard-report';
import { DashboardPage } from './dashboard-page';

vi.mock('../api/dashboard-query', () => ({
  useDashboardReport: vi.fn(),
}));

const report: DashboardReport = {
  period: { from: '2026-08-01', to: '2026-08-04', currency: 'VND' },
  revenue: { grossRevenue: 1_200_000, refundedAmount: 200_000, netRevenue: 1_000_000 },
  occupancy: { occupiedRoomNights: 9, availableRoomNights: 12, occupancyRate: 75 },
  bookings: {
    totalBookings: 4,
    pendingBookings: 0,
    confirmedBookings: 2,
    checkedInBookings: 0,
    checkedOutBookings: 1,
    cancelledBookings: 1,
    expiredBookings: 0,
    totalBookingValue: 2_000_000,
    averageBookingValue: 500_000,
    cancellationRate: 25,
  },
  revenueTrend: [
    { date: '2026-08-01', grossRevenue: 1_200_000, refundedAmount: 200_000, netRevenue: 1_000_000 },
  ],
  occupancyTrend: [
    { date: '2026-08-01', occupiedRoomNights: 9, availableRoomNights: 12, occupancyRate: 75 },
  ],
  bookingTrend: [{ date: '2026-08-01', totalBookings: 4 }],
};

const queryResult = (overrides: Record<string, unknown> = {}) => ({
  data: undefined,
  error: null,
  isLoading: false,
  isError: false,
  isFetching: false,
  refetch: vi.fn(),
  ...overrides,
});

const startDateInput = () => screen.getByLabelText('Từ ngày');
const endDateInput = () => screen.getByLabelText('Đến ngày');

describe('DashboardPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    globalThis.ResizeObserver = class ResizeObserver {
      observe() {}
      unobserve() {}
      disconnect() {}
    };
    window.matchMedia = vi.fn().mockImplementation((query: string) => ({
      matches: false,
      media: query,
      onchange: null,
      addListener: vi.fn(),
      removeListener: vi.fn(),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn(),
    }));
  });
  afterEach(cleanup);

  it('renders KPI cards and all report charts', () => {
    vi.mocked(useDashboardReport).mockReturnValue(queryResult({ data: report }) as never);

    render(<DashboardPage />);

    expect(screen.getByRole('heading', { name: 'Tổng quan khách sạn' })).toBeInTheDocument();
    expect(screen.getByText('Tiền thu sau hoàn')).toBeInTheDocument();
    expect(screen.getAllByText('1.000.000 ₫')).not.toHaveLength(0);
    expect(screen.queryByText('Công suất phòng')).not.toBeInTheDocument();
    expect(screen.getByText('Chi phí bảo trì')).toBeInTheDocument();
    expect(screen.getByText('Còn lại sau bảo trì')).toBeInTheDocument();
    expect(screen.getAllByText('4')).not.toHaveLength(0);
    expect(screen.getByLabelText('Xu hướng tiền thu sau hoàn')).toBeInTheDocument();
    expect(screen.getByLabelText('Chi phí bảo trì theo ngày')).toBeInTheDocument();
    expect(screen.getByLabelText('Xu hướng đặt phòng')).toBeInTheDocument();
    expect(screen.getByLabelText('Booking theo trạng thái')).toBeInTheDocument();
  });

  it('shows maintenance-only spending and warns about missing costs', () => {
    vi.mocked(useDashboardReport).mockReturnValue(
      queryResult({
        data: {
          ...report,
          revenue: { grossRevenue: 0, refundedAmount: 0, netRevenue: 0 },
          bookings: { ...report.bookings, totalBookings: 0 },
          maintenance: {
            recordedExpense: 500000,
            cashAfterMaintenance: -500000,
            completedWithoutCost: 2,
            trend: [{ date: '2026-08-01', amount: 500000 }],
          },
        },
      }) as never,
    );
    render(<DashboardPage />);
    expect(screen.getByText('-500.000 ₫')).toBeInTheDocument();
    expect(
      screen.getByText(/2 yêu cầu bảo trì hoàn thành trong kỳ chưa nhập chi phí/),
    ).toBeInTheDocument();
    expect(
      screen.queryByText('Chưa có dữ liệu kinh doanh trong khoảng thời gian này'),
    ).not.toBeInTheDocument();
  });

  it('shows a skeleton while loading', () => {
    vi.mocked(useDashboardReport).mockReturnValue(queryResult({ isLoading: true }) as never);

    render(<DashboardPage />);

    expect(screen.getByRole('status', { name: 'Đang tải báo cáo dashboard' })).toBeInTheDocument();
  });

  it('shows API feedback and retries after an error', () => {
    const refetch = vi.fn();
    vi.mocked(useDashboardReport).mockReturnValue(
      queryResult({ isError: true, error: new Error('Máy chủ không phản hồi'), refetch }) as never,
    );

    render(<DashboardPage />);
    expect(screen.getByText('Không thể tải báo cáo tổng quan')).toBeInTheDocument();
    expect(screen.getByText('Máy chủ không phản hồi')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Thử lại' }));
    expect(refetch).toHaveBeenCalledOnce();
  });

  it('keeps the last successful report visible when a refresh fails', () => {
    vi.mocked(useDashboardReport).mockReturnValue(
      queryResult({ data: report, isError: true, error: new Error('Mất kết nối') }) as never,
    );

    render(<DashboardPage />);

    expect(screen.getByText('Không thể tải báo cáo tổng quan')).toBeInTheDocument();
    expect(screen.getByText('Tiền thu sau hoàn')).toBeInTheDocument();
    expect(screen.getByLabelText('Xu hướng tiền thu sau hoàn')).toBeInTheDocument();
  });

  it('shows an empty state when all business metrics are zero', () => {
    vi.mocked(useDashboardReport).mockReturnValue(
      queryResult({
        data: {
          ...report,
          revenue: { grossRevenue: 0, refundedAmount: 0, netRevenue: 0 },
          occupancy: { occupiedRoomNights: 0, availableRoomNights: 12, occupancyRate: 0 },
          bookings: {
            ...report.bookings,
            totalBookings: 0,
            confirmedBookings: 0,
            checkedOutBookings: 0,
            cancelledBookings: 0,
          },
        },
      }) as never,
    );

    render(<DashboardPage />);

    expect(
      screen.getByText('Chưa có dữ liệu kinh doanh trong khoảng thời gian này'),
    ).toBeInTheDocument();
    expect(screen.queryByLabelText('Xu hướng tiền thu sau hoàn')).not.toBeInTheDocument();
  });

  it('applies the selected inclusive date range to the report query', () => {
    vi.mocked(useDashboardReport).mockReturnValue(queryResult({ data: report }) as never);

    render(<DashboardPage />);
    fireEvent.change(startDateInput(), { target: { value: '2026-07-10' } });
    fireEvent.change(endDateInput(), { target: { value: '2026-07-20' } });
    fireEvent.click(screen.getByRole('button', { name: 'Áp dụng' }));

    expect(useDashboardReport).toHaveBeenLastCalledWith({
      from: '2026-07-10',
      to: '2026-07-21',
      currency: 'VND',
    });
    expect(screen.getByText('10/07/2026 – 20/07/2026')).toBeInTheDocument();
  });

  it('validates an inverted date range without changing the report query', () => {
    vi.mocked(useDashboardReport).mockReturnValue(queryResult({ data: report }) as never);

    render(<DashboardPage />);
    const callsBeforeSubmit = vi.mocked(useDashboardReport).mock.calls.length;
    fireEvent.change(startDateInput(), { target: { value: '2026-07-20' } });
    fireEvent.change(endDateInput(), { target: { value: '2026-07-10' } });
    fireEvent.click(screen.getByRole('button', { name: 'Áp dụng' }));

    expect(screen.getByRole('alert')).toHaveTextContent(
      'Ngày kết thúc không được trước ngày bắt đầu.',
    );
    expect(useDashboardReport).toHaveBeenCalledTimes(callsBeforeSubmit);
  });

  it('rejects a report period longer than 366 days', () => {
    vi.mocked(useDashboardReport).mockReturnValue(queryResult({ data: report }) as never);

    render(<DashboardPage />);
    fireEvent.change(startDateInput(), { target: { value: '2025-01-01' } });
    fireEvent.change(endDateInput(), { target: { value: '2026-01-02' } });
    fireEvent.click(screen.getByRole('button', { name: 'Áp dụng' }));

    expect(screen.getByRole('alert')).toHaveTextContent(
      'Khoảng báo cáo không được vượt quá 366 ngày.',
    );
  });

  it('resets an applied custom period to the current month', () => {
    vi.mocked(useDashboardReport).mockReturnValue(queryResult({ data: report }) as never);

    render(<DashboardPage />);
    const initialFrom = (startDateInput() as HTMLInputElement).value;
    const initialTo = (endDateInput() as HTMLInputElement).value;
    fireEvent.change(startDateInput(), { target: { value: '2026-07-10' } });
    fireEvent.change(endDateInput(), { target: { value: '2026-07-20' } });
    fireEvent.click(screen.getByRole('button', { name: 'Áp dụng' }));
    fireEvent.click(screen.getByRole('button', { name: 'Tháng hiện tại' }));

    expect(startDateInput()).toHaveValue(initialFrom);
    expect(endDateInput()).toHaveValue(initialTo);
  });
});
