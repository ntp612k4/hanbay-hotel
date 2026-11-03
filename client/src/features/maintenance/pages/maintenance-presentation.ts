import { ApiClientError } from '../../../types/api';
import type { MaintenanceRequestStatus } from '../types/maintenance';

export const maintenanceStatusLabels: Record<MaintenanceRequestStatus, string> = {
  PENDING: 'Chờ xử lý',
  IN_PROGRESS: 'Đang xử lý',
  COMPLETED: 'Hoàn thành',
};

export const nextMaintenanceStatus: Partial<
  Record<MaintenanceRequestStatus, MaintenanceRequestStatus>
> = {
  PENDING: 'IN_PROGRESS',
  IN_PROGRESS: 'COMPLETED',
};

export function maintenanceErrorMessage(error: unknown): string {
  if (error instanceof ApiClientError) {
    if (error.code === 'ROOM_MAINTENANCE_START_CONFLICT')
      return 'Phòng không còn trống để bảo trì. Vui lòng chọn phòng khác.';
    if (error.code === 'MAINTENANCE_REQUEST_STATUS_CONFLICT')
      return 'Yêu cầu đã đổi trạng thái. Dữ liệu sẽ được cập nhật, vui lòng kiểm tra lại.';
    if (error.data?.errors) return Object.values(error.data.errors).flat().join('. ');
  }
  return error instanceof Error ? error.message : 'Không thể thực hiện thao tác. Vui lòng thử lại.';
}

export const formatMaintenanceTime = (value: string | null) =>
  value
    ? new Intl.DateTimeFormat('vi-VN', {
        dateStyle: 'short',
        timeStyle: 'short',
        timeZone: 'Asia/Ho_Chi_Minh',
      }).format(new Date(value))
    : '—';
