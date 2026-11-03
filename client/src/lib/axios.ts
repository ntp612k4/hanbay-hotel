import { parseRetryAfter } from './retry-after';
import axios, { AxiosError, AxiosHeaders, type InternalAxiosRequestConfig } from 'axios';

import { env } from '@config/env';
import { refreshAccessToken } from '@features/authentication/refresh-token';

import { ApiClientError, type ApiErrorResponse } from '../types/api';
import { authStorage, tokenStorage } from './storage';

export const API_CLIENT_EVENTS = {
  authExpired: 'api:auth-expired',
} as const;

const createApiClient = () =>
  axios.create({
    baseURL: env.apiBaseUrl,
    timeout: 10_000,
    headers: {
      'Content-Type': 'application/json',
    },
  });

export const publicApiClient = createApiClient();
export const privateApiClient = createApiClient();

privateApiClient.interceptors.request.use((config) => {
  const accessToken = tokenStorage.getAccessToken();

  if (accessToken) {
    config.headers = AxiosHeaders.from(config.headers);
    config.headers.set('Authorization', `Bearer ${accessToken}`);
  }

  return config;
});

type RetryableRequestConfig = InternalAxiosRequestConfig & { _retry?: boolean };

let refreshTokenRequest: Promise<string> | null = null;

const dispatchApiEvent = (eventName: string): void => {
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent(eventName));
  }
};

const toApiClientError = (error: unknown): ApiClientError => {
  if (!axios.isAxiosError<ApiErrorResponse>(error)) {
    return new ApiClientError({
      message: 'Đã xảy ra lỗi không xác định. Vui lòng thử lại.',
      kind: 'unknown',
      cause: error,
    });
  }

  const axiosError: AxiosError<ApiErrorResponse> = error;

  if (!axiosError.response) {
    return new ApiClientError({
      message: 'Không thể kết nối đến máy chủ. Vui lòng thử lại.',
      kind: 'network',
      code: axiosError.code,
      cause: error,
    });
  }

  const { data, status } = axiosError.response;
  const retryAfterHeader = axiosError.response.headers['retry-after'];
  const retryAfterSeconds = parseRetryAfter(retryAfterHeader);

  return new ApiClientError({
    message: data?.message ?? 'Yêu cầu không thể hoàn thành.',
    kind: 'api',
    status,
    code: data?.code,
    data,
    retryAfterSeconds,
    cause: error,
  });
};

publicApiClient.interceptors.response.use(
  (response) => response,
  (error: unknown) => Promise.reject(toApiClientError(error)),
);

privateApiClient.interceptors.response.use(
  (response) => response,
  async (error: unknown) => {
    const originalRequest = axios.isAxiosError(error)
      ? (error.config as RetryableRequestConfig | undefined)
      : undefined;
    const apiError = toApiClientError(error);

    if (apiError.status === 401) {
      const refreshToken = tokenStorage.getRefreshToken();

      if (refreshToken && originalRequest && !originalRequest._retry) {
        originalRequest._retry = true;

        try {
          refreshTokenRequest ??= refreshAccessToken(refreshToken)
            .then((tokens) => {
              authStorage.updateTokens(tokens.accessToken, tokens.refreshToken, tokens.expiresIn);
              return tokens.accessToken;
            })
            .finally(() => {
              refreshTokenRequest = null;
            });

          const accessToken = await refreshTokenRequest;
          originalRequest.headers = AxiosHeaders.from(originalRequest.headers);
          originalRequest.headers.set('Authorization', `Bearer ${accessToken}`);

          return apiClient(originalRequest);
        } catch (refreshError) {
          authStorage.clearSession();
          dispatchApiEvent(API_CLIENT_EVENTS.authExpired);
          return Promise.reject(toApiClientError(refreshError));
        }
      }

      dispatchApiEvent(API_CLIENT_EVENTS.authExpired);
    }

    return Promise.reject(apiError);
  },
);

export const apiClient = privateApiClient;
