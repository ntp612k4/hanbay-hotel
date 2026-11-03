// @vitest-environment jsdom

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { App, ConfigProvider } from 'antd';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';

import { ApiClientError } from '../../../types/api';
import { checkInGuest, getAvailablePhysicalRooms } from '../api/booking-api';
import { CheckInGuestPage } from './check-in-guest-page';

vi.mock('../api/booking-api', () => ({
  checkInGuest: vi.fn(),
  getAvailablePhysicalRooms: vi.fn(),
}));

const ROOM_ID = '30000000-0000-4000-8000-000000000001';

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

beforeEach(() => vi.clearAllMocks());
afterEach(cleanup);

describe('CheckInGuestPage', () => {
  it('gọi API và hiển thị phòng khi check-in thành công', async () => {
    vi.mocked(getAvailablePhysicalRooms).mockResolvedValue([
      { roomId: ROOM_ID, roomNumber: '0501', floor: 5 },
    ]);
    vi.mocked(checkInGuest).mockResolvedValue({
      bookingId: 'booking-id',
      bookingCode: 'BK20260819000001',
      bookingStatus: 'CHECKED_IN',
      room: { roomId: ROOM_ID, roomNumber: '0501', floor: 5, status: 'OCCUPIED' },
      checkedInAt: '2026-08-19T07:00:00Z',
    });
    renderPage();

    fireEvent.change(screen.getByLabelText('Mã booking'), {
      target: { value: 'bk20260819000001' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Tìm phòng khả dụng' }));
    await waitFor(() => expect(getAvailablePhysicalRooms).toHaveBeenCalled());
    const roomOption = await screen.findByText(/Phòng 0501 · Tầng 5/);
    fireEvent.click(roomOption);
    expect(
      roomOption.closest('label')?.classList.contains('ant-radio-button-wrapper-checked'),
    ).toBe(true);
    fireEvent.click(screen.getByRole('button', { name: /Xác nhận check-in/ }));

    expect(await screen.findByText('Check-in thành công')).toBeTruthy();
    expect(screen.getByText('0501')).toBeTruthy();
    expect(checkInGuest).toHaveBeenCalledWith('BK20260819000001', ROOM_ID);
  });

  it('hiển thị business error dễ hiểu', async () => {
    vi.mocked(getAvailablePhysicalRooms).mockResolvedValue([
      { roomId: ROOM_ID, roomNumber: '0501', floor: 5 },
    ]);
    vi.mocked(checkInGuest).mockRejectedValue(
      new ApiClientError({
        kind: 'api',
        status: 409,
        code: 'ROOM_NOT_AVAILABLE',
        message: 'Conflict',
      }),
    );
    renderPage();

    fireEvent.change(screen.getByLabelText('Mã booking'), {
      target: { value: 'BK20260819000001' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Tìm phòng khả dụng' }));
    await waitFor(() => expect(getAvailablePhysicalRooms).toHaveBeenCalled());
    fireEvent.click(await screen.findByText(/Phòng 0501 · Tầng 5/));
    fireEvent.click(screen.getByRole('button', { name: /Xác nhận check-in/ }));

    expect(
      await screen.findByText('Phòng đã chọn không còn trống. Vui lòng chọn phòng khác.'),
    ).toBeTruthy();
  });

  it('thông báo khi booking không còn phòng khả dụng', async () => {
    vi.mocked(getAvailablePhysicalRooms).mockResolvedValue([]);
    renderPage();

    fireEvent.change(screen.getByLabelText('Mã booking'), {
      target: { value: 'BK20260819000001' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Tìm phòng khả dụng' }));

    expect(
      await screen.findByText('Không còn phòng khả dụng thuộc hạng phòng đã đặt'),
    ).toBeTruthy();
    expect(checkInGuest).not.toHaveBeenCalled();
  });
});

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <ConfigProvider>
        <App>
          <CheckInGuestPage />
        </App>
      </ConfigProvider>
    </QueryClientProvider>,
  );
}
