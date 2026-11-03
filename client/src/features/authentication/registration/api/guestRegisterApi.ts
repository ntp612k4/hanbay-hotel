import { apiClient } from '@services/apiClient';

import type { GuestRegisterRequest, GuestRegisterResponse } from '../types/guestRegister.types';

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

export async function registerGuest(payload: GuestRegisterRequest): Promise<GuestRegisterResponse> {
  const response = await apiClient.post<ApiResponse<GuestRegisterResponse>>(
    '/auth/register',
    payload,
  );

  return response.data.data;
}
