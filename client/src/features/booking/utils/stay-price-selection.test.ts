import dayjs from 'dayjs';
import { describe, expect, it } from 'vitest';

import { getStayPriceSelection } from './stay-price-selection';

describe('getStayPriceSelection', () => {
  it('không tạo request khi thiếu room hoặc ngày', () => {
    expect(getStayPriceSelection(undefined, null).request).toBeUndefined();
    expect(
      getStayPriceSelection('room-type-id', [dayjs('2026-08-20'), null]).request,
    ).toBeUndefined();
  });

  it.each([
    ['bằng ngày nhận phòng', '2026-08-20'],
    ['trước ngày nhận phòng', '2026-08-19'],
  ])('validation khi check-out %s', (_label, checkOut) => {
    const selection = getStayPriceSelection('room-type-id', [dayjs('2026-08-20'), dayjs(checkOut)]);

    expect(selection.request).toBeUndefined();
    expect(selection.validationMessage).toBe('Ngày trả phòng phải sau ngày nhận phòng.');
  });

  it('tạo request LocalDate đúng format khi khoảng ngày hợp lệ', () => {
    expect(
      getStayPriceSelection('room-type-id', [dayjs('2026-08-20'), dayjs('2026-08-23')]).request,
    ).toEqual({
      roomTypeId: 'room-type-id',
      checkInDate: '2026-08-20',
      checkOutDate: '2026-08-23',
    });
  });
});
