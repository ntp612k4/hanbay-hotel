import { CalendarOutlined, DollarOutlined, HomeOutlined, ReloadOutlined } from '@ant-design/icons';
import { Alert, Button, Col, Empty, Row, Skeleton, Typography } from 'antd';
import dayjs from 'dayjs';
import { useMemo, useState } from 'react';

import { useDashboardReport } from '../api/dashboard-query';
import { BookingStatusChart } from '../components/booking-status-chart';
import { DashboardFilters } from '../components/dashboard-filters';
import { MetricCard } from '../components/metric-card';
import { TrendChart } from '../components/trend-chart';
import './dashboard-page.css';

const { Paragraph, Text, Title } = Typography;
const currencyFormatter = new Intl.NumberFormat('vi-VN', {
  style: 'currency',
  currency: 'VND',
  maximumFractionDigits: 0,
});
const integerFormatter = new Intl.NumberFormat('vi-VN');

export function DashboardPage() {
  const defaultParams = useMemo(
    () => ({
      from: dayjs().startOf('month').format('YYYY-MM-DD'),
      to: dayjs().add(1, 'day').format('YYYY-MM-DD'),
      currency: 'VND',
    }),
    [],
  );
  const [params, setParams] = useState(defaultParams);
  const reportQuery = useDashboardReport(params);
  const report = reportQuery.data;
  const isEmpty =
    report?.revenue.grossRevenue === 0 &&
    report.revenue.refundedAmount === 0 &&
    report.bookings.totalBookings === 0 &&
    (report.maintenance?.recordedExpense ?? 0) === 0 &&
    (report.maintenance?.completedWithoutCost ?? 0) === 0;

  return (
    <main className="manager-dashboard">
      <header className="manager-dashboard__header">
        <div>
          <Title level={2}>Tổng quan khách sạn</Title>
          <Paragraph type="secondary">
            Theo dõi tiền đã thu, tiền đã hoàn, chi phí bảo trì và lượt đặt phòng trong kỳ.
          </Paragraph>
        </div>
        <div className="manager-dashboard__period">
          <CalendarOutlined />
          <Text>
            {dayjs(params.from).format('DD/MM/YYYY')} –{' '}
            {dayjs(params.to).subtract(1, 'day').format('DD/MM/YYYY')}
          </Text>
          <Button
            type="text"
            icon={<ReloadOutlined />}
            loading={reportQuery.isFetching}
            aria-label="Làm mới báo cáo"
            onClick={() => void reportQuery.refetch()}
          />
        </div>
      </header>

      <DashboardFilters
        key={`${params.from}-${params.to}`}
        from={params.from}
        to={params.to}
        onApply={(from, to) => setParams({ ...params, from, to })}
        onReset={() => setParams(defaultParams)}
      />

      {reportQuery.isError && (
        <Alert
          showIcon
          type="error"
          title="Không thể tải báo cáo tổng quan"
          description={reportQuery.error.message}
          action={<Button onClick={() => void reportQuery.refetch()}>Thử lại</Button>}
        />
      )}

      {reportQuery.isLoading && (
        <div aria-label="Đang tải báo cáo dashboard" role="status">
          <Row gutter={[16, 16]}>
            {[0, 1, 2, 3].map((item) => (
              <Col xs={24} sm={12} xl={6} key={item}>
                <Skeleton.Node active className="manager-dashboard__metric-skeleton" />
              </Col>
            ))}
          </Row>
          <Skeleton active paragraph={{ rows: 10 }} className="manager-dashboard__chart-skeleton" />
        </div>
      )}

      {!reportQuery.isLoading && report && (
        <>
          <Row gutter={[16, 16]}>
            <Col xs={24} sm={12} xl={6}>
              <MetricCard
                title="Tiền thu sau hoàn"
                value={report.revenue.netRevenue}
                formatter={(value) => currencyFormatter.format(value)}
                icon={<DollarOutlined />}
                tone="green"
                caption={`Đã thu ${currencyFormatter.format(report.revenue.grossRevenue)} · đã hoàn ${currencyFormatter.format(report.revenue.refundedAmount)}`}
              />
            </Col>
            <Col xs={24} sm={12} xl={6}>
              <MetricCard
                title="Chi phí bảo trì"
                value={report.maintenance?.recordedExpense ?? 0}
                formatter={(value) => currencyFormatter.format(value)}
                icon={<DollarOutlined />}
                tone="blue"
                caption="Tổng chi phí đã ghi nhận theo ngày chi"
              />
            </Col>
            <Col xs={24} sm={12} xl={6}>
              <MetricCard
                title="Tổng lượt đặt phòng"
                value={report.bookings.totalBookings}
                formatter={(value) => integerFormatter.format(value)}
                icon={<HomeOutlined />}
                tone="violet"
                caption={`${report.bookings.confirmedBookings} chờ nhận phòng · ${report.bookings.checkedInBookings} đang lưu trú · ${report.bookings.checkedOutBookings} đã trả phòng`}
              />
            </Col>
            <Col xs={24} sm={12} xl={6}>
              <MetricCard
                title="Còn lại sau bảo trì"
                value={report.maintenance?.cashAfterMaintenance ?? report.revenue.netRevenue}
                formatter={(value) => currencyFormatter.format(value)}
                icon={<DollarOutlined />}
                tone="amber"
                caption="Tiền đã thu − tiền đã hoàn − chi phí bảo trì đã ghi nhận"
              />
            </Col>
          </Row>

          <Typography.Paragraph type="secondary" style={{ marginTop: 16 }}>
            Tiền thu gồm các giao dịch thành công qua ngân hàng và tiền mặt đã ghi nhận. Số còn lại
            chưa trừ các chi phí vận hành khác, không phải lợi nhuận toàn bộ khách sạn.
          </Typography.Paragraph>
          {!!report.maintenance?.completedWithoutCost && (
            <Alert
              showIcon
              type="warning"
              title={`${report.maintenance.completedWithoutCost} yêu cầu bảo trì hoàn thành trong kỳ chưa nhập chi phí. Số tiền còn lại chưa phản ánh đầy đủ các khoản chi này.`}
            />
          )}
          {isEmpty ? (
            <section className="manager-dashboard__empty">
              <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description="Chưa có dữ liệu kinh doanh trong khoảng thời gian này"
              >
                <Button onClick={() => void reportQuery.refetch()}>Làm mới dữ liệu</Button>
              </Empty>
            </section>
          ) : (
            <div className="manager-dashboard__charts">
              <TrendChart
                title="Xu hướng tiền thu sau hoàn"
                description="Tiền thanh toán thành công trừ tiền hoàn theo ngày"
                points={report.revenueTrend.map((item) => ({
                  date: item.date,
                  value: item.netRevenue,
                }))}
                color="#059669"
                formatValue={(value) => currencyFormatter.format(value)}
              />
              <TrendChart
                title="Chi phí bảo trì theo ngày"
                description="Khoản chi theo ngày chi đã ghi nhận"
                points={(report.maintenance?.trend ?? []).map((item) => ({
                  date: item.date,
                  value: item.amount,
                }))}
                color="#0284c7"
                formatValue={(value) => currencyFormatter.format(value)}
              />
              <TrendChart
                title="Xu hướng đặt phòng"
                description="Số lượt đặt phòng mới theo ngày"
                points={report.bookingTrend.map((item) => ({
                  date: item.date,
                  value: item.totalBookings,
                }))}
                color="#7c3aed"
                formatValue={(value) => `${integerFormatter.format(value)} đặt phòng`}
              />
              <BookingStatusChart bookings={report.bookings} />
            </div>
          )}
        </>
      )}
    </main>
  );
}
