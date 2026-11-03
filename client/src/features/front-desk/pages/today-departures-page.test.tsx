// @vitest-environment jsdom

import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import {
  useCheckOutGuest,
  useEarlyCheckout,
  useEarlyCheckoutQuote,
  useTodayDepartures,
} from '../api/today-departure-query';
import type { TodayDeparture } from '../types/today-departure';
import { TodayDeparturesPage } from './today-departures-page';

const antdMocks = vi.hoisted(() => ({
  success: vi.fn(),
  error: vi.fn(),
}));

vi.mock('antd', async () => {
  const actual = await vi.importActual<typeof import('antd')>('antd');
  return {
    ...actual,
    App: {
      ...actual.App,
      useApp: () => ({ message: antdMocks, modal: {}, notification: {} }),
    },
  };
});

vi.mock('../api/today-departure-query', () => ({
  useCheckOutGuest: vi.fn(),
  useTodayDepartures: vi.fn(),
  useEarlyCheckout: vi.fn(),
  useEarlyCheckoutQuote: vi.fn(),
}));

const refetch = vi.fn();
const mutate = vi.fn();
const resetMutation = vi.fn();
const departure: TodayDeparture = {
  bookingId: '10000000-0000-0000-0000-000000000001',
  bookingCode: 'HAN-20260819-001',
  guestName: 'Nguyễn Văn A',
  guestPhone: '0901234567',
  roomTypeName: 'Deluxe',
  checkInDate: '2026-08-17',
  checkOutDate: '2026-08-19',
  adults: 2,
  children: 1,
  status: 'CHECKED_IN',
  totalAmount: 5_000_000,
  paidAmount: 3_500_000,
  outstandingAmount: 1_500_000,
};

const paidDeparture: TodayDeparture = {
  ...departure,
  paidAmount: 5_000_000,
  outstandingAmount: 0,
};

const pageData = (overrides: Record<string, unknown> = {}) => ({
  content: [departure],
  page: 0,
  size: 20,
  totalElements: 1,
  totalPages: 1,
  first: true,
  last: true,
  ...overrides,
});

const queryResult = (overrides: Record<string, unknown> = {}) => ({
  data: undefined,
  error: null,
  isLoading: false,
  isFetching: false,
  isError: false,
  refetch,
  ...overrides,
});

