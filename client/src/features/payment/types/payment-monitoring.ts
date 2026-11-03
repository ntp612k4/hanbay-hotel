export type PaymentStatus =
  'PENDING' | 'SUCCESS' | 'FAILED' | 'CANCELLED' | 'REFUNDED' | 'EXPIRED' | 'PARTIALLY_REFUNDED';

export type WebhookStatus = 'RECEIVED' | 'PROCESSING' | 'PROCESSED' | 'FAILED' | 'IGNORED';

export interface MonitoringPage<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface MonitoringParams {
  page: number;
  size: number;
  keyword?: string;
  status?: string;
}

export interface PaymentMonitoringSummary {
  totalPayments: number;
  successfulPayments: number;
  totalWebhookEvents: number;
  failedWebhookEvents: number;
}

export interface MonitoredPayment {
  id: string;
  bookingCode: string;
  paymentCode: string;
  amount: number;
  currency: string;
  status: PaymentStatus;
  createdAt: string;
  paidAt?: string | null;
}

export interface MonitoredWebhookEvent {
  id: string;
  eventId: string;
  gateway?: string | null;
  transferAmount: number;
  transactionReference?: string | null;
  paymentCode?: string | null;
  status: WebhookStatus;
  attempts: number;
  receivedAt: string;
  lastError?: string | null;
}

export interface WebhookEventDetail {
  id: string;
  eventId: string;
  eventType?: string | null;
  paymentCode?: string | null;
  deduplicationKey: string;
  deduplicationStrategy: 'EVENT_ID' | 'TRANSACTION_ID' | 'BUSINESS_HASH';
  signatureValid: boolean;
  status: WebhookStatus;
  attempts: number;
  processingStartedAt?: string | null;
  processedAt?: string | null;
  lastError?: string | null;
  providerResponseStatusCode?: number | null;
  receivedAt: string;
  safeHeaders?: Record<string, unknown> | null;
  payload: Record<string, unknown>;
}
