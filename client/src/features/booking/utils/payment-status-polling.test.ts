import { describe, expect, it } from 'vitest';

import { ApiClientError } from '../../../types/api';
import { getPaymentPollingInterval } from './payment-status-polling';

describe('getPaymentPollingInterval', () => {
  it('tiếp tục polling với backoff khi lỗi mạng xảy ra trước response đầu tiên', () => {
    const interval = getPaymentPollingInterval({
      error: new ApiClientError({ message: 'Mất kết nối', kind: 'network' }),
      failureCount: 2,
    });

    expect(interval).toBe(8_000);
  });

  it('dừng polling với lỗi 4xx không thể tự phục hồi', () => {
    const interval = getPaymentPollingInterval({
      error: new ApiClientError({ message: 'Không tìm thấy', kind: 'api', status: 404 }),
      failureCount: 1,
    });

    expect(interval).toBe(false);
  });

  it('dừng polling khi payment đã kết thúc', () => {
    expect(
      getPaymentPollingInterval({ paymentStatus: 'SUCCESS', error: null, failureCount: 0 }),
    ).toBe(false);
  });

  it('tiếp tục polling khi refund vẫn đang được xử lý', () => {
    expect(
      getPaymentPollingInterval({
        paymentStatus: 'SUCCESS',
        refundStatus: 'PROCESSING',
        error: null,
        failureCount: 0,
      }),
    ).toBe(2_000);
  });
});
