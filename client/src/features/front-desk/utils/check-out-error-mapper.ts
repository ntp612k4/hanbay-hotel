import { ApiClientError } from '../../../types/api';

const CHECK_OUT_ERRORS: Record<string, string> = {
  PAYMENT_REQUIRED:
    'Đặt phòng còn công nợ. Hãy kiểm tra trạng thái thanh toán hoặc liên hệ quản lý trước khi thử lại.',
  CHECKOUT_OVER_COLLECTION: 'Số tiền thu tại quầy vượt số tiền cần thu. Vui lòng kiểm tra lại.',
  REFUND_INFORMATION_REQUIRED: 'Cần nhập đầy đủ thông tin tài khoản nhận hoàn tiền.',
  INSUFFICIENT_PAID_AMOUNT: 'Không tìm thấy khoản thanh toán hợp lệ để tạo yêu cầu hoàn tiền.',
  INVALID_EARLY_CHECKOUT_DATE: 'Ngày trả phòng sớm phải nằm trong thời gian lưu trú.',
  BOOKING_NOT_ELIGIBLE_FOR_EARLY_CHECKOUT: 'Chỉ có thể trả phòng sớm khi khách đang lưu trú.',
  BOOKING_NOT_CHECKED_IN: 'Đặt phòng không còn ở trạng thái đang lưu trú.',
  BOOKING_ALREADY_CHECKED_OUT: 'Đặt phòng này đã được trả phòng trước đó.',
  BOOKING_NOT_FOUND: 'Không tìm thấy đặt phòng. Danh sách sẽ được cập nhật lại.',
  ROOM_NOT_ASSIGNED: 'Đặt phòng chưa có phòng vật lý được gán.',
  ROOM_NOT_OCCUPIED: 'Phòng đã gán không còn ở trạng thái đang sử dụng.',
  ROOM_NOT_FOUND: 'Không tìm thấy phòng đã gán. Vui lòng làm mới danh sách.',
  USER_NOT_FOUND: 'Không tìm thấy tài khoản đang thao tác. Vui lòng đăng nhập lại.',
  AUTH_UNAUTHORIZED: 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.',
  AUTH_FORBIDDEN: 'Tài khoản không có quyền thực hiện trả phòng.',
};

export function getCheckOutErrorMessage(error: unknown): string {
  if (error instanceof ApiClientError && error.code) {
    return CHECK_OUT_ERRORS[error.code] ?? error.message;
  }

  return error instanceof Error ? error.message : 'Không thể trả phòng. Vui lòng thử lại.';
}
