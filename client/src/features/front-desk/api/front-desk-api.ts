import { apiClient } from '../../../lib/axios';
import type { ApiResponse } from '../../../types/api';
import type { TodayArrivalsPage, TodayArrivalsParams } from '../types/today-arrivals';

export async function getTodayArrivals(params: TodayArrivalsParams): Promise<TodayArrivalsPage> {
  const response = await apiClient.get<ApiResponse<TodayArrivalsPage>>(
    '/front-desk/arrivals/today',
    { params },
  );
  return response.data.data;
}
