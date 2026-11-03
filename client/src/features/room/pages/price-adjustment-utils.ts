import type { AvailableRoomType } from '../types/available-room-search';
import { formatCurrency } from './room-search-utils';

export interface PriceAdjustmentView {
  stayDate: string;
  name: string;
  label: string;
  valueLabel: string;
  amountLabel: string;
}

export function getPriceAdjustments(room: AvailableRoomType): PriceAdjustmentView[] {
  return room.nightlyPrices.flatMap((night) =>
    night.adjustments.map((adjustment) => {
      const isDiscount = adjustment.adjustmentDirection === 'DISCOUNT';
      const sign = isDiscount ? '-' : '+';
      const label = isDiscount ? 'Giảm giá' : 'Phụ thu';
      const valueLabel =
        adjustment.adjustmentType === 'PERCENTAGE'
          ? `${sign}${adjustment.adjustmentValue}%`
          : `${sign}${formatCurrency(adjustment.adjustmentValue)}`;

      return {
        stayDate: night.stayDate,
        name: adjustment.name,
        label,
        valueLabel,
        amountLabel: `${sign}${formatCurrency(adjustment.amount)}`,
      };
    }),
  );
}
