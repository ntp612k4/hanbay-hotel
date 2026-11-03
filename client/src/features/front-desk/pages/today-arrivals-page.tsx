import { ReloadOutlined, TeamOutlined, WarningOutlined } from '@ant-design/icons';
import {
  Alert,
  Button,
  Card,
  Empty,
  Space,
  Table,
  Tag,
  Typography,
  type TableColumnsType,
} from 'antd';
import dayjs from 'dayjs';
import { useState } from 'react';

import { useTodayArrivals } from '../api/front-desk-query';
import type { TodayArrival } from '../types/today-arrivals';
import './today-arrivals-page.css';

const { Paragraph, Text, Title } = Typography;
const PAGE_SIZE = 20;
const formatDate = (value: string) => dayjs(value).format('DD/MM/YYYY');

const columns: TableColumnsType<TodayArrival> = [
  {
    title: 'Mã booking',
    dataIndex: 'bookingCode',
    render: (value: string) => <Text code>{value}</Text>,
  },
  {
    title: 'Khách chính',
    key: 'primaryGuest',
    render: (_value, arrival) =>
      arrival.primaryGuest ? (
        <Space orientation="vertical" size={0}>
          <Text strong>{arrival.primaryGuest.fullName}</Text>
          <Text type="secondary">{arrival.primaryGuest.phone ?? 'Chưa có số điện thoại'}</Text>
          {arrival.primaryGuest.email && <Text type="secondary">{arrival.primaryGuest.email}</Text>}
        </Space>
      ) : (
        <Tag icon={<WarningOutlined />} color="warning">
          Thiếu thông tin khách
        </Tag>
      ),
  },
  {
    title: 'Hạng phòng',
    key: 'roomType',
    render: (_value, arrival) => (
      <Space orientation="vertical" size={0}>
        <Text strong>{arrival.roomType.name}</Text>
        <Text type="secondary">{arrival.roomType.code}</Text>
      </Space>
    ),
  },
  {
    title: 'Lưu trú',
    key: 'stayDates',
    render: (_value, arrival) => (
      <Space orientation="vertical" size={0}>
        <Text>Nhận: {formatDate(arrival.checkInDate)}</Text>
        <Text>Trả: {formatDate(arrival.checkOutDate)}</Text>
      </Space>
    ),
  },
  {
    title: 'Số khách',
    key: 'guestCount',
    align: 'center',
    render: (_value, arrival) => (
      <Space orientation="vertical" size={0}>
        <Text>{arrival.adultCount} người lớn</Text>
        <Text type="secondary">{arrival.childCount} trẻ em</Text>
      </Space>
    ),
  },
  {
    title: 'Trạng thái',
    dataIndex: 'bookingStatus',
    render: () => <Tag color="success">Đã xác nhận</Tag>,
  },
];

export function TodayArrivalsPage() {
  const [page, setPage] = useState(0);
  const arrivalsQuery = useTodayArrivals({ page, size: PAGE_SIZE });
  const arrivals = arrivalsQuery.data?.content ?? [];

  return (
    <main className="today-arrivals-page">
      <div className="today-arrivals-page__header">
        <div>
          <Title level={2}>Khách đến hôm nay</Title>
          <Paragraph type="secondary">
            Danh sách đặt phòng đã xác nhận dự kiến nhận phòng trong ngày.
          </Paragraph>
        </div>
        <Button
          icon={<ReloadOutlined />}
          loading={arrivalsQuery.isFetching}
          onClick={() => void arrivalsQuery.refetch()}
        >
          Làm mới
        </Button>
      </div>

      <Card>
        {!arrivalsQuery.isLoading && !arrivalsQuery.isError && (
          <div className="today-arrivals-page__summary">
            <TeamOutlined />
            <Text>
              Tổng cộng <Text strong>{arrivalsQuery.data?.totalElements ?? 0}</Text> booking dự kiến
              đến
            </Text>
          </div>
        )}

        {arrivalsQuery.isError && (
          <Alert
            showIcon
            type="error"
            title="Không thể tải danh sách khách đến hôm nay"
            description={arrivalsQuery.error.message}
            action={
              <Button onClick={() => void arrivalsQuery.refetch()} aria-label="Thử tải lại">
                Thử lại
              </Button>
            }
            className="today-arrivals-page__feedback"
          />
        )}

        {!arrivalsQuery.isError &&
          (!arrivalsQuery.isLoading && arrivalsQuery.data?.totalElements === 0 ? (
            <Empty
              description="Hôm nay không có khách dự kiến nhận phòng"
              image={Empty.PRESENTED_IMAGE_SIMPLE}
            >
              <Button onClick={() => void arrivalsQuery.refetch()}>Làm mới</Button>
            </Empty>
          ) : (
            <Table<TodayArrival>
              columns={columns}
              dataSource={arrivals}
              rowKey="bookingId"
              loading={arrivalsQuery.isLoading}
              pagination={{
                current: page + 1,
                pageSize: PAGE_SIZE,
                total: arrivalsQuery.data?.totalElements ?? 0,
                showSizeChanger: false,
                hideOnSinglePage: true,
                onChange: (nextPage) => setPage(nextPage - 1),
              }}
              scroll={{ x: 'max-content' }}
            />
          ))}
      </Card>
    </main>
  );
}
