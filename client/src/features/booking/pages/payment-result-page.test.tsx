// @vitest-environment jsdom

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ConfigProvider } from 'antd';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';

import { getPaymentStatus } from '../api/booking-api';
import { useAuth } from '../../authentication/providers/auth-context';
import type { PaymentStatusResponse } from '../types/booking';
import { storePaymentResultToken } from '../utils/payment-result-storage';
import { PaymentResultPage } from './payment-result-page';

vi.mock('@components/layout/MainHeader', () => ({
  MainHeader: () => <header>Header</header>,
}));

vi.mock('@components/layout/MainFooter', () => ({
  MainFooter: () => <footer>Footer</footer>,
}));

vi.mock('../api/booking-api', () => ({
  getPaymentStatus: vi.fn(),
}));

vi.mock('../../authentication/providers/auth-context', () => ({
  useAuth: vi.fn(),
}));

const successResponse: PaymentStatusResponse = {
  paymentId: 'payment-id',
  bookingId: 'booking-id',
  bookingCode: 'BK202608140001',
  transactionRef: 'PAY-202608140001',
  paymentStatus: 'SUCCESS',
  bookingStatus: 'CONFIRMED',
  roomTypeName: 'Junior Suite',
  checkInDate: '2026-09-01',
  checkOutDate: '2026-09-05',
  adultCount: 2,
  childCount: 1,
  totalNights: 4,
  amount: 2_450_000,
  paidAmount: 2_450_000,
  currency: 'VND',
  refundStatus: null,
  refundAmount: null,
  refundReason: null,
  paymentUrl: 'https://qr.example/payment.png',
  bankCode: 'Vietcombank',
  accountNumber: '0000000002',
  accountName: 'HANBAY HOTEL',
  transferContent: 'PAY202608140001',
  expiredAt: '2026-08-14T08:30:00Z',
  paidAt: '2026-08-14T08:20:00Z',
};

beforeAll(() => {
  globalThis.ResizeObserver = class ResizeObserver {
    observe() {}
    unobserve() {}
    disconnect() {}
  };

  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: vi.fn().mockImplementation(() => ({
      matches: false,
      addListener: vi.fn(),
      removeListener: vi.fn(),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn(),
    })),
  });
});

beforeEach(() => {
  window.sessionStorage.clear();
  vi.clearAllMocks();
  vi.mocked(useAuth).mockReturnValue({ isAuthenticated: false, roles: [] } as never);
});

afterEach(() => {
  cleanup();
});

