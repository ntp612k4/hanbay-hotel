const PAYMENT_RESULT_TOKEN_PREFIX = 'hanbay:payment-result-token:';
const LATEST_PAYMENT_RESULT_ID = 'hanbay:latest-pending-payment-id';

export function storePaymentResultToken(paymentId: string, bookingAccessToken: string) {
  if (typeof window === 'undefined') return;
  window.sessionStorage.setItem(`${PAYMENT_RESULT_TOKEN_PREFIX}${paymentId}`, bookingAccessToken);
  window.sessionStorage.setItem(LATEST_PAYMENT_RESULT_ID, paymentId);
}

export function getLatestPaymentResultId() {
  if (typeof window === 'undefined') return null;
  return window.sessionStorage.getItem(LATEST_PAYMENT_RESULT_ID);
}

export function clearLatestPaymentResult(paymentId: string) {
  if (typeof window === 'undefined') return;
  if (window.sessionStorage.getItem(LATEST_PAYMENT_RESULT_ID) === paymentId) {
    window.sessionStorage.removeItem(LATEST_PAYMENT_RESULT_ID);
  }
}

export function getPaymentResultToken(paymentId: string) {
  if (typeof window === 'undefined') return null;
  return window.sessionStorage.getItem(`${PAYMENT_RESULT_TOKEN_PREFIX}${paymentId}`);
}
