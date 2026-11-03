import { Alert, Button, Divider, Flex, Skeleton, Typography } from 'antd';
import dayjs from 'dayjs';

import { formatCurrency } from '../../room/pages/room-search-utils';
import type { CalculateStayPriceResponse } from '../types/stay-price';
import { getStayPriceErrorMessage } from '../utils/stay-price-error-mapper';

interface StayPriceSummaryProps {
  data?: CalculateStayPriceResponse;
  error: Error | null;
  isLoading: boolean;
  validationMessage?: string;
  hasSelection: boolean;
  onRetry: () => void;
}

export function StayPriceSummary({
  data,
  error,
  isLoading,
  validationMessage,
  hasSelection,
  onRetry,
}: StayPriceSummaryProps) {
  if (validationMessage) {
    return <Alert showIcon type="warning" message={validationMessage} />;
  }

  if (!hasSelection) {
    return (
      <Typography.Text type="secondary">Chọn ngày nhận và trả phòng để xem giá.</Typography.Text>
    );
  }

  if (isLoading) {
    return (
      <div aria-label="Đang tính giá lưu trú">
        <Skeleton active title={false} paragraph={{ rows: 3 }} />
      </div>
    );
  }

  if (error) {
    return (
      <Alert
        showIcon
        type="error"
        message={getStayPriceErrorMessage(error)}
        action={
          <Button size="small" onClick={onRetry}>
            Thử lại
          </Button>
        }
      />
    );
  }

  if (!data) return null;

  return (
    <div aria-label="Chi tiết giá từng đêm">
      <Flex justify="space-between" align="center">
        <Typography.Text strong>{data.numberOfNights} đêm</Typography.Text>
        <Typography.Text type="secondary">Giá theo từng đêm</Typography.Text>
      </Flex>
      <Divider style={{ marginBlock: 12 }} />
      {data.nightlyPrices.map((night) => (
        <Flex key={night.date} justify="space-between" gap={16} style={{ marginBlock: 8 }}>
          <Typography.Text>{dayjs(night.date).format('DD/MM/YYYY')}</Typography.Text>
          <Typography.Text className="numeric-data">
            {formatCurrency(night.finalPrice)}
          </Typography.Text>
        </Flex>
      ))}
      <Divider style={{ marginBlock: 12 }} />
      <Flex justify="space-between" align="center" gap={16}>
        <Typography.Text strong>Tổng tiền phòng</Typography.Text>
        <Typography.Title level={4} className="numeric-data" style={{ margin: 0 }}>
          {formatCurrency(data.totalPrice)}
        </Typography.Title>
      </Flex>
    </div>
  );
}
