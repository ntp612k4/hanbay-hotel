import { apiClient } from '../../../lib/axios';
import type { ApiResponse } from '../../../types/api';
import type { DashboardReport, DashboardReportParams } from '../types/dashboard-report';

export async function getDashboardReport(params: DashboardReportParams): Promise<DashboardReport> {
  const response = await apiClient.get<ApiResponse<DashboardReport>>('/reports/dashboard', {
    params: { ...params, currency: params.currency ?? 'VND' },
  });
  return response.data.data;
}
