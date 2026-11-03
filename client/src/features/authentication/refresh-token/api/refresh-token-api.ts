import axios from 'axios';

import { env } from '@config/env';

import type {
  ApiResponse,
  RefreshTokenRequest,
  RefreshTokenResponse,
} from '../types/refresh-token.types';

const refreshClient = axios.create({
  baseURL: env.apiBaseUrl,
  timeout: 10_000,
  headers: {
    'Content-Type': 'application/json',
  },
});

export async function refreshAccessToken(refreshToken: string): Promise<RefreshTokenResponse> {
  const payload: RefreshTokenRequest = { refreshToken };
  const response = await refreshClient.post<ApiResponse<RefreshTokenResponse>>(
    '/auth/refresh-token',
    payload,
  );

  return response.data.data;
}