beforeEach(() => {
  vi.clearAllMocks();
  vi.mocked(useCheckOutGuest).mockReturnValue({
    mutate,
    reset: resetMutation,
    isPending: false,
  } as never);
  vi.mocked(useEarlyCheckout).mockReturnValue({
    mutate,
    reset: resetMutation,
    isPending: false,
  } as never);
  vi.mocked(useEarlyCheckoutQuote).mockReturnValue(queryResult() as never);
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

describe('TodayDeparturesPage', () => {
  it('hiển thị dữ liệu trả phòng bằng tiếng Việt', () => {
    vi.mocked(useTodayDepartures).mockReturnValue(queryResult({ data: pageData() }) as never);

    render(<TodayDeparturesPage />);

    expect(screen.getByText('HAN-20260819-001')).not.toBeNull();
    expect(screen.getByText('Nguyễn Văn A')).not.toBeNull();
    expect(screen.getByText('Quá hạn trả phòng')).not.toBeNull();
    expect(screen.getAllByText(/lượt đặt phòng/).length).toBeGreaterThan(0);
  });

  it('chỉ hiển thị trạng thái lỗi khi request đầu tiên thất bại', () => {
    vi.mocked(useTodayDepartures).mockReturnValue(
      queryResult({ isError: true, error: new Error('Máy chủ không phản hồi') }) as never,
    );

    render(<TodayDeparturesPage />);

    expect(screen.getByText('Không thể tải danh sách khách đang lưu trú')).not.toBeNull();
    expect(screen.queryByText('Hiện chưa có khách đang lưu trú')).toBeNull();
    expect(document.querySelector('.ant-table-wrapper')).toBeNull();
  });

  it('quay về trang hợp lệ khi tổng số trang giảm', async () => {
    vi.mocked(useTodayDepartures).mockImplementation(
      ({ page }) =>
        queryResult({
          data:
            page === 0
              ? pageData({ totalElements: 21, totalPages: 2, last: false })
              : pageData({ content: [], page: 1, totalElements: 1, totalPages: 1, first: false }),
        }) as never,
    );

    render(<TodayDeparturesPage />);
    const nextButton = document.querySelector<HTMLButtonElement>('.ant-pagination-next button');
    fireEvent.click(nextButton!);

    await waitFor(() => {
      expect(useTodayDepartures).toHaveBeenLastCalledWith({ page: 0, size: 20 });
    });
  });

  it('hiển thị nút trả phòng khi booking đã thanh toán đủ', () => {
    vi.mocked(useTodayDepartures).mockReturnValue(
      queryResult({ data: pageData({ content: [paidDeparture] }) }) as never,
    );

    render(<TodayDeparturesPage />);

    expect(screen.getByRole('button', { name: /trả phòng/i })).toHaveProperty('disabled', false);
  });

  it('cho mở modal trả phòng để thu thêm khi booking còn công nợ', () => {
    vi.mocked(useTodayDepartures).mockReturnValue(queryResult({ data: pageData() }) as never);

    render(<TodayDeparturesPage />);

    expect(screen.getByRole('button', { name: /trả phòng/i })).toHaveProperty('disabled', false);
    expect(screen.getByText('Cần thu thêm')).not.toBeNull();
  });

  it('mở modal và gửi note khi xác nhận trả phòng', async () => {
    vi.mocked(useTodayDepartures).mockReturnValue(
      queryResult({ data: pageData({ content: [paidDeparture] }) }) as never,
    );

    render(<TodayDeparturesPage />);

    fireEvent.click(screen.getByRole('button', { name: /trả phòng/i }));
    expect(screen.getByText(/Hoàn tất trả phòng cho mã đặt phòng/)).not.toBeNull();

    fireEvent.change(screen.getByPlaceholderText('Ví dụ: Khách đã trả đủ chìa khóa'), {
      target: { value: 'Khách đã trả chìa khóa' },
    });
    await waitFor(() =>
      expect(screen.getByRole('button', { name: /xác nhận trả phòng/i })).toHaveProperty(
        'disabled',
        false,
      ),
    );
    fireEvent.click(screen.getByRole('button', { name: /xác nhận trả phòng/i }));

    await waitFor(() => {
      expect(mutate).toHaveBeenCalledWith(
        {
          bookingId: paidDeparture.bookingId,
          payload: { note: 'Khách đã trả chìa khóa' },
        },
        expect.objectContaining({
          onError: expect.any(Function),
          onSuccess: expect.any(Function),
        }),
      );
    });
  });

  it('gửi payload rỗng khi không nhập ghi chú', async () => {
    vi.mocked(useTodayDepartures).mockReturnValue(
      queryResult({ data: pageData({ content: [paidDeparture] }) }) as never,
    );

    render(<TodayDeparturesPage />);

    fireEvent.click(screen.getByRole('button', { name: /trả phòng/i }));
    await waitFor(() =>
      expect(screen.getByRole('button', { name: /xác nhận trả phòng/i })).toHaveProperty(
        'disabled',
        false,
      ),
    );
    fireEvent.click(screen.getByRole('button', { name: /xác nhận trả phòng/i }));

    await waitFor(() => {
      expect(mutate).toHaveBeenCalledWith(
        {
          bookingId: paidDeparture.bookingId,
          payload: undefined,
        },
        expect.any(Object),
      );
    });
  });

  it('gửi phí phát sinh và số tiền thu thêm khi xác nhận trả phòng', async () => {
    vi.mocked(useTodayDepartures).mockReturnValue(
      queryResult({ data: pageData({ content: [paidDeparture] }) }) as never,
    );

    render(<TodayDeparturesPage />);

    fireEvent.click(screen.getByRole('button', { name: /trả phòng/i }));
    fireEvent.change(screen.getByLabelText('Phí phát sinh / đền bù'), {
      target: { value: '100000' },
    });
    fireEvent.change(screen.getByLabelText('Lý do phát sinh'), {
      target: { value: 'Mất thẻ phòng' },
    });
    await waitFor(() =>
      expect(screen.getByLabelText('Tiền mặt thu cho khoản còn phải trả')).toHaveProperty(
        'disabled',
        false,
      ),
    );
    fireEvent.change(screen.getByLabelText('Tiền mặt thu cho khoản còn phải trả'), {
      target: { value: '100000' },
    });
    await waitFor(() =>
      expect(screen.getByRole('button', { name: /xác nhận trả phòng/i })).toHaveProperty(
        'disabled',
        false,
      ),
    );
    fireEvent.click(screen.getByRole('button', { name: /xác nhận trả phòng/i }));

    await waitFor(() => {
      expect(mutate).toHaveBeenCalledWith(
        {
          bookingId: paidDeparture.bookingId,
          payload: {
            additionalChargeAmount: 100000,
            additionalChargeReason: 'Mất thẻ phòng',
            collectedAmount: 100000,
          },
        },
        expect.objectContaining({
          onError: expect.any(Function),
          onSuccess: expect.any(Function),
        }),
      );
    });
  });

  it('chặn submit khi có phí phát sinh nhưng thiếu lý do', async () => {
    vi.mocked(useTodayDepartures).mockReturnValue(
      queryResult({ data: pageData({ content: [paidDeparture] }) }) as never,
    );

    render(<TodayDeparturesPage />);

    fireEvent.click(screen.getByRole('button', { name: /trả phòng/i }));
    fireEvent.change(screen.getByLabelText('Phí phát sinh / đền bù'), {
      target: { value: '100000' },
    });
    await waitFor(() =>
      expect(screen.getByLabelText('Tiền mặt thu cho khoản còn phải trả')).toHaveProperty(
        'disabled',
        false,
      ),
    );
    fireEvent.change(screen.getByLabelText('Tiền mặt thu cho khoản còn phải trả'), {
      target: { value: '100000' },
    });
    await waitFor(() =>
      expect(screen.getByRole('button', { name: /xác nhận trả phòng/i })).toHaveProperty(
        'disabled',
        false,
      ),
    );
    fireEvent.click(screen.getByRole('button', { name: /xác nhận trả phòng/i }));

    await waitFor(() => {
      expect(screen.getByText('Lý do phát sinh là bắt buộc khi có phí phát sinh')).not.toBeNull();
      expect(mutate).not.toHaveBeenCalled();
    });
  });

  it('không tự ghi nhận thu tiền khi mở booking còn nợ', () => {
    vi.mocked(useTodayDepartures).mockReturnValue(queryResult({ data: pageData() }) as never);
    render(<TodayDeparturesPage />);
    fireEvent.click(screen.getByRole('button', { name: /trả phòng/i }));
    expect(screen.getByLabelText('Tiền mặt thu cho khoản còn phải trả')).toHaveProperty(
      'value',
      '0',
    );
    expect(screen.getByRole('button', { name: /xác nhận trả phòng/i })).toHaveProperty(
      'disabled',
      true,
    );
  });

  it('chặn thu vượt công nợ đã cộng phí đền bù', async () => {
    vi.mocked(useTodayDepartures).mockReturnValue(
      queryResult({ data: pageData({ content: [paidDeparture] }) }) as never,
    );
    render(<TodayDeparturesPage />);
    fireEvent.click(screen.getByRole('button', { name: /trả phòng/i }));
    fireEvent.change(screen.getByLabelText('Phí phát sinh / đền bù'), {
      target: { value: '200000' },
    });
    await waitFor(() =>
      expect(screen.getByLabelText('Tiền mặt thu cho khoản còn phải trả')).toHaveProperty(
        'disabled',
        false,
      ),
    );
    fireEvent.change(screen.getByLabelText('Tiền mặt thu cho khoản còn phải trả'), {
      target: { value: '300000' },
    });
    await waitFor(() =>
      expect(screen.getByRole('button', { name: /xác nhận trả phòng/i })).toHaveProperty(
        'disabled',
        true,
      ),
    );
    expect(mutate).not.toHaveBeenCalled();
  });

  it('dùng số dư mới khi thanh toán cập nhật trong lúc mở modal', async () => {
    vi.mocked(useTodayDepartures).mockReturnValue(queryResult({ data: pageData() }) as never);
    const { rerender } = render(<TodayDeparturesPage />);
    fireEvent.click(screen.getByRole('button', { name: /trả phòng/i }));
    await waitFor(() =>
      expect(screen.getByLabelText('Tiền mặt thu cho khoản còn phải trả')).toHaveProperty(
        'disabled',
        false,
      ),
    );
    fireEvent.change(screen.getByLabelText('Tiền mặt thu cho khoản còn phải trả'), {
      target: { value: '1500000' },
    });
    await waitFor(() =>
      expect(screen.getByRole('button', { name: /xác nhận trả phòng/i })).toHaveProperty(
        'disabled',
        false,
      ),
    );
    vi.mocked(useTodayDepartures).mockReturnValue(
      queryResult({ data: pageData({ content: [paidDeparture] }) }) as never,
    );
    rerender(<TodayDeparturesPage />);
    await waitFor(() =>
      expect(screen.getByRole('button', { name: /xác nhận trả phòng/i })).toHaveProperty(
        'disabled',
        true,
      ),
    );
  });

  it('đóng modal và báo thành công khi check-out thành công', async () => {
    mutate.mockImplementation((_input, options) =>
      options.onSuccess({
        bookingId: paidDeparture.bookingId,
        bookingCode: paidDeparture.bookingCode,
        bookingStatus: 'CHECKED_OUT',
        room: {
          roomId: '20000000-0000-0000-0000-000000000001',
          roomNumber: '0501',
          floor: 5,
          status: 'DIRTY',
        },
        checkedOutAt: '2026-08-24T08:30:00Z',
        totalAmount: paidDeparture.totalAmount,
        paidAmount: paidDeparture.paidAmount,
        outstandingAmount: 0,
        additionalChargeAmount: 0,
        additionalChargeReason: null,
        collectedAmount: 0,
      }),
    );
    vi.mocked(useTodayDepartures).mockReturnValue(
      queryResult({ data: pageData({ content: [paidDeparture] }) }) as never,
    );

    render(<TodayDeparturesPage />);

    fireEvent.click(screen.getByRole('button', { name: /trả phòng/i }));
    await waitFor(() =>
      expect(screen.getByRole('button', { name: /xác nhận trả phòng/i })).toHaveProperty(
        'disabled',
        false,
      ),
    );
    fireEvent.click(screen.getByRole('button', { name: /xác nhận trả phòng/i }));

    await waitFor(() => {
      expect(antdMocks.success).toHaveBeenCalledWith('Trả phòng thành công, phòng 0501 cần dọn.');
      expect(resetMutation).toHaveBeenCalled();
    });
  });

  it('hiển thị lỗi thân thiện khi check-out thất bại', async () => {
    mutate.mockImplementation((_input, options) => options.onError(new Error('Máy chủ lỗi')));
    vi.mocked(useTodayDepartures).mockReturnValue(
      queryResult({ data: pageData({ content: [paidDeparture] }) }) as never,
    );

    render(<TodayDeparturesPage />);

    fireEvent.click(screen.getByRole('button', { name: /trả phòng/i }));
    await waitFor(() =>
      expect(screen.getByRole('button', { name: /xác nhận trả phòng/i })).toHaveProperty(
        'disabled',
        false,
      ),
    );
    fireEvent.click(screen.getByRole('button', { name: /xác nhận trả phòng/i }));

    await waitFor(() => {
      expect(antdMocks.error).toHaveBeenCalledWith('Máy chủ lỗi');
    });
  });

  it('chặn ghi chú vượt quá 500 ký tự', async () => {
    vi.mocked(useTodayDepartures).mockReturnValue(
      queryResult({ data: pageData({ content: [paidDeparture] }) }) as never,
    );

    render(<TodayDeparturesPage />);

    fireEvent.click(screen.getByRole('button', { name: /trả phòng/i }));
    fireEvent.change(screen.getByPlaceholderText('Ví dụ: Khách đã trả đủ chìa khóa'), {
      target: { value: 'a'.repeat(501) },
    });
    await waitFor(() =>
      expect(screen.getByRole('button', { name: /xác nhận trả phòng/i })).toHaveProperty(
        'disabled',
        false,
      ),
    );
    fireEvent.click(screen.getByRole('button', { name: /xác nhận trả phòng/i }));

    await waitFor(() => {
      expect(screen.getByText('Ghi chú không được vượt quá 500 ký tự')).not.toBeNull();
      expect(mutate).not.toHaveBeenCalled();
    });
  });
});
