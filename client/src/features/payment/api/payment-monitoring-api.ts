import { apiClient } from '../../../lib/axios';
import type { ApiResponse } from '../../../types/api';
import type {
  MonitoredPayment,
  MonitoredWebhookEvent,
  MonitoringPage,
  MonitoringParams,
  PaymentMonitoringSummary,
  WebhookEventDetail,
} from '../types/payment-monitoring';

const normalizedParams = (params: MonitoringParams) => ({
  ...params,
  keyword: params.keyword?.trim() || undefined,
  status: params.status || undefined,
});

export async function getPaymentMonitoringSummary(): Promise<PaymentMonitoringSummary> {
  const response = await apiClient.get<ApiResponse<PaymentMonitoringSummary>>(
    '/payment-monitoring/summary',
  );
  return response.data.data;
}

export async function getMonitoredPayments(
  params: MonitoringParams,
): Promise<MonitoringPage<MonitoredPayment>> {
  const response = await apiClient.get<ApiResponse<MonitoringPage<MonitoredPayment>>>(
    '/payment-monitoring/payments',
    { params: normalizedParams(params) },
  );
  return response.data.data;
}

export async function getMonitoredWebhookEvents(
  params: MonitoringParams,
): Promise<MonitoringPage<MonitoredWebhookEvent>> {
  const response = await apiClient.get<ApiResponse<MonitoringPage<MonitoredWebhookEvent>>>(
    '/payment-monitoring/webhook-events',
    { params: normalizedParams(params) },
  );
  return response.data.data;
}

export async function getWebhookEventDetail(eventId: string): Promise<WebhookEventDetail> {
  const response = await apiClient.get<ApiResponse<WebhookEventDetail>>(
    `/payment-monitoring/webhook-events/${eventId}`,
  );
  return response.data.data;
}
