import type { BookingSummary } from '../types/dashboard-report';

interface BookingStatusChartProps {
  bookings: BookingSummary;
}

export function BookingStatusChart({ bookings }: BookingStatusChartProps) {
  const statuses = [
    { label: 'Đã xác nhận', value: bookings.confirmedBookings, color: '#0284c7' },
    { label: 'Đang lưu trú', value: bookings.checkedInBookings, color: '#059669' },
    { label: 'Đã trả phòng', value: bookings.checkedOutBookings, color: '#7c3aed' },
    { label: 'Đã hủy', value: bookings.cancelledBookings, color: '#dc2626' },
    { label: 'Chờ xử lý', value: bookings.pendingBookings, color: '#d97706' },
    { label: 'Hết hạn', value: bookings.expiredBookings, color: '#64748b' },
  ];
  const maxValue = Math.max(...statuses.map((status) => status.value), 1);

  return (
    <section className="manager-dashboard__chart" aria-label="Booking theo trạng thái">
      <div className="manager-dashboard__chart-heading">
        <div>
          <h3>Booking theo trạng thái</h3>
          <span>Phân bổ trạng thái hiện tại của booking trong kỳ</span>
        </div>
        <strong>{bookings.totalBookings} booking</strong>
      </div>
      <div className="manager-dashboard__status-bars">
        {statuses.map((status) => (
          <div className="manager-dashboard__status-row" key={status.label}>
            <span>{status.label}</span>
            <div className="manager-dashboard__status-track">
              <div
                className="manager-dashboard__status-fill"
                style={{ width: `${(status.value / maxValue) * 100}%`, background: status.color }}
              />
            </div>
            <strong>{status.value}</strong>
          </div>
        ))}
      </div>
    </section>
  );
}
