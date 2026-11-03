import { Button, Card, DatePicker, Divider, Flex, InputNumber, Space, Typography } from 'antd';
import dayjs, { type Dayjs } from 'dayjs';
import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { StayPriceSummary } from '../../features/booking/components/stay-price-summary';
import { useStayPrice } from '../../features/booking/hooks/use-stay-price';
import { getStayPriceSelection } from '../../features/booking/utils/stay-price-selection';
import type { RoomTypeDetail } from '../../features/room/types/room-detail';
import { formatCurrency } from '../../features/room/pages/room-search-utils';

export function BookingCard({ roomType }: { roomType: RoomTypeDetail }) {
  const navigate = useNavigate();
  const [dates, setDates] = useState<[Dayjs | null, Dayjs | null] | null>(null);
  const [adults, setAdults] = useState(Math.min(2, roomType.capacityAdults));
  const [children, setChildren] = useState(0);
  const nights = useMemo(
    () => (dates?.[0] && dates[1] ? dates[1].diff(dates[0], 'day') : 0),
    [dates],
  );
  const stayPriceSelection = getStayPriceSelection(roomType.id, dates);
  const stayPriceRequest = stayPriceSelection.request;
  const stayPriceQuery = useStayPrice(stayPriceRequest);
  const guestCount = adults + children;
  const validGuests =
    adults >= 1 &&
    adults <= roomType.capacityAdults &&
    children <= roomType.capacityChildren &&
    guestCount <= roomType.maxGuests;
  const canContinue = nights > 0 && validGuests && stayPriceQuery.isSuccess;

  const continueBooking = () => {
    if (!canContinue || !dates?.[0] || !dates[1]) return;
    const params = new URLSearchParams({
      roomTypeId: roomType.id,
      checkIn: dates[0].format('YYYY-MM-DD'),
      checkOut: dates[1].format('YYYY-MM-DD'),
      adults: String(adults),
      children: String(children),
    });
    navigate(`/bookings/new?${params.toString()}`);
  };

  return (
    <Card title="Đặt loại phòng này" className="booking-card">
      <Space orientation="vertical" size="middle" style={{ width: '100%' }}>
        <Typography.Title level={3} style={{ margin: 0 }}>
          {formatCurrency(roomType.basePrice)}{' '}
          <Typography.Text type="secondary">/ đêm</Typography.Text>
        </Typography.Title>
        <DatePicker.RangePicker
          style={{ width: '100%' }}
          format="DD/MM/YYYY"
          placeholder={['Ngày nhận phòng', 'Ngày trả phòng']}
          disabledDate={(date) => date.isBefore(dayjs().add(1, 'day').startOf('day'))}
          onChange={setDates}
        />
        <Flex gap="middle">
          <Space orientation="vertical">
            <Typography.Text>Người lớn</Typography.Text>
            <InputNumber
              min={1}
              max={roomType.capacityAdults}
              value={adults}
              onChange={(value) => setAdults(value ?? 1)}
            />
          </Space>
          <Space orientation="vertical">
            <Typography.Text>Trẻ em</Typography.Text>
            <InputNumber
              min={0}
              max={roomType.capacityChildren}
              value={children}
              onChange={(value) => setChildren(value ?? 0)}
            />
          </Space>
        </Flex>
        {!validGuests && (
          <Typography.Text type="danger">
            Tổng số khách không được vượt quá {roomType.maxGuests} người.
          </Typography.Text>
        )}
        <Divider />
        <StayPriceSummary
          data={stayPriceQuery.data}
          error={stayPriceQuery.error}
          isLoading={stayPriceQuery.isLoading}
          validationMessage={stayPriceSelection.validationMessage}
          hasSelection={Boolean(stayPriceRequest)}
          onRetry={() => void stayPriceQuery.refetch()}
        />
        <Button type="primary" size="large" block disabled={!canContinue} onClick={continueBooking}>
          Tiếp tục đặt phòng
        </Button>
        <Typography.Text type="secondary">
          Phòng cụ thể sẽ được lễ tân sắp xếp khi nhận phòng.
        </Typography.Text>
      </Space>
    </Card>
  );
}
