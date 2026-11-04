import { ResponsiveTable as Table } from '../../../components/data/responsive-table';
import {
  CheckCircleOutlined,
  ClockCircleOutlined,
  CloseCircleOutlined,
  EyeOutlined,
  ReloadOutlined,
  SearchOutlined,
} from '@ant-design/icons';
import {
  Alert,
  Button,
  Card,
  Col,
  Descriptions,
  Drawer,
  Input,
  Row,
  Select,
  Space,
  Spin,
  Statistic,
  Tabs,
  Tag,
  Typography,
  type TableColumnsType,
} from 'antd';
import dayjs from 'dayjs';
import { useDeferredValue, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';

import { useAuth } from '../../authentication/providers/auth-context';
import { USER_ROLES } from '../../../types/common';
import {
  useMonitoredPayments,
  useMonitoredWebhookEvents,
  usePaymentMonitoringSummary,
  useWebhookEventDetail,
} from '../api/payment-monitoring-query';
import type {
  MonitoredPayment,
  MonitoredWebhookEvent,
  PaymentStatus,
  WebhookStatus,
} from '../types/payment-monitoring';
import './payment-monitoring-page.css';

const { Paragraph, Text, Title } = Typography;
const money = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' });
const PAGE_SIZE = 10;

const paymentStatus: Record<PaymentStatus, { color: string; label: string }> = {
  PENDING: { color: 'processing', label: 'Chờ thanh toán' },
  SUCCESS: { color: 'success', label: 'Thành công' },
  FAILED: { color: 'error', label: 'Thất bại' },
  CANCELLED: { color: 'default', label: 'Đã hủy' },
  EXPIRED: { color: 'default', label: 'Hết hạn' },
  REFUNDED: { color: 'purple', label: 'Đã hoàn tiền' },
  PARTIALLY_REFUNDED: { color: 'magenta', label: 'Hoàn tiền một phần' },
};

const webhookStatus: Record<WebhookStatus, { color: string; label: string }> = {
  RECEIVED: { color: 'blue', label: 'Đã tiếp nhận' },
  PROCESSING: { color: 'processing', label: 'Đang xử lý' },
  PROCESSED: { color: 'success', label: 'Đã xử lý' },
  FAILED: { color: 'error', label: 'Thất bại' },
  IGNORED: { color: 'default', label: 'Bỏ qua' },
};

const formatTime = (value?: string | null) =>
  value ? dayjs(value).format('DD/MM/YYYY HH:mm:ss') : '—';

export function PaymentMonitoringPage() {
  const { roles } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const isAdmin = roles.includes(USER_ROLES.admin);
  const activeTab = location.pathname.endsWith('/webhook-events') ? 'webhooks' : 'payments';
  const [query, setQuery] = useState('');
  const deferredQuery = useDeferredValue(query);
  const [status, setStatus] = useState<string>();
  const [page, setPage] = useState(0);
  const [selectedEventId, setSelectedEventId] = useState<string>();
  const params = { page, size: PAGE_SIZE, keyword: deferredQuery, status };

  const summaryQuery = usePaymentMonitoringSummary();
  const paymentsQuery = useMonitoredPayments(params, activeTab === 'payments');
  const eventsQuery = useMonitoredWebhookEvents(params, activeTab === 'webhooks');
  const detailQuery = useWebhookEventDetail(selectedEventId, isAdmin);
  const currentQuery = activeTab === 'payments' ? paymentsQuery : eventsQuery;

  const paymentColumns: TableColumnsType<MonitoredPayment> = [
    {
      title: 'Booking',
      dataIndex: 'bookingCode',
      render: (value) => <Text strong>{value}</Text>,
    },
    {
      title: 'Mã thanh toán',
      dataIndex: 'paymentCode',
      render: (value) => <Text code>{value}</Text>,
    },
    {
      title: 'Số tiền',
      dataIndex: 'amount',
      align: 'right',
      render: (value) => money.format(value),
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      render: (value: PaymentStatus) => (
        <Tag color={paymentStatus[value].color}>{paymentStatus[value].label}</Tag>
      ),
    },
    { title: 'Khởi tạo lúc', dataIndex: 'createdAt', render: formatTime },
    { title: 'Thanh toán lúc', dataIndex: 'paidAt', render: formatTime },
  ];

  const webhookColumns: TableColumnsType<MonitoredWebhookEvent> = [
    { title: 'Event ID', dataIndex: 'eventId', render: (value) => <Text code>{value}</Text> },
    { title: 'Ngân hàng', dataIndex: 'gateway', render: (value) => value ?? '—' },
    {
      title: 'Mã thanh toán',
      dataIndex: 'paymentCode',
      render: (value) => (value ? <Text code>{value}</Text> : '—'),
    },
    {
      title: 'Số tiền',
      dataIndex: 'transferAmount',
      align: 'right',
      render: (value) => money.format(value),
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      render: (value: WebhookStatus) => (
        <Tag color={webhookStatus[value].color}>{webhookStatus[value].label}</Tag>
      ),
    },
    { title: 'Số lần thử', dataIndex: 'attempts', align: 'center' },
    { title: 'Tiếp nhận lúc', dataIndex: 'receivedAt', render: formatTime },
    ...(isAdmin
      ? [
          {
            title: 'Thao tác',
            key: 'action',
            fixed: 'right' as const,
            render: (_: unknown, event: MonitoredWebhookEvent) => (
              <Button
                type="link"
                icon={<EyeOutlined />}
                onClick={() => setSelectedEventId(event.id)}
              >
                Chi tiết
              </Button>
            ),
          },
        ]
      : []),
  ];

  const statusOptions =
    activeTab === 'payments'
      ? Object.entries(paymentStatus).map(([value, item]) => ({ value, label: item.label }))
      : Object.entries(webhookStatus).map(([value, item]) => ({ value, label: item.label }));

  const refresh = () => {
    void summaryQuery.refetch();
    void currentQuery.refetch();
  };

  const detail = detailQuery.data;

  return (
    <div className="payment-monitoring-page">
      <div className="payment-monitoring-page__header">
        <Title level={2}>Giám sát thanh toán</Title>
        <Paragraph type="secondary">
          Theo dõi trạng thái thanh toán và webhook giao dịch ngân hàng từ một màn hình tập trung.
        </Paragraph>
      </div>

      {summaryQuery.isError && (
        <Alert
          showIcon
          type="error"
          message="Không thể tải dữ liệu tổng quan"
          description={summaryQuery.error.message}
          action={<Button onClick={() => void summaryQuery.refetch()}>Thử lại</Button>}
          style={{ marginBottom: 16 }}
        />
      )}

      <Row gutter={[16, 16]} style={{ marginBottom: 20 }}>
        <Col xs={24} sm={12} xl={6}>
          <Card>
            <Statistic
              loading={summaryQuery.isLoading}
              title="Tổng thanh toán"
              value={summaryQuery.data?.totalPayments ?? 0}
              prefix={<ClockCircleOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} xl={6}>
          <Card>
            <Statistic
              loading={summaryQuery.isLoading}
              title="Đã thanh toán"
              value={summaryQuery.data?.successfulPayments ?? 0}
              prefix={<CheckCircleOutlined />}
              valueStyle={{ color: '#389e0d' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} xl={6}>
          <Card>
            <Statistic
              loading={summaryQuery.isLoading}
              title="Tổng webhook"
              value={summaryQuery.data?.totalWebhookEvents ?? 0}
              prefix={<ReloadOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} xl={6}>
          <Card>
            <Statistic
              loading={summaryQuery.isLoading}
              title="Webhook lỗi"
              value={summaryQuery.data?.failedWebhookEvents ?? 0}
              prefix={<CloseCircleOutlined />}
              valueStyle={{ color: '#cf1322' }}
            />
          </Card>
        </Col>
      </Row>

      <Card>
        <Tabs
          activeKey={activeTab}
          onChange={(key) => {
            setQuery('');
            setStatus(undefined);
            setPage(0);
            navigate(
              key === 'payments'
                ? '/payment-monitoring/payments'
                : '/payment-monitoring/webhook-events',
            );
          }}
          items={[
            { key: 'payments', label: 'Thanh toán' },
            { key: 'webhooks', label: 'Sự kiện webhook' },
          ]}
        />

        <div className="payment-monitoring-page__toolbar">
          <div className="payment-monitoring-page__filters">
            <Input
              allowClear
              className="payment-monitoring-page__search"
              prefix={<SearchOutlined />}
              placeholder={
                activeTab === 'payments' ? 'Booking hoặc mã thanh toán' : 'Event hoặc mã thanh toán'
              }
              value={query}
              onChange={(event) => {
                setQuery(event.target.value);
                setPage(0);
              }}
            />
            <Select
              allowClear
              placeholder="Tất cả trạng thái"
              style={{ minWidth: 180 }}
              options={statusOptions}
              value={status}
              onChange={(value) => {
                setStatus(value);
                setPage(0);
              }}
            />
          </div>
          <Button icon={<ReloadOutlined />} loading={currentQuery.isFetching} onClick={refresh}>
            Làm mới
          </Button>
        </div>

        {currentQuery.isError && (
          <Alert
            showIcon
            type="error"
            message="Không thể tải dữ liệu monitoring"
            description={currentQuery.error.message}
            action={<Button onClick={() => void currentQuery.refetch()}>Thử lại</Button>}
            style={{ marginBottom: 16 }}
          />
        )}

        {activeTab === 'payments' ? (
          <Table<MonitoredPayment>
            className="payment-monitoring-page__table"
            columns={paymentColumns}
            dataSource={paymentsQuery.data?.content ?? []}
            loading={paymentsQuery.isLoading || paymentsQuery.isFetching}
            rowKey="id"
            pagination={{
              current: page + 1,
              pageSize: PAGE_SIZE,
              total: paymentsQuery.data?.totalElements ?? 0,
              showSizeChanger: false,
              onChange: (nextPage) => setPage(nextPage - 1),
            }}
            scroll={{ x: 'max-content' }}
          />
        ) : (
          <Table<MonitoredWebhookEvent>
            className="payment-monitoring-page__table"
            columns={webhookColumns}
            dataSource={eventsQuery.data?.content ?? []}
            loading={eventsQuery.isLoading || eventsQuery.isFetching}
            rowKey="id"
            pagination={{
              current: page + 1,
              pageSize: PAGE_SIZE,
              total: eventsQuery.data?.totalElements ?? 0,
              showSizeChanger: false,
              onChange: (nextPage) => setPage(nextPage - 1),
            }}
            scroll={{ x: 'max-content' }}
          />
        )}
      </Card>

      <Drawer
        title="Webhook Event Detail"
        rootClassName="payment-monitoring-drawer"
        size={640}
        open={isAdmin && Boolean(selectedEventId)}
        onClose={() => setSelectedEventId(undefined)}
        destroyOnHidden
      >
        {detailQuery.isLoading && <Spin description="Đang tải chi tiết webhook" />}
        {detailQuery.isError && (
          <Alert
            showIcon
            type="error"
            message="Không thể tải chi tiết webhook"
            description={detailQuery.error.message}
          />
        )}
        {detail && (
          <Space orientation="vertical" size="large" style={{ width: '100%' }}>
            {detail.lastError && <Alert type="error" showIcon message={detail.lastError} />}
            <Descriptions bordered column={1} size="small">
              <Descriptions.Item label="Event ID">{detail.eventId}</Descriptions.Item>
              <Descriptions.Item label="Loại event">{detail.eventType ?? '—'}</Descriptions.Item>
              <Descriptions.Item label="Trạng thái">
                <Tag color={webhookStatus[detail.status].color}>
                  {webhookStatus[detail.status].label}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Mã thanh toán">
                {detail.paymentCode ?? '—'}
              </Descriptions.Item>
              <Descriptions.Item label="Deduplication key">
                <Text code>{detail.deduplicationKey}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="API key hợp lệ">
                {detail.signatureValid ? 'Có' : 'Không'}
              </Descriptions.Item>
              <Descriptions.Item label="Số lần xử lý">{detail.attempts}</Descriptions.Item>
              <Descriptions.Item label="Tiếp nhận lúc">
                {formatTime(detail.receivedAt)}
              </Descriptions.Item>
              <Descriptions.Item label="Xử lý xong lúc">
                {formatTime(detail.processedAt)}
              </Descriptions.Item>
            </Descriptions>
            <Card size="small" title="Raw Payload">
              <pre className="payment-monitoring-page__payload">
                {JSON.stringify(detail.payload, null, 2)}
              </pre>
            </Card>
          </Space>
        )}
      </Drawer>
    </div>
  );
}
