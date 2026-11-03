import { ApiClientError } from '../../../types/api';
import type { BookingPaymentStatus, PaymentRefundStatus } from '../types/booking';

const BASE_INTERVAL_MS = 2_000;
const MAX_INTERVAL_MS = 15_000;

interface PaymentPollingState {
  paymentStatus?: BookingPaymentStatus;
  refundStatus?: PaymentRefundStatus | null;
  error: unknown;
  failureCount: number;
}

export function getPaymentPollingInterval({
  paymentStatus,
  refundStatus,
  error,
  failureCount,
}: PaymentPollingState): number | false {
  const refundIsActive = refundStatus === 'PENDING' || refundStatus === 'PROCESSING';
  if (paymentStatus && paymentStatus !== 'PENDING' && !refundIsActive) return false;
  if (error instanceof ApiClientError && error.kind === 'api' && (error.status ?? 0) < 500) {
    return false;
  }
  return Math.min(BASE_INTERVAL_MS * 2 ** failureCount, MAX_INTERVAL_MS);
}
