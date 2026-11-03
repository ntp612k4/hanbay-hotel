// @vitest-environment jsdom

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { App, ConfigProvider, Form, Input } from 'antd';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';

import { AuthProvider } from '../../authentication/providers/auth-provider';
import { createPendingBooking } from '../api/booking-api';
import { CreateBookingPage } from './create-booking-page';

vi.mock('../api/catalog-api', () => ({
  getAddOns: vi.fn().mockResolvedValue([]),
}));

vi.mock('@features/room', () => ({
  searchAvailableRoomTypes: vi.fn().mockResolvedValue({
    content: [
      {
        id: '11111111-1111-4111-8111-111111111111',
        code: 'DELUXE',
        name: 'Deluxe',
        basePrice: 1_000_000,
        maxGuests: 4,
        description: null,
        bedType: null,
        areaM2: null,
        imageUrl: null,
        availableRooms: 1,
      },
    ],
    page: 0,
    size: 1,
    totalElements: 1,
    totalPages: 1,
    first: true,
    last: true,
  }),
}));

vi.mock('../api/booking-api', () => ({ createPendingBooking: vi.fn() }));

vi.mock('./guest-information-page', () => ({
  GuestInformationPage: ({ onSubmit }: { onSubmit: () => void }) => (
    <>
      <Form.Item name="fullName" initialValue="Nguyễn Văn A" hidden>
        <Input />
      </Form.Item>
      <Form.Item name="email" initialValue="guest@example.com" hidden>
        <Input />
      </Form.Item>
      <Form.Item name="phone" initialValue="0912345678" hidden>
        <Input />
      </Form.Item>
      <Form.Item name="identityNumber" initialValue="012345678901" hidden>
        <Input />
      </Form.Item>
      <button onClick={onSubmit}>Kiểm tra đơn hàng</button>
    </>
  ),
}));

vi.mock('./booking-payment-page', () => ({
  BookingPaymentPage: () => <div>Trang thanh toán</div>,
}));

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
  vi.spyOn(App, 'useApp').mockReturnValue({
    message: {
      success: vi.fn().mockResolvedValue(undefined),
      error: vi.fn().mockResolvedValue(undefined),
      warning: vi.fn().mockResolvedValue(undefined),
    } as unknown as ReturnType<typeof App.useApp>['message'],
    notification: {} as ReturnType<typeof App.useApp>['notification'],
    modal: {} as ReturnType<typeof App.useApp>['modal'],
  });
});

afterEach(() => {
  vi.restoreAllMocks();
});

describe('CreateBookingPage', () => {
  it('calls createPendingBooking and opens payment page after review', async () => {
    vi.mocked(createPendingBooking).mockResolvedValue({
      bookingId: '22222222-2222-4222-8222-222222222222',
      bookingCode: 'BKG-001',
      bookingAccessToken: 'booking-access-token',
      status: 'PENDING',
      totalAmount: 2_000_000,
      expiredAt: '2026-08-04T08:15:00Z',
    });
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });

    const { unmount } = render(
      <QueryClientProvider client={queryClient}>
        <ConfigProvider>
          <App>
            <AuthProvider>
              <MemoryRouter
                initialEntries={[
                  '/bookings/new?roomTypeId=11111111-1111-4111-8111-111111111111&checkIn=2026-08-10&checkOut=2026-08-12&adults=2&children=0',
                ]}
              >
                <CreateBookingPage />
              </MemoryRouter>
            </AuthProvider>
          </App>
        </ConfigProvider>
      </QueryClientProvider>,
    );

    fireEvent.click(await screen.findByRole('button', { name: 'Kiểm tra đơn hàng' }));

    await waitFor(() => expect(createPendingBooking).toHaveBeenCalledTimes(1));
    expect(vi.mocked(createPendingBooking).mock.calls[0]?.[0]).toEqual({
      roomTypeId: '11111111-1111-4111-8111-111111111111',
      checkInDate: '2026-08-10',
      checkOutDate: '2026-08-12',
      adults: 2,
      children: 0,
      guestInfo: {
        fullName: 'Nguyễn Văn A',
        email: 'guest@example.com',
        phone: '0912345678',
        identityNumber: '012345678901',
      },
      addOns: [],
    });
    expect(await screen.findByText('Trang thanh toán')).toBeTruthy();

    unmount();
    queryClient.clear();
  });
});
