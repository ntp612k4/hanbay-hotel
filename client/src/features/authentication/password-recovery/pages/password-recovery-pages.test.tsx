// @vitest-environment jsdom

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { App, ConfigProvider } from 'antd';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';

import { ApiClientError } from '../../../../types/api';
import { forgotPassword, resetPassword, verifyResetToken } from '../api/password-recovery-api';
import { ForgotPasswordPage } from './forgot-password-page';
import { ResetPasswordPage } from './reset-password-page';

vi.mock('../api/password-recovery-api', () => ({
  forgotPassword: vi.fn(),
  verifyResetToken: vi.fn(),
  resetPassword: vi.fn(),
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
  vi.clearAllMocks();
});

afterEach(() => {
  cleanup();
});

function renderPage(page: React.ReactNode, initialEntry: string) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <ConfigProvider>
        <App>
          <MemoryRouter initialEntries={[initialEntry]}>
            <Routes>
              <Route path="*" element={page} />
              <Route path="/login" element={<div>Login destination</div>} />
            </Routes>
          </MemoryRouter>
        </App>
      </ConfigProvider>
    </QueryClientProvider>,
  );
}

function apiError(code: string, message: string) {
  return new ApiClientError({ kind: 'api', status: 400, code, message });
}

async function fillResetForm(confirmPassword = 'NewPassword123') {
  fireEvent.change(screen.getByLabelText('Mật khẩu mới'), {
    target: { value: 'NewPassword123' },
  });
  fireEvent.change(screen.getByLabelText('Xác nhận mật khẩu'), {
    target: { value: confirmPassword },
  });
  fireEvent.click(screen.getByRole('button', { name: 'Đặt lại mật khẩu' }));
}

describe('ForgotPasswordPage', () => {
  it('shows field error for invalid email', async () => {
    renderPage(<ForgotPasswordPage />, '/forgot-password');

    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'invalid-email' } });
    fireEvent.click(screen.getByRole('button', { name: /Gửi hướng dẫn/ }));

    expect(await screen.findByText('Email không hợp lệ.')).toBeTruthy();
    expect(forgotPassword).not.toHaveBeenCalled();
  });

  it('shows the generic success message after submitting', async () => {
    vi.mocked(forgotPassword).mockResolvedValue({ status: 'RESET_INSTRUCTIONS_ACCEPTED' });
    renderPage(<ForgotPasswordPage />, '/forgot-password');

    fireEvent.change(screen.getByLabelText('Email'), { target: { value: ' Guest@Example.COM ' } });
    fireEvent.click(screen.getByRole('button', { name: /Gửi hướng dẫn/ }));

    expect(
      await screen.findAllByText('Nếu email tồn tại, hướng dẫn đặt lại mật khẩu sẽ được gửi.'),
    ).not.toHaveLength(0);
    expect(vi.mocked(forgotPassword).mock.calls[0]?.[0]).toEqual({
      email: 'guest@example.com',
    });
  });

  it('shows a friendly message when the API fails', async () => {
    vi.mocked(forgotPassword).mockRejectedValue(
      apiError('INTERNAL_SERVER_ERROR', 'Dịch vụ tạm thời không khả dụng.'),
    );
    renderPage(<ForgotPasswordPage />, '/forgot-password');

    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'guest@example.com' } });
    fireEvent.click(screen.getByRole('button', { name: /Gửi hướng dẫn/ }));

    expect(await screen.findByText('Dịch vụ tạm thời không khả dụng.')).toBeTruthy();
  });
});

describe('ResetPasswordPage', () => {
  it('shows the form when token verification succeeds', async () => {
    vi.mocked(verifyResetToken).mockResolvedValue({
      status: 'RESET_TOKEN_VALID',
      expiresAt: '2026-08-10T06:15:00Z',
    });
    renderPage(<ResetPasswordPage />, '/reset-password?token=valid-token');

    expect(await screen.findByLabelText('Mật khẩu mới')).toBeTruthy();
    expect(verifyResetToken).toHaveBeenCalledWith('valid-token');
  });

  it.each([
    ['AUTH_RESET_TOKEN_INVALID', 'Liên kết đặt lại mật khẩu không hợp lệ.'],
    ['AUTH_RESET_TOKEN_EXPIRED', 'Liên kết đặt lại mật khẩu đã hết hạn.'],
  ])('shows the correct state for %s', async (code, expectedMessage) => {
    vi.mocked(verifyResetToken).mockRejectedValue(apiError(code, 'Backend error'));
    renderPage(<ResetPasswordPage />, '/reset-password?token=invalid-token');

    expect(await screen.findByText(expectedMessage)).toBeTruthy();
    expect(screen.queryByLabelText('Mật khẩu mới')).toBeNull();
  });

  it('blocks submission when password confirmation does not match', async () => {
    vi.mocked(verifyResetToken).mockResolvedValue({
      status: 'RESET_TOKEN_VALID',
      expiresAt: '2026-08-10T06:15:00Z',
    });
    renderPage(<ResetPasswordPage />, '/reset-password?token=valid-token');
    await screen.findByLabelText('Mật khẩu mới');

    await fillResetForm('OtherPassword123');

    expect(await screen.findByText('Mật khẩu xác nhận không khớp.')).toBeTruthy();
    expect(resetPassword).not.toHaveBeenCalled();
  });

  it('resets the password and redirects to login', async () => {
    vi.mocked(verifyResetToken).mockResolvedValue({
      status: 'RESET_TOKEN_VALID',
      expiresAt: '2026-08-10T06:15:00Z',
    });
    vi.mocked(resetPassword).mockResolvedValue({ status: 'PASSWORD_RESET' });
    renderPage(<ResetPasswordPage />, '/reset-password?token=valid-token');
    await screen.findByLabelText('Mật khẩu mới');

    await fillResetForm();

    await waitFor(() =>
      expect(vi.mocked(resetPassword).mock.calls[0]?.[0]).toEqual({
        token: 'valid-token',
        newPassword: 'NewPassword123',
        confirmPassword: 'NewPassword123',
      }),
    );
    expect(await screen.findByText('Login destination')).toBeTruthy();
  });

  it('shows a friendly reset API error', async () => {
    vi.mocked(verifyResetToken).mockResolvedValue({
      status: 'RESET_TOKEN_VALID',
      expiresAt: '2026-08-10T06:15:00Z',
    });
    vi.mocked(resetPassword).mockRejectedValue(apiError('AUTH_RESET_TOKEN_USED', 'Backend error'));
    renderPage(<ResetPasswordPage />, '/reset-password?token=valid-token');
    await screen.findByLabelText('Mật khẩu mới');

    await fillResetForm();

    expect(
      await screen.findByText('Liên kết đặt lại mật khẩu đã được sử dụng hoặc bị thu hồi.'),
    ).toBeTruthy();
  });
});
