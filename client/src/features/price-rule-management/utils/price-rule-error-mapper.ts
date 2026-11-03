import { ApiClientError } from '../../../types/api';

export function getPriceRuleErrorMessage(error: unknown): string {
  if (!(error instanceof ApiClientError)) {
    return 'Không thể kết nối đến hệ thống. Vui lòng kiểm tra kết nối và thử lại.';
  }

  switch (error.code) {
    case 'HOLIDAY_CALENDAR_NOT_FOUND':
      return 'Ngày lễ không còn tồn tại. Vui lòng tải lại danh sách.';
    case 'PRICE_RULE_NOT_FOUND':
      return 'Quy tắc giá không còn tồn tại. Vui lòng tải lại danh sách.';
    case 'PRICE_RULE_CONFLICT':
    case 'CONFLICT_001':
      return 'Dữ liệu vừa được thay đổi. Vui lòng tải lại và thử lại.';
    case 'ROOM_001':
      return 'Hạng phòng không tồn tại hoặc không còn kinh doanh.';
    case 'AUTH_FORBIDDEN':
      return 'Bạn không có quyền quản lý quy tắc giá.';
    case 'AUTH_UNAUTHORIZED':
      return 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.';
    case 'VALIDATION_FAILED':
      return 'Dữ liệu chưa hợp lệ. Vui lòng kiểm tra lại.';
    case 'MALFORMED_REQUEST':
      return 'Dữ liệu gửi lên không đúng định dạng.';
    case 'INTERNAL_SERVER_ERROR':
      return 'Hệ thống đang gặp sự cố. Vui lòng thử lại sau.';
    default:
      return error.message || 'Không thể hoàn thành thao tác. Vui lòng thử lại.';
  }
}

export function getPriceRuleFieldErrors(error: unknown): Array<{ name: string; errors: string[] }> {
  if (!(error instanceof ApiClientError)) return [];

  return Object.entries(error.data?.errors ?? {}).map(([name, value]) => ({
    name,
    errors: Array.isArray(value) ? value : [value],
  }));
}
