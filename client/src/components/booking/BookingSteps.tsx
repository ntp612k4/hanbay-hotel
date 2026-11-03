import { Steps } from 'antd';
import type { CSSProperties } from 'react';

interface BookingStepsProps {
  current: 0 | 1 | 2;
  paymentDisabled?: boolean;
  className?: string;
  style?: CSSProperties;
}

const bookingStepItems = [
  { title: 'Chọn phòng' },
  { title: 'Thông tin khách' },
  { title: 'Thanh toán' },
];

export function BookingSteps({
  current,
  paymentDisabled = true,
  className,
  style,
}: BookingStepsProps) {
  return (
    <Steps
      className={className}
      current={current}
      responsive
      style={style}
      items={bookingStepItems.map((item, index) => ({
        ...item,
        disabled: index === 2 && paymentDisabled,
      }))}
    />
  );
}
