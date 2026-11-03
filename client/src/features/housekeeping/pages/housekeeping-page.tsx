import {
  ApartmentOutlined,
  ClearOutlined,
  FileTextOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import {
  Alert,
  App,
  Button,
  Card,
  Col,
  Empty,
  Row,
  Space,
  Statistic,
  Table,
  Tag,
  Typography,
  type TableColumnsType,
} from 'antd';
import { useEffect, useState } from 'react';

import { useDirtyRooms, useHousekeepingAction } from '../api/housekeeping-query';
import type { DirtyRoom } from '../types/dirty-rooms';
import './housekeeping-page.css';

const { Paragraph, Text, Title } = Typography;
const PAGE_SIZE = 20;

function getRoomAction(status: DirtyRoom['status']) {
  if (status === 'DIRTY') return { mutation: 'start' as const, label: 'Bắt đầu dọn' };
  if (status === 'CLEANING') return { mutation: 'complete' as const, label: 'Hoàn tất dọn' };
  return undefined;
}

function buildColumns(
  onAction: (room: DirtyRoom) => void,
  pendingRoomId?: string,
): TableColumnsType<DirtyRoom> {
  return [
    {
      title: 'Phòng',
      dataIndex: 'roomNumber',
      fixed: 'left',
      render: (roomNumber: string) => (
        <Space orientation="vertical" size={4}>
          <Text strong className="housekeeping-page__room-number">
            {roomNumber}
          </Text>
          <Tag color="warning" className="housekeeping-page__status-tag">
            Cần xử lý
          </Tag>
        </Space>
      ),
    },
    {
      title: 'Hạng phòng',
      key: 'roomType',
      render: (_value, room) => (
        <Space orientation="vertical" size={2}>
          <Text strong>{room.roomType.name}</Text>
          <Text type="secondary">{room.roomType.code}</Text>
        </Space>
      ),
    },
    {
      title: 'Tầng',
      dataIndex: 'floor',
      align: 'center',
      render: (floor: number) => <Tag color="blue">Tầng {floor}</Tag>,
    },
    {
      title: 'Ghi chú vận hành',
      dataIndex: 'note',
      render: (note: string | null) =>
        note ? (
          <Paragraph ellipsis={{ rows: 2, expandable: true, symbol: 'Xem thêm' }}>{note}</Paragraph>
        ) : (
          <Text type="secondary">Không có ghi chú</Text>
        ),
    },
    {
      title: 'Trạng thái / thao tác',
      key: 'action',
      fixed: 'right',
      render: (_value, room) => {
        const action = getRoomAction(room.status);
        const statusLabel =
          room.status === 'DIRTY'
            ? 'Chờ dọn'
            : room.status === 'CLEANING'
              ? 'Đang dọn'
              : 'Sẵn sàng';
        const statusColor =
          room.status === 'DIRTY'
            ? 'warning'
            : room.status === 'CLEANING'
              ? 'processing'
              : 'success';

        return (
          <Space orientation="vertical" size={6}>
            <Tag color={statusColor}>{statusLabel}</Tag>
            {action && (
              <Button
                type="primary"
                loading={pendingRoomId === room.roomId}
                disabled={pendingRoomId !== undefined && pendingRoomId !== room.roomId}
                onClick={() => onAction(room)}
              >
                {action.label}
              </Button>
            )}
          </Space>
        );
      },
    },
  ];
}

export function HousekeepingPage() {
  const { message } = App.useApp();
  const [page, setPage] = useState(0);
  const dirtyRoomsQuery = useDirtyRooms({ page, size: PAGE_SIZE });
  const housekeepingAction = useHousekeepingAction();
  const dirtyRooms = dirtyRoomsQuery.data?.content ?? [];
  const currentPageFloorCount = new Set(dirtyRooms.map((room) => room.floor)).size;
  const roomsWithNotes = dirtyRooms.filter((room) => Boolean(room.note?.trim())).length;
  const columns = buildColumns(
    (room) => {
      const action = getRoomAction(room.status);
      if (!action) return;

      housekeepingAction.mutate(
        { roomId: room.roomId, action: action.mutation },
        {
          onSuccess: () =>
            void message.success(
              room.status === 'DIRTY'
                ? `Đã bắt đầu dọn phòng ${room.roomNumber}.`
                : `Phòng ${room.roomNumber} đã sẵn sàng.`,
            ),
          onError: (error: Error) => void message.error(error.message),
        },
      );
    },
    housekeepingAction.isPending ? housekeepingAction.variables?.roomId : undefined,
  );

  useEffect(() => {
    const totalPages = dirtyRoomsQuery.data?.totalPages;
    let nextPage: number | undefined;

    if (totalPages !== undefined && totalPages > 0 && page >= totalPages) {
      nextPage = totalPages - 1;
    } else if (totalPages === 0 && page !== 0) {
      nextPage = 0;
    }

    if (nextPage === undefined) return;

    const timeoutId = window.setTimeout(() => setPage(nextPage), 0);
    return () => window.clearTimeout(timeoutId);
  }, [dirtyRoomsQuery.data?.totalPages, page]);

  return (
    <main className="housekeeping-page">
      <header className="housekeeping-page__header">
        <div>
          <Text className="housekeeping-page__eyebrow">BUỒNG PHÒNG</Text>
          <Title level={2}>Phòng cần dọn</Title>
          <Paragraph type="secondary">
            Theo dõi các phòng đang chờ vệ sinh và ghi chú vận hành theo từng tầng.
          </Paragraph>
        </div>
        <Button
          icon={<ReloadOutlined />}
          loading={dirtyRoomsQuery.isFetching}
          onClick={() => void dirtyRoomsQuery.refetch()}
        >
          Làm mới
        </Button>
      </header>

      <Row gutter={[16, 16]}>
        <Col xs={24} md={8}>
          <Card className="housekeeping-page__metric housekeeping-page__metric--primary">
            <Statistic
              title="Tổng phòng cần dọn"
              value={dirtyRoomsQuery.data?.totalElements ?? 0}
              prefix={<ClearOutlined />}
              suffix="phòng"
            />
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card className="housekeeping-page__metric">
            <Statistic
              title="Tầng trong trang hiện tại"
              value={currentPageFloorCount}
              prefix={<ApartmentOutlined />}
              suffix="tầng"
            />
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card className="housekeeping-page__metric">
            <Statistic
              title="Phòng có ghi chú trong trang"
              value={roomsWithNotes}
              prefix={<FileTextOutlined />}
              suffix="phòng"
            />
          </Card>
        </Col>
      </Row>

      <Card
        className="housekeeping-page__list-card"
        title={
          <Space>
            <ClearOutlined />
            <span>Danh sách phòng chờ dọn</span>
          </Space>
        }
        extra={
          !dirtyRoomsQuery.isLoading &&
          !dirtyRoomsQuery.isError && (
            <Text type="secondary">
              {dirtyRoomsQuery.data?.totalElements ?? 0} phòng đang chờ xử lý
            </Text>
          )
        }
      >
        {dirtyRoomsQuery.isError && (
          <Alert
            showIcon
            type="error"
            title="Không thể tải danh sách phòng cần dọn"
            description={dirtyRoomsQuery.error.message}
            action={
              <Button onClick={() => void dirtyRoomsQuery.refetch()} aria-label="Thử tải lại">
                Thử lại
              </Button>
            }
            className="housekeeping-page__feedback"
          />
        )}

        {!dirtyRoomsQuery.isError &&
          (!dirtyRoomsQuery.isLoading && dirtyRoomsQuery.data?.totalElements === 0 ? (
            <Empty
              description="Hiện không có phòng nào cần dọn"
              image={Empty.PRESENTED_IMAGE_SIMPLE}
            >
              <Button onClick={() => void dirtyRoomsQuery.refetch()}>Làm mới</Button>
            </Empty>
          ) : (
            <Table<DirtyRoom>
              columns={columns}
              dataSource={dirtyRooms}
              rowKey="roomId"
              loading={dirtyRoomsQuery.isLoading}
              rowClassName="housekeeping-page__table-row"
              pagination={{
                current: page + 1,
                pageSize: PAGE_SIZE,
                total: dirtyRoomsQuery.data?.totalElements ?? 0,
                showSizeChanger: false,
                hideOnSinglePage: true,
                showTotal: (total) => `Tổng ${total} phòng`,
                onChange: (nextPage) => setPage(nextPage - 1),
              }}
              scroll={{ x: 760 }}
            />
          ))}
      </Card>
    </main>
  );
}

export default HousekeepingPage;
