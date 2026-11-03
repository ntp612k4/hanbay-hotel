import { describe, expect, it } from 'vitest';

import type { AvailableRoomType } from '../types/available-room-search';
import { getPriceAdjustments } from './price-adjustment-utils';

const room: AvailableRoomType = {
  id: 'room-type-id',
  code: 'DELUXE',
  name: 'Phòng Deluxe',
  description: null,
  basePrice: 1_000_000,
  maxGuests: 2,
  adultCapacity: 2,
  childCapacity: 0,
  bedType: null,
  areaM2: null,
  imageUrl: null,
  availableRooms: 1,
  totalNights: 1,
  roomSubtotal: 1_200_000,
  roomDiscount: 120_000,
  roomTotal: 1_080_000,
  nightlyPrices: [
    {
      stayDate: '2027-08-14',
      price: 1_080_000,
      adjustments: [
        {
          name: 'Phụ thu sự kiện',
          ruleType: 'SEASONAL',
          adjustmentDirection: 'INCREASE',
          adjustmentType: 'PERCENTAGE',
          adjustmentValue: 20,
          amount: 200_000,
        },
        {
          name: 'Ưu đãi mùa hè',
          ruleType: 'PROMOTION',
          adjustmentDirection: 'DISCOUNT',
          adjustmentType: 'PERCENTAGE',
          adjustmentValue: 10,
          amount: 120_000,
        },
      ],
    },
  ],
};

describe('getPriceAdjustments', () => {
  it('hiển thị giảm giá mùa vụ theo direction từ API', () => {
    const seasonalRoom: AvailableRoomType = {
      ...room,
      nightlyPrices: [
        {
          ...room.nightlyPrices[0],
          adjustments: [
            {
              ...room.nightlyPrices[0].adjustments[0],
              adjustmentDirection: 'DISCOUNT',
            },
          ],
        },
      ],
    };
    expect(getPriceAdjustments(seasonalRoom)[0]).toMatchObject({
      label: 'Giảm giá',
      valueLabel: '-20%',
      amountLabel: '-200.000 ₫',
    });
  });

  it('hiển thị riêng từng quy tắc thay vì suy đoán từ chênh lệch giá cuối', () => {
    expect(getPriceAdjustments(room)).toEqual([
      {
        stayDate: '2027-08-14',
        name: 'Phụ thu sự kiện',
        label: 'Phụ thu',
        valueLabel: '+20%',
        amountLabel: '+200.000 ₫',
      },
      {
        stayDate: '2027-08-14',
        name: 'Ưu đãi mùa hè',
        label: 'Giảm giá',
        valueLabel: '-10%',
        amountLabel: '-120.000 ₫',
      },
    ]);
  });
});
