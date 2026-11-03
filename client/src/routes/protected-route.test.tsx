// @vitest-environment jsdom

import { cleanup, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { useAuth } from '../features/authentication/providers/auth-context';
import { ProtectedRoute } from './protected-route';

vi.mock('../features/authentication/providers/auth-context', () => ({ useAuth: vi.fn() }));

afterEach(cleanup);

function renderProtectedRoute() {
  return render(
    <MemoryRouter initialEntries={['/account/profile']}>
      <Routes>
        <Route path="/login" element={<div>Trang đăng nhập</div>} />
        <Route element={<ProtectedRoute />}>
          <Route path="/account/profile" element={<div>Trang hồ sơ</div>} />
        </Route>
      </Routes>
    </MemoryRouter>,
  );
}

describe('ProtectedRoute', () => {
  it('redirects an anonymous user to login', () => {
    vi.mocked(useAuth).mockReturnValue({
      isAuthenticated: false,
      isInitializing: false,
    } as never);

    renderProtectedRoute();

    expect(screen.getByText('Trang đăng nhập')).not.toBeNull();
  });

  it('renders profile for an authenticated guest', () => {
    vi.mocked(useAuth).mockReturnValue({
      isAuthenticated: true,
      isInitializing: false,
    } as never);

    renderProtectedRoute();

    expect(screen.getByText('Trang hồ sơ')).not.toBeNull();
  });
});
