// @vitest-environment jsdom

import type { AxiosAdapter, AxiosResponse, InternalAxiosRequestConfig } from 'axios';
import { AxiosError, AxiosHeaders } from 'axios';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { refreshAccessToken } from '@features/authentication/refresh-token';

import { API_CLIENT_EVENTS, apiClient } from './axios';
import { tokenStorage } from './storage';

vi.mock('@features/authentication/refresh-token', () => ({
  refreshAccessToken: vi.fn(),
}));

vi.mock('@config/env', () => ({
  env: { apiBaseUrl: 'http://localhost:8080/api/v1' },
}));

const mockedRefreshAccessToken = vi.mocked(refreshAccessToken);

const unauthorizedError = (config: InternalAxiosRequestConfig): AxiosError =>
  new AxiosError('Unauthorized', 'ERR_BAD_REQUEST', config, undefined, {
    data: { code: 'AUTH_002', message: 'Token expired' },
    status: 401,
    statusText: 'Unauthorized',
    headers: new AxiosHeaders(),
    config,
  });

const forbiddenError = (config: InternalAxiosRequestConfig): AxiosError =>
  new AxiosError('Forbidden', 'ERR_BAD_REQUEST', config, undefined, {
    data: { code: 'AUTH_FORBIDDEN', message: 'Bạn không có quyền truy cập' },
    status: 403,
    statusText: 'Forbidden',
    headers: new AxiosHeaders(),
    config,
  });

const successResponse = (config: InternalAxiosRequestConfig): AxiosResponse => ({
  data: { ok: true },
  status: 200,
  statusText: 'OK',
  headers: new AxiosHeaders(),
  config,
});

describe('apiClient refresh interceptor', () => {
  beforeEach(() => {
    window.localStorage.clear();
    mockedRefreshAccessToken.mockReset();
  });

  it('uses one refresh request for concurrent unauthorized responses', async () => {
    tokenStorage.setTokens('expired-access-token', 'current-refresh-token');
    mockedRefreshAccessToken.mockResolvedValue({
      accessToken: 'new-access-token',
      refreshToken: 'new-refresh-token',
      tokenType: 'Bearer',
      expiresIn: 900,
    });

    const adapter: AxiosAdapter = async (config) => {
      const retryableConfig = config as InternalAxiosRequestConfig & { _retry?: boolean };
      if (!retryableConfig._retry) throw unauthorizedError(config);
      return successResponse(config);
    };

    const [firstResponse, secondResponse] = await Promise.all([
      apiClient.get('/bookings', { adapter }),
      apiClient.get('/profile', { adapter }),
    ]);

    expect(firstResponse.status).toBe(200);
    expect(secondResponse.status).toBe(200);
    expect(mockedRefreshAccessToken).toHaveBeenCalledTimes(1);
    expect(mockedRefreshAccessToken).toHaveBeenCalledWith('current-refresh-token');
    expect(tokenStorage.getAccessToken()).toBe('new-access-token');
    expect(tokenStorage.getRefreshToken()).toBe('new-refresh-token');
  });

  it('clears tokens and expires the session when refresh fails', async () => {
    tokenStorage.setTokens('expired-access-token', 'invalid-refresh-token');
    const refreshConfig = {
      headers: new AxiosHeaders(),
      method: 'post',
      url: '/auth/refresh-token',
    } as InternalAxiosRequestConfig;
    mockedRefreshAccessToken.mockRejectedValue(unauthorizedError(refreshConfig));
    const authExpiredListener = vi.fn();
    window.addEventListener(API_CLIENT_EVENTS.authExpired, authExpiredListener);

    const adapter: AxiosAdapter = async (config) => {
      throw unauthorizedError(config);
    };

    await expect(apiClient.get('/bookings', { adapter })).rejects.toMatchObject({ status: 401 });

    expect(mockedRefreshAccessToken).toHaveBeenCalledTimes(1);
    expect(tokenStorage.getAccessToken()).toBeNull();
    expect(tokenStorage.getRefreshToken()).toBeNull();
    expect(authExpiredListener).toHaveBeenCalledTimes(1);
    window.removeEventListener(API_CLIENT_EVENTS.authExpired, authExpiredListener);
  });

  it('preserves the session and does not refresh on forbidden responses', async () => {
    tokenStorage.setTokens('valid-access-token', 'valid-refresh-token');
    const authExpiredListener = vi.fn();
    window.addEventListener(API_CLIENT_EVENTS.authExpired, authExpiredListener);

    const adapter: AxiosAdapter = async (config) => {
      throw forbiddenError(config);
    };

    await expect(apiClient.get('/payment-monitoring/summary', { adapter })).rejects.toMatchObject({
      status: 403,
      code: 'AUTH_FORBIDDEN',
    });

    expect(mockedRefreshAccessToken).not.toHaveBeenCalled();
    expect(tokenStorage.getAccessToken()).toBe('valid-access-token');
    expect(tokenStorage.getRefreshToken()).toBe('valid-refresh-token');
    expect(authExpiredListener).not.toHaveBeenCalled();
    window.removeEventListener(API_CLIENT_EVENTS.authExpired, authExpiredListener);
  });
});
