import type { Dayjs } from 'dayjs';

import type { CalculateStayPriceRequest } from '../types/stay-price';

interface StayPriceSelection {
  request?: CalculateStayPriceRequest;
  validationMessage?: string;
}

export function getStayPriceSelection(
  roomTypeId: string | undefined,
  dates: [Dayjs | null, Dayjs | null] | null,
): StayPriceSelection {
  const checkIn = dates?.[0];
  const checkOut = dates?.[1];
  if (!roomTypeId || !checkIn || !checkOut) return {};

  if (!checkOut.isAfter(checkIn, 'day')) {
    return { validationMessage: 'Ngày trả phòng phải sau ngày nhận phòng.' };
  }

  return {
    request: {
      roomTypeId,
      checkInDate: checkIn.format('YYYY-MM-DD'),
      checkOutDate: checkOut.format('YYYY-MM-DD'),
    },
  };
}
