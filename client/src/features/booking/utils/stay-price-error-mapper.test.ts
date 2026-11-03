import { describe, expect, it } from 'vitest';

import { ApiClientError } from '../../../types/api';
import { getStayPriceErrorMessage } from './stay-price-error-mapper';

describe('getStayPriceErrorMessage', () => {
  it.each([
    [
      new ApiClientError({ message: 'technical network detail', kind: 'network' }),
      'Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối và thử lại.',
    ],
    [
      new ApiClientError({ message: 'technical forbidden detail', kind: 'api', status: 403 }),
      'Bạn không có quyền xem giá lưu trú.',
    ],
    [
      new ApiClientError({
        message: 'technical room detail',
        kind: 'api',
        status: 404,
        code: 'ROOM_001',
      }),
      'Hạng phòng này không còn được kinh doanh.',
    ],
    [
      new ApiClientError({
        message: 'technical pricing detail',
        kind: 'api',
        status: 400,
        code: 'PRICING_VALIDATION_FAILED',
      }),
      'Không thể tính giá cho khoảng ngày đã chọn. Vui lòng chọn khoảng ngày khác.',
    ],
  ])('không hiển thị chi tiết kỹ thuật cho lỗi backend/network', (error, expected) => {
    expect(getStayPriceErrorMessage(error)).toBe(expected);
    expect(getStayPriceErrorMessage(error)).not.toContain('technical');
  });

  it('dùng thông báo an toàn cho lỗi không xác định', () => {
    expect(getStayPriceErrorMessage(new Error('stack or internal database detail'))).toBe(
      'Không thể tính giá lưu trú. Vui lòng thử lại.',
    );
  });
});
