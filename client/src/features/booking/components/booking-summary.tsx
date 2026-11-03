import { HomeOutlined } from '@ant-design/icons';
import { Button, Card, Divider, Flex, Grid, Tag, theme, Typography } from 'antd';
import type { Dayjs } from 'dayjs';
import type { ReactNode } from 'react';

import type { RoomTypeCatalogItem, SelectedAddOn } from '../types/booking';

interface BookingSummaryProps {
  roomType?: RoomTypeCatalogItem;
  stayDates?: [Dayjs, Dayjs];
  adults?: number;
  children?: number;
  addOns: SelectedAddOn[];
  submitting: boolean;
  onSubmit: () => void;
}

export function BookingSummary({
  roomType,
  stayDates,
  adults = 2,
  children = 0,
  addOns,
  submitting,
  onSubmit,
}: BookingSummaryProps) {
  const { token } = theme.useToken();
  const screens = Grid.useBreakpoint();
  const nights = stayDates?.[1].diff(stayDates[0], 'day') ?? 0;
  const roomSubtotal = roomType?.roomTotal ?? 0;
  const addOnSubtotal = addOns.reduce((total, addOn) => {
    return total + addOn.price * addOn.quantity;
  }, 0);

  return (
    <Card
      className={screens.xl ? 'booking-summary-sticky' : undefined}
      title={
        <Flex vertical gap={2}>
          <Typography.Text strong>Tóm tắt đặt phòng</Typography.Text>
          <Typography.Text type="secondary" style={{ fontSize: 13, fontWeight: 400 }}>
            Xem lại chi tiết đặt phòng của bạn
          </Typography.Text>
        </Flex>
      }
      style={{ borderColor: token.colorBorder }}
      styles={{ header: { background: token.colorFillAlter } }}
    >
      {roomType?.imageUrl ? (
        <img
          src={roomType.imageUrl}
          alt={roomType.name}
          style={{
            width: '100%',
            height: screens.xs ? 210 : 180,
            borderRadius: token.borderRadiusLG,
            objectFit: 'cover',
          }}
        />
      ) : (
        <Flex
          align="center"
          justify="center"
          style={{
            height: screens.xs ? 210 : 180,
            borderRadius: token.borderRadiusLG,
            background: token.colorFillAlter,
            color: token.colorTextTertiary,
            fontSize: 34,
          }}
        >
          <HomeOutlined />
        </Flex>
      )}

      <Flex justify="space-between" align="start" gap={16} style={{ marginTop: 18 }}>
        <Flex vertical>
          <Typography.Text strong>{roomType?.name || 'Chưa chọn hạng phòng'}</Typography.Text>
          <Typography.Text type="secondary">{roomType?.bedType || roomType?.code}</Typography.Text>
        </Flex>
        <Typography.Text strong className="numeric-data">
          {formatMoney(roomType?.basePrice ?? 0)}
        </Typography.Text>
      </Flex>
      <Flex
        justify="space-between"
        align="center"
        gap={12}
        wrap
        style={{ marginBlock: '6px 18px' }}
      >
        <Typography.Text type="secondary" style={{ fontSize: 13 }}>
          {stayDates
            ? `${stayDates[0].format('DD/MM/YYYY')} – ${stayDates[1].format('DD/MM/YYYY')}`
            : 'Chưa chọn ngày'}
        </Typography.Text>
        <Tag>{nights} đêm</Tag>
      </Flex>

      <SummaryLine
        label="Khách"
        value={`${adults} người lớn${children > 0 ? `, ${children} trẻ em` : ''}`}
      />
      <Divider />
      <Typography.Text
        strong
        style={{ fontSize: 12, letterSpacing: '0.1em', textTransform: 'uppercase' }}
      >
        Dịch vụ tùy chọn
      </Typography.Text>
      {addOns.length ? (
        addOns.map((addOn) => (
          <SummaryLine
            key={addOn.id}
            label={`${addOn.name} × ${addOn.quantity}`}
            value={formatMoney(addOn.price * addOn.quantity)}
          />
        ))
      ) : (
        <Typography.Paragraph type="secondary" style={{ marginTop: 10 }}>
          Chưa chọn dịch vụ
        </Typography.Paragraph>
      )}
      <Divider />
      <SummaryLine label="Tiền phòng tạm tính" value={formatMoney(roomSubtotal)} />
      <SummaryLine label="Dịch vụ tạm tính" value={formatMoney(addOnSubtotal)} />
      <Divider style={{ borderColor: token.colorTextTertiary }} />
      <SummaryLine
        label={<Typography.Text strong>Tạm tính</Typography.Text>}
        value={
          <Typography.Text strong>{formatMoney(roomSubtotal + addOnSubtotal)}</Typography.Text>
        }
      />
      <Typography.Paragraph
        type="secondary"
        style={{ marginBottom: 22, fontSize: 12, fontStyle: 'italic', textAlign: 'right' }}
      >
        Tổng tiền cuối cùng sẽ được xác nhận ở bước thanh toán.
      </Typography.Paragraph>
      <Button type="primary" size="large" block loading={submitting} onClick={onSubmit}>
        Kiểm tra đơn hàng
      </Button>
    </Card>
  );
}

function SummaryLine({ label, value }: { label: ReactNode; value: ReactNode }) {
  return (
    <Flex justify="space-between" align="center" gap={12} style={{ marginBlock: 10, fontSize: 13 }}>
      <span>{label}</span>
      <strong className="numeric-data">{value}</strong>
    </Flex>
  );
}

function formatMoney(value: number) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value);
}