describe('PaymentResultPage', () => {
  it('hiển thị kết quả thành công và chi tiết giao dịch', async () => {
    storePaymentResultToken('payment-id', 'booking-access-token');
    vi.mocked(getPaymentStatus).mockResolvedValue(successResponse);

    renderPaymentResultPage();

    expect(await screen.findByText('Thanh toán thành công')).toBeTruthy();
    expect(screen.getByText('BK202608140001')).toBeTruthy();
    expect(screen.getByText('PAY-202608140001')).toBeTruthy();
    expect(screen.getByText('Đã xác nhận')).toBeTruthy();
    expect(getPaymentStatus).toHaveBeenCalledWith('payment-id', 'booking-access-token');
  });

  it('cho khách đã đăng nhập mở chi tiết booking từ kết quả thanh toán', async () => {
    vi.mocked(useAuth).mockReturnValue({ isAuthenticated: true, roles: [] } as never);
    vi.mocked(getPaymentStatus).mockResolvedValue(successResponse);

    renderPaymentResultPage();

    const link = await screen.findByRole('link', { name: 'Xem chi tiết đặt phòng' });
    expect(link.getAttribute('href')).toBe('/account/bookings/BK202608140001');
  });

  it('không hiển thị deep-link booking owner cho tài khoản nhân viên', async () => {
    vi.mocked(useAuth).mockReturnValue({
      isAuthenticated: true,
      roles: ['RECEPTIONIST'],
    } as never);
    vi.mocked(getPaymentStatus).mockResolvedValue(successResponse);

    renderPaymentResultPage();

    expect(await screen.findByText('Thanh toán thành công')).toBeTruthy();
    expect(screen.queryByRole('link', { name: 'Xem chi tiết đặt phòng' })).toBeNull();
  });

  it('hiển thị phản hồi rõ ràng khi thanh toán thất bại', async () => {
    storePaymentResultToken('payment-id', 'booking-access-token');
    vi.mocked(getPaymentStatus).mockResolvedValue({
      ...successResponse,
      paymentStatus: 'FAILED',
      bookingStatus: 'PENDING',
      paidAmount: 0,
      paidAt: null,
    });

    renderPaymentResultPage();

    expect(await screen.findByText('Thanh toán không thành công')).toBeTruthy();
    expect(screen.getByText('Thất bại')).toBeTruthy();
    expect(screen.getByText('Chờ thanh toán')).toBeTruthy();
  });

  it('hiển thị trạng thái chờ và chi tiết giao dịch đang được đối soát', async () => {
    storePaymentResultToken('payment-id', 'booking-access-token');
    vi.mocked(getPaymentStatus).mockResolvedValue({
      ...successResponse,
      paymentStatus: 'PENDING',
      bookingStatus: 'PENDING',
      paidAmount: 0,
      paidAt: null,
    });

    renderPaymentResultPage();

    expect(await screen.findByText('Đang chờ xác nhận thanh toán')).toBeTruthy();
    expect(screen.getByText('Đang xử lý')).toBeTruthy();
    expect(screen.getAllByText('Chưa ghi nhận').length).toBeGreaterThan(0);
  });

  it('hiển thị hướng dẫn tạo lại giao dịch khi yêu cầu đã hết hạn', async () => {
    storePaymentResultToken('payment-id', 'booking-access-token');
    vi.mocked(getPaymentStatus).mockResolvedValue({
      ...successResponse,
      paymentStatus: 'EXPIRED',
      bookingStatus: 'EXPIRED',
      paidAmount: 0,
      paidAt: null,
    });

    renderPaymentResultPage();

    expect(await screen.findByText('Yêu cầu thanh toán đã hết hạn')).toBeTruthy();
    expect(screen.getAllByText('Hết hạn')).toHaveLength(2);
    expect(screen.getByRole('button', { name: /Đặt phòng khác/ })).toBeTruthy();
  });

  it('cho phép thử lại sau khi API trạng thái tạm thời bị lỗi', async () => {
    storePaymentResultToken('payment-id', 'booking-access-token');
    vi.mocked(getPaymentStatus)
      .mockRejectedValueOnce(new Error('network error'))
      .mockResolvedValueOnce(successResponse);

    renderPaymentResultPage();

    expect(await screen.findByText('Không thể kiểm tra giao dịch')).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: /Thử lại/ }));

    expect(await screen.findByText('Thanh toán thành công')).toBeTruthy();
    expect(getPaymentStatus).toHaveBeenCalledTimes(2);
  });

  it('hiển thị refund đang chờ thay vì báo sai thanh toán thất bại', async () => {
    storePaymentResultToken('payment-id', 'booking-access-token');
    vi.mocked(getPaymentStatus).mockResolvedValue({
      ...successResponse,
      bookingStatus: 'CANCELLED',
      refundStatus: 'PENDING',
      refundAmount: 2_450_000,
      refundReason: 'Thanh toán đến sau khi booking đã hủy',
    });

    renderPaymentResultPage();

    expect(await screen.findByText('Đã nhận tiền, đang xử lý hoàn tiền')).toBeTruthy();
    expect(screen.getAllByText('Thanh toán đến sau khi booking đã hủy').length).toBeGreaterThan(0);
    expect(screen.getByText('Chờ xử lý')).toBeTruthy();
  });

  it('ưu tiên kết quả đã hoàn tiền dù booking đã hết hạn', async () => {
    storePaymentResultToken('payment-id', 'booking-access-token');
    vi.mocked(getPaymentStatus).mockResolvedValue({
      ...successResponse,
      paymentStatus: 'REFUNDED',
      bookingStatus: 'EXPIRED',
      refundStatus: 'SUCCESS',
      refundAmount: 500_000,
      refundReason: 'Thanh toán sau khi booking hết hạn',
    });

    renderPaymentResultPage();

    expect(await screen.findByText('Khoản tiền đã được hoàn lại')).toBeTruthy();
    expect(screen.queryByText('Yêu cầu thanh toán đã hết hạn')).toBeNull();
  });

  it('gọi API bằng JWT của owner khi không còn booking access token', async () => {
    vi.mocked(getPaymentStatus).mockResolvedValue(successResponse);

    renderPaymentResultPage();

    expect(await screen.findByText('Thanh toán thành công')).toBeTruthy();
    expect(getPaymentStatus).toHaveBeenCalledWith('payment-id', undefined);
  });
});

function renderPaymentResultPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <ConfigProvider>
        <MemoryRouter initialEntries={['/payments/payment-id/result']}>
          <Routes>
            <Route path="/payments/:paymentId/result" element={<PaymentResultPage />} />
          </Routes>
        </MemoryRouter>
      </ConfigProvider>
    </QueryClientProvider>,
  );
}
