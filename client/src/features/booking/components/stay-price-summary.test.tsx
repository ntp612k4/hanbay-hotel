// @vitest-environment jsdom

import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';

import { ApiClientError } from '../../../types/api';
import { StayPriceSummary } from './stay-price-summary';

describe('StayPriceSummary', () => {
  it('hiển thị empty state và validation feedback', () => {
    const { rerender } = render(
      <StayPriceSummary error={null} isLoading={false} hasSelection={false} onRetry={vi.fn()} />,
    );
    expect(screen.getByText('Chọn ngày nhận và trả phòng để xem giá.')).toBeTruthy();

    rerender(
      <StayPriceSummary
        error={null}
        isLoading={false}
        validationMessage="Ngày trả phòng phải sau ngày nhận phòng."
        hasSelection={false}
        onRetry={vi.fn()}
      />,
    );
    expect(screen.getByText('Ngày trả phòng phải sau ngày nhận phòng.')).toBeTruthy();
  });

  it('hiển thị loading state', () => {
    render(<StayPriceSummary error={null} isLoading hasSelection onRetry={vi.fn()} />);
    expect(screen.getByLabelText('Đang tính giá lưu trú')).toBeTruthy();
  });

  it('hiển thị number of nights, từng đêm và totalPrice của backend', () => {
    render(
      <StayPriceSummary
        data={{
          roomType: {
            id: '10000000-0000-0000-0000-000000000001',
            code: 'DLX',
            name: 'Deluxe',
          },
          checkInDate: '2026-08-20',
          checkOutDate: '2026-08-22',
          numberOfNights: 2,
          nightlyPrices: [
            { date: '2026-08-20', finalPrice: 1_000_000 },
            { date: '2026-08-21', finalPrice: 1_300_000 },
          ],
          totalPrice: 9_999_999,
        }}
        error={null}
        isLoading={false}
        hasSelection
        onRetry={vi.fn()}
      />,
    );

    expect(screen.getByText('2 đêm')).toBeTruthy();
    expect(screen.getByText('20/08/2026')).toBeTruthy();
    expect(screen.getByText('21/08/2026')).toBeTruthy();
    expect(screen.getByText('1.000.000 ₫')).toBeTruthy();
    expect(screen.getByText('1.300.000 ₫')).toBeTruthy();
    expect(screen.getByText('9.999.999 ₫')).toBeTruthy();
  });

  it('map lỗi authorization an toàn và cho phép retry', () => {
    const onRetry = vi.fn();
    render(
      <StayPriceSummary
        error={new ApiClientError({ message: 'technical', kind: 'api', status: 401 })}
        isLoading={false}
        hasSelection
        onRetry={onRetry}
      />,
    );

    expect(screen.getByText('Vui lòng đăng nhập để xem giá lưu trú.')).toBeTruthy();
    expect(screen.queryByText('technical')).toBeNull();
    fireEvent.click(screen.getByRole('button', { name: 'Thử lại' }));
    expect(onRetry).toHaveBeenCalledOnce();
  });
});
