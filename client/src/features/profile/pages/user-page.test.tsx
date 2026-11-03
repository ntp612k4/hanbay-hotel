// @vitest-environment jsdom

import { App } from 'antd';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';

import { useAuth } from '../../authentication/providers/auth-context';
import { useCurrentProfile, useUpdateCurrentProfile } from '../hooks/use-profile';
import { UserPage } from './user-page';

vi.mock('../../../components/layout/MainHeader', () => ({ MainHeader: () => <header /> }));
vi.mock('../../../components/layout/MainFooter', () => ({ MainFooter: () => <footer /> }));
vi.mock('../../authentication/providers/auth-context', () => ({ useAuth: vi.fn() }));
vi.mock('../../booking/pages/booking-list-page', () => ({
  BookingHistorySection: () => <section>Lịch sử tích hợp</section>,
}));
vi.mock('../hooks/use-profile', () => ({
  useCurrentProfile: vi.fn(),
  useUpdateCurrentProfile: vi.fn(),
}));

const profile = {
  id: '96a8f221-c24d-4a52-a91d-b110fbde8c0e',
  email: 'guest@example.com',
  fullName: 'Nguyễn Văn A',
  phone: '0901234567',
  status: 'ACTIVE' as const,
  roles: [],
  createdAt: '2026-08-01T08:30:00Z',
  updatedAt: '2026-08-10T10:00:00Z',
};

const mutate = vi.fn();
const reset = vi.fn();

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

afterEach(cleanup);

describe('UserPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(useAuth).mockReturnValue({ updateUser: vi.fn() } as never);
    vi.mocked(useCurrentProfile).mockReturnValue({ data: profile } as never);
    vi.mocked(useUpdateCurrentProfile).mockReturnValue({
      mutate,
      reset,
      isPending: false,
      error: null,
    } as never);
  });

  it('renders editable and read-only profile fields', () => {
    render(
      <App>
        <UserPage />
      </App>,
    );

    expect((screen.getByLabelText('Họ và tên') as HTMLInputElement).value).toBe('Nguyễn Văn A');
    expect((screen.getByLabelText('Số điện thoại') as HTMLInputElement).value).toBe('0901234567');
    expect((screen.getByDisplayValue('guest@example.com') as HTMLInputElement).disabled).toBe(true);
    expect(screen.getByText('Đang hoạt động')).not.toBeNull();
    expect(screen.getAllByText('Khách hàng').length).toBeGreaterThan(0);
  });

  it('renders a loading state while profile is being fetched', () => {
    vi.mocked(useCurrentProfile).mockReturnValue({ isLoading: true } as never);

    render(
      <App>
        <UserPage />
      </App>,
    );

    expect(document.querySelector('.ant-skeleton')).not.toBeNull();
  });

  it('renders an error state with retry action', () => {
    vi.mocked(useCurrentProfile).mockReturnValue({
      isError: true,
      error: new Error('Mất kết nối'),
      refetch: vi.fn(),
    } as never);

    render(
      <App>
        <UserPage />
      </App>,
    );

    expect(screen.getByText('Không thể tải thông tin cá nhân')).not.toBeNull();
    expect(screen.getByRole('button', { name: 'Thử lại' })).not.toBeNull();
  });

  it('shows only implemented profile sections', () => {
    render(
      <App>
        <UserPage />
      </App>,
    );

    expect(screen.queryByText('Bảo mật')).toBeNull();
    expect(screen.queryByText('Tùy chọn thông báo')).toBeNull();
    expect(screen.queryByText('Thay đổi ảnh')).toBeNull();
  });

  it('opens booking history inside the profile page', () => {
    render(
      <App>
        <UserPage />
      </App>,
    );

    fireEvent.click(screen.getByText('Lịch sử đặt phòng'));

    expect(screen.getByText('Lịch sử tích hợp')).not.toBeNull();
  });

  it('submits only the changed field', async () => {
    render(
      <App>
        <UserPage />
      </App>,
    );

    fireEvent.change(screen.getByLabelText('Số điện thoại'), {
      target: { value: ' 0987654321 ' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Lưu thay đổi' }));

    await waitFor(() => expect(mutate).toHaveBeenCalled());
    expect(mutate.mock.calls[0][0]).toEqual({ phone: '0987654321' });
  });
});
