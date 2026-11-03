// @vitest-environment jsdom
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { App } from 'antd';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import { useAuth } from '../providers/auth-context';
import { login } from '../services/auth-service';
import { LoginPage } from './login-page';
vi.mock('../providers/auth-context', () => ({ useAuth: vi.fn() }));
vi.mock('../services/auth-service', () => ({ login: vi.fn() }));
const startSession = vi.fn();
beforeEach(() => {
  vi.clearAllMocks();
  globalThis.ResizeObserver = class {
    observe() {}
    unobserve() {}
    disconnect() {}
  };
  window.matchMedia = vi
    .fn()
    .mockImplementation(() => ({ matches: false, addListener: vi.fn(), removeListener: vi.fn() }));
  vi.mocked(useAuth).mockReturnValue({
    isAuthenticated: false,
    isInitializing: false,
    roles: [],
    startSession,
  } as never);
});
afterEach(cleanup);
function view() {
  return (
    <QueryClientProvider client={new QueryClient()}>
      <App>
        <MemoryRouter
          initialEntries={[
            { pathname: '/login', state: { from: { pathname: '/account/profile/' } } },
          ]}
        >
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/account/profile" element={<div>Trang hồ sơ</div>} />
          </Routes>
        </MemoryRouter>
      </App>
    </QueryClientProvider>
  );
}
it('redirects a restored session to its allowed deep link', async () => {
  vi.mocked(useAuth).mockReturnValue({
    isAuthenticated: true,
    isInitializing: false,
    roles: ['MANAGER'],
    startSession,
  } as never);
  render(view());
  expect(await screen.findByText('Trang hồ sơ')).toBeTruthy();
  expect(login).not.toHaveBeenCalled();
});
it('waits for session initialization before allowing login', () => {
  vi.mocked(useAuth).mockReturnValue({
    isAuthenticated: false,
    isInitializing: true,
    roles: [],
    startSession,
  } as never);
  render(view());
  expect(screen.getByRole('button', { name: /ĐĂNG NHẬP/ }).hasAttribute('disabled')).toBe(true);
});
it('allows retry after failure and navigates after success', async () => {
  vi.mocked(login)
    .mockRejectedValueOnce(new Error('offline'))
    .mockResolvedValueOnce({
      accessToken: 'token',
      refreshToken: 'refresh',
      expiresIn: 3600,
      userRoles: ['MANAGER'],
      user: { fullName: 'Nguyễn An' },
    } as never);
  render(view());
  fireEvent.change(screen.getByLabelText('Email hoặc số điện thoại'), {
    target: { value: 'user@example.com' },
  });
  fireEvent.change(screen.getByLabelText('Mật khẩu'), { target: { value: 'password123' } });
  fireEvent.click(screen.getByRole('button', { name: /ĐĂNG NHẬP/ }));
  expect(await screen.findByText('Không thể đăng nhập. Vui lòng thử lại.')).toBeTruthy();
  fireEvent.click(screen.getByRole('button', { name: /ĐĂNG NHẬP/ }));
  await waitFor(() => expect(startSession).toHaveBeenCalledTimes(1));
  expect(await screen.findByText('Trang hồ sơ')).toBeTruthy();
});
