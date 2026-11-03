import type { PageResponse } from '../../../types/api';

export interface RefundRequest {
  refundId: string;
  bookingCode: string;
  paymentCode: string;
  amount: number;
  currency: 'VND';
  reason: string;
  status: 'PENDING' | 'PROCESSING' | 'SUCCESS' | 'FAILED' | 'CANCELLED';
  bankReference: string | null;
  bankName?: string | null;
  accountHolderName?: string | null;
  accountNumber?: string | null;
  requestedAt: string;
  refundedAt: string | null;
}

export type RefundRequestPage = PageResponse<RefundRequest>;
export interface RefundInformation {
  method: 'BANK_TRANSFER';
  bankName: string;
  accountHolderName: string;
  accountNumber: string;
}
