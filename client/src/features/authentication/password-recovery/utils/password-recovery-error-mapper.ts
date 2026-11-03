import { ApiClientError } from '../../../../types/api';

const ERROR_MESSAGES: Record<string, string> = {
  AUTH_RESET_TOKEN_INVALID: 'Liên kết đặt lại mật khẩu không hợp lệ.',
  AUTH_RESET_TOKEN_EXPIRED: 'Liên kết đặt lại mật khẩu đã hết hạn.',
  AUTH_RESET_TOKEN_USED: 'Liên kết đặt lại mật khẩu đã được sử dụng hoặc bị thu hồi.',
  PASSWORD_MISMATCH: 'Mật khẩu xác nhận không khớp.',
  VALIDATION_FAILED: 'Vui lòng kiểm tra lại thông tin đã nhập.',
};

export function mapPasswordRecoveryError(
  error: unknown,
  fallback = 'Không thể hoàn thành yêu cầu. Vui lòng thử lại.',
): string {
  if (error instanceof ApiClientError) {
    return (error.code && ERROR_MESSAGES[error.code]) || error.message || fallback;
  }

  return fallback;
}
