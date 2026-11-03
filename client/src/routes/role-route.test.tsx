// @vitest-environment jsdom

import { cleanup, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';

import { useAuth } from '../features/authentication/providers/auth-context';
import { USER_ROLES, type UserRole } from '../types/common';
import { ProtectedRoute } from './protected-route';
import { RoleRoute } from './role-route';

vi.mock('../features/authentication/providers/auth-context', () => ({ useAuth: vi.fn() }));

afterEach(cleanup);

function CurrentPath() {
  return <div data-testid="current-path">{useLocation().pathname}</div>;
}

function renderRestrictedRoute(roles: UserRole[], isAuthenticated = true) {
  vi.mocked(useAuth).mockReturnValue({ roles, isAuthenticated, isInitializing: false } as never);

  return render(
    <MemoryRouter initialEntries={['/payment-monitoring/payments']}>
      <Routes>
        <Route path="/login" element={<div>Trang đăng nhập</div>} />
        <Route element={<ProtectedRoute />}>
          <Route
            path="/forbidden"
            element={
              <>
                <div>Không có quyền</div>
                <CurrentPath />
              </>
            }
          />
          <Route
            element={
              <RoleRoute allowedRoles={[USER_ROLES.admin, USER_ROLES.manager]}>
                <div>Giám sát thanh toán</div>
              </RoleRoute>
            }
          >
            <Route path="/payment-monitoring/payments" element={<div>Giám sát thanh toán</div>} />
          </Route>
        </Route>
      </Routes>
    </MemoryRouter>,
  );
}

describe('RoleRoute', () => {
  it('renders a restricted route for an allowed role', () => {
    renderRestrictedRoute([USER_ROLES.admin]);

    expect(screen.getByText('Giám sát thanh toán')).not.toBeNull();
  });

  it('redirects a denied role when opening the URL directly', () => {
    renderRestrictedRoute([]);

    expect(screen.getByText('Không có quyền')).not.toBeNull();
    expect(screen.getByTestId('current-path').textContent).toBe('/forbidden');
    expect(screen.queryByText('Giám sát thanh toán')).toBeNull();
  });

  it('allows a multi-role user when at least one role is allowed', () => {
    renderRestrictedRoute([USER_ROLES.housekeeper, USER_ROLES.manager]);

    expect(screen.getByText('Giám sát thanh toán')).not.toBeNull();
  });

  it('keeps anonymous users in the authentication flow', () => {
    renderRestrictedRoute([], false);

    expect(screen.getByText('Trang đăng nhập')).not.toBeNull();
    expect(screen.queryByText('Không có quyền')).toBeNull();
  });
});
