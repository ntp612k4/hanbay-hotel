import { ApiClientError } from '../../../types/api';

export function getAddOnErrorMessage(error: unknown): string {
  if (!(error instanceof ApiClientError)) {
    return 'Không thể kết nối đến hệ thống. Vui lòng kiểm tra kết nối và thử lại.';
  }
  switch (error.code) {
    case 'ADD_ON_CODE_ALREADY_EXISTS':
      return 'Mã dịch vụ đã tồn tại.';
    case 'ADD_ON_NOT_FOUND':
      return 'Dịch vụ không còn tồn tại. Vui lòng tải lại danh sách.';
    case 'CONFLICT_001':
      return 'Dữ liệu vừa được thay đổi. Vui lòng tải lại và thử lại.';
    case 'AUTH_FORBIDDEN':
      return 'Bạn không có quyền quản lý dịch vụ bổ sung.';
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

export function getAddOnFieldErrors(error: unknown): Array<{ name: string; errors: string[] }> {
  if (!(error instanceof ApiClientError)) return [];
  if (error.code === 'ADD_ON_CODE_ALREADY_EXISTS') {
    return [{ name: 'code', errors: ['Mã dịch vụ đã tồn tại'] }];
  }
  return Object.entries(error.data?.errors ?? {}).map(([name, value]) => ({
    name,
    errors: Array.isArray(value) ? value : [value],
  }));
}
