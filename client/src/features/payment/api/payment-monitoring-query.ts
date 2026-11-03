import { keepPreviousData, useQuery } from '@tanstack/react-query';

import type { MonitoringParams } from '../types/payment-monitoring';
import {
  getMonitoredPayments,
  getMonitoredWebhookEvents,
  getPaymentMonitoringSummary,
  getWebhookEventDetail,
} from './payment-monitoring-api';

export const paymentMonitoringKeys = {
  all: ['payment-monitoring'] as const,
  summary: () => [...paymentMonitoringKeys.all, 'summary'] as const,
  payments: (params: MonitoringParams) =>
    [...paymentMonitoringKeys.all, 'payments', params] as const,
  webhookEvents: (params: MonitoringParams) =>
    [...paymentMonitoringKeys.all, 'webhook-events', params] as const,
  webhookDetail: (eventId?: string) =>
    [...paymentMonitoringKeys.all, 'webhook-events', eventId] as const,
};

export const usePaymentMonitoringSummary = () =>
  useQuery({
    queryKey: paymentMonitoringKeys.summary(),
    queryFn: getPaymentMonitoringSummary,
  });

export const useMonitoredPayments = (params: MonitoringParams, enabled: boolean) =>
  useQuery({
    queryKey: paymentMonitoringKeys.payments(params),
    queryFn: () => getMonitoredPayments(params),
    placeholderData: keepPreviousData,
    enabled,
  });

export const useMonitoredWebhookEvents = (params: MonitoringParams, enabled: boolean) =>
  useQuery({
    queryKey: paymentMonitoringKeys.webhookEvents(params),
    queryFn: () => getMonitoredWebhookEvents(params),
    placeholderData: keepPreviousData,
    enabled,
  });

export const useWebhookEventDetail = (eventId: string | undefined, enabled: boolean) =>
  useQuery({
    queryKey: paymentMonitoringKeys.webhookDetail(eventId),
    queryFn: () => getWebhookEventDetail(eventId!),
    enabled: enabled && Boolean(eventId),
  });
