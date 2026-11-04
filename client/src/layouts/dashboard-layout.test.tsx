// @vitest-environment jsdom

import { cleanup, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter } from 'react-router-dom';

import { useAuth } from '../features/authentication/providers/auth-context';
import { USER_ROLES, type UserRole } from '../types/common';
import { DashboardLayout } from './dashboard-layout';

vi.mock('../features/authentication/providers/auth-context', () => ({ useAuth: vi.fn() }));

beforeEach(() => {
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

function renderLayout(roles: UserRole[]) {
  vi.mocked(useAuth).mockReturnValue({
    logout: vi.fn(),
    roles,
    session: { user: { fullName: 'Nhân viên' } },
  } as never);

  return render(
    <MemoryRouter>
      <DashboardLayout />
    </MemoryRouter>,
  );
}

describe('DashboardLayout role menu', () => {
  it('shows payment monitoring to an allowed manager', () => {
    renderLayout([USER_ROLES.manager]);

    expect(screen.getByText('Giám sát thanh toán')).not.toBeNull();
    expect(screen.getByText('Tổng quan')).not.toBeNull();
  });

  it('hides payment monitoring from a denied receptionist', () => {
    renderLayout([USER_ROLES.receptionist]);

    expect(screen.queryByText('Giám sát thanh toán')).toBeNull();
    expect(screen.queryByText('Tổng quan')).toBeNull();
  });

  it('shows maintenance management only to manager', () => {
    const { unmount } = renderLayout([USER_ROLES.manager]);
    expect(screen.getByText('Quản lý bảo trì')).not.toBeNull();
    unmount();

    for (const role of [USER_ROLES.admin, USER_ROLES.receptionist, USER_ROLES.housekeeper]) {
      const view = renderLayout([role]);
      expect(screen.queryByText('Quản lý bảo trì')).toBeNull();
      view.unmount();
    }
  });

  it.each([USER_ROLES.receptionist, USER_ROLES.manager, USER_ROLES.admin])(
    'shows today arrivals to an allowed %s',
    (role) => {
      renderLayout([role]);

      expect(screen.getByText('Khách đến hôm nay')).not.toBeNull();
    },
  );

  it('hides today arrivals from housekeeper', () => {
    renderLayout([USER_ROLES.housekeeper]);

    expect(screen.queryByText('Khách đến hôm nay')).toBeNull();
  });

  it('shows dirty rooms only to housekeeper', () => {
    renderLayout([USER_ROLES.housekeeper]);

    expect(screen.getByText('Phòng cần dọn')).not.toBeNull();
  });

  it('hides dirty rooms from manager', () => {
    renderLayout([USER_ROLES.manager]);

    expect(screen.queryByText('Phòng cần dọn')).toBeNull();
  });

  it('shows check-in only to receptionist', () => {
    renderLayout([USER_ROLES.receptionist]);

    expect(screen.getByText('Nhận phòng')).not.toBeNull();
  });

  it('shows today departures to a receptionist', () => {
    renderLayout([USER_ROLES.receptionist]);

    expect(screen.getByText('Khách trả phòng')).not.toBeNull();
  });

  it('hides today departures from a housekeeper', () => {
    renderLayout([USER_ROLES.housekeeper]);

    expect(screen.queryByText('Khách trả phòng')).toBeNull();
  });

  it('shows add-on management to manager and admin roles', () => {
    const { unmount } = renderLayout([USER_ROLES.manager]);
    expect(screen.getByText('Dịch vụ bổ sung')).not.toBeNull();
    unmount();

    renderLayout([USER_ROLES.admin]);
    expect(screen.getByText('Dịch vụ bổ sung')).not.toBeNull();
  });

  it('hides add-on management from operational staff', () => {
    renderLayout([USER_ROLES.receptionist]);
    expect(screen.queryByText('Dịch vụ bổ sung')).toBeNull();
  });

  it('shows price-rule management only to manager and admin roles', () => {
    const managerView = renderLayout([USER_ROLES.manager]);
    expect(screen.getByText('Quy tắc giá')).not.toBeNull();
    managerView.unmount();

    const adminView = renderLayout([USER_ROLES.admin]);
    expect(screen.getByText('Quy tắc giá')).not.toBeNull();
    adminView.unmount();

    for (const role of [USER_ROLES.receptionist, USER_ROLES.housekeeper]) {
      const operationalView = renderLayout([role]);
      expect(screen.queryByText('Quy tắc giá')).toBeNull();
      operationalView.unmount();
    }
  });
});

describe('account administration menu', () => {
  it('shows account management to admin', () => {
    renderLayout([USER_ROLES.admin]);
    expect(screen.getByText('Tài khoản và phân quyền')).toBeTruthy();
  });
  it.each([USER_ROLES.manager, USER_ROLES.receptionist, USER_ROLES.housekeeper])(
    'hides account management from %s',
    (role) => {
      renderLayout([role]);
      expect(screen.queryByText('Tài khoản và phân quyền')).toBeNull();
    },
  );
});
