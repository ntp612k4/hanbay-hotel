// @vitest-environment jsdom
import { render, screen, fireEvent, cleanup } from '@testing-library/react';
import { afterEach, expect, it, vi } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { App } from 'antd';
import { RefundRequestsPage } from './refund-requests-page';
import { getPendingRefunds } from '../api/refund-api';
vi.mock('../api/refund-api', () => ({ getPendingRefunds: vi.fn(), confirmRefund: vi.fn() }));
afterEach(cleanup);
it('shows the bank, recipient and full account number including leading zeros before confirmation', async () => {
  globalThis.ResizeObserver = class {
    observe() {}
    unobserve() {}
    disconnect() {}
  };
  window.matchMedia = vi.fn().mockImplementation(() => ({
    matches: false,
    addListener() {},
    removeListener() {},
    addEventListener() {},
    removeEventListener() {},
  }));
  vi.mocked(getPendingRefunds).mockResolvedValue({
    content: [
      {
        refundId: 'refund-1',
        bookingCode: 'BOOK-1',
        paymentCode: 'PAY-1',
        amount: 200000,
        currency: 'VND',
        reason: 'Trả phòng sớm',
        status: 'PENDING',
        bankReference: null,
        requestedAt: '2026-09-08T03:00:00Z',
        refundedAt: null,
        bankName: 'Vietcombank',
        accountHolderName: 'NGUYEN VAN A',
        accountNumber: '00123456789',
      },
    ],
    page: 0,
    size: 10,
    totalElements: 1,
    totalPages: 1,
    first: true,
    last: true,
  });
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={client}>
      <App>
        <RefundRequestsPage />
      </App>
    </QueryClientProvider>,
  );
  expect(await screen.findByText('00123456789')).toBeTruthy();
  expect(screen.getByText('Vietcombank')).toBeTruthy();
  expect(screen.getByText('NGUYEN VAN A')).toBeTruthy();
  fireEvent.click(screen.getByRole('button', { name: 'Xác nhận đã hoàn' }));
  expect(await screen.findByText('Ngân hàng nhận')).toBeTruthy();
  expect(screen.getAllByText('00123456789')).toHaveLength(2);
  client.clear();
});
