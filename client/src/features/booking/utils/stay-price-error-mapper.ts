import { ApiClientError } from '../../../types/api';

export function getStayPriceErrorMessage(error: Error): string {
  if (!(error instanceof ApiClientError)) {
    return 'Không thể tính giá lưu trú. Vui lòng thử lại.';
  }

  if (error.kind === 'network') {
    return 'Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối và thử lại.';
  }

  if (error.status === 401) {
    return 'Vui lòng đăng nhập để xem giá lưu trú.';
  }
  if (error.status === 403) {
    return 'Bạn không có quyền xem giá lưu trú.';
  }
  if (error.code === 'ROOM_001') {
    return 'Hạng phòng này không còn được kinh doanh.';
  }
  if (error.code === 'PRICING_VALIDATION_FAILED') {
    return 'Không thể tính giá cho khoảng ngày đã chọn. Vui lòng chọn khoảng ngày khác.';
  }

  return 'Không thể tính giá lưu trú. Vui lòng thử lại.';
}
