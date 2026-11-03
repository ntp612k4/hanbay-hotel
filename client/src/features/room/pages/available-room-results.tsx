import {
  ArrowRightOutlined,
  CalendarOutlined,
  HomeOutlined,
  TeamOutlined,
} from '@ant-design/icons';
import {
  Button,
  Card,
  Col,
  Empty,
  Image,
  Pagination,
  Popover,
  Row,
  Skeleton,
  Space,
  Statistic,
  Tag,
  Typography,
} from 'antd';

import type { AvailableRoomType } from '../types/available-room-search';
import type { PageResponse } from '../../../types/api';
import { getPriceAdjustments, type PriceAdjustmentView } from './price-adjustment-utils';
import {
  fallbackRoomImageUrl,
  formatCurrency,
  formatDisplayDate,
  type SearchFormValues,
} from './room-search-utils';
import '../components/room-card-content.css';

const { Text, Title } = Typography;

interface AvailableRoomResultsProps {
  availableRooms: PageResponse<AvailableRoomType> | null;
  currentValues: SearchFormValues;
  loading: boolean;
  nights: number;
  onBookRoom: (roomTypeId: string) => void;
  onPageChange: (page: number) => void;
}

export function AvailableRoomResults({
  availableRooms,
  currentValues,
  loading,
  nights,
  onBookRoom,
  onPageChange,
}: AvailableRoomResultsProps) {
  const [checkIn, checkOut] = currentValues.stayDates;

  return (
    <section className="room-section" aria-label="Hạng phòng còn trống">
      <div className="section-heading">
        <div>
          <Text className="eyebrow">Phòng nghỉ</Text>
          <Title level={2}>Hạng phòng còn trống</Title>
        </div>
        <Text type="secondary">
          {formatDisplayDate(checkIn)} - {formatDisplayDate(checkOut)} · {nights} đêm ·{' '}
          {currentValues.adults} người lớn, {currentValues.children} trẻ em
        </Text>
      </div>

      {renderRoomResults({ availableRooms, loading, onBookRoom, onPageChange })}
    </section>
  );
}

function renderRoomResults({
  availableRooms,
  loading,
  onBookRoom,
  onPageChange,
}: Pick<AvailableRoomResultsProps, 'availableRooms' | 'loading' | 'onBookRoom' | 'onPageChange'>) {
  if (loading) {
    return (
      <Row gutter={[24, 24]}>
        {[1, 2, 3].map((item) => (
          <Col xs={24} md={8} key={item}>
            <Card>
              <Skeleton.Image active className="room-skeleton-image" />
              <Skeleton active paragraph={{ rows: 4 }} />
            </Card>
          </Col>
        ))}
      </Row>
    );
  }

  if (availableRooms === null) {
    return (
      <Empty
        className="room-empty-state"
        image={Empty.PRESENTED_IMAGE_SIMPLE}
        description="Chọn tiêu chí và bấm tìm kiếm để xem hạng phòng còn trống."
      />
    );
  }

  if (!availableRooms.content.length) {
    return (
      <Empty
        className="room-empty-state"
        description="Không tìm thấy hạng phòng còn trống phù hợp với ngày lưu trú và số khách đã chọn."
      />
    );
  }

  return (
    <Space orientation="vertical" size="large" style={{ width: '100%' }}>
      <Row gutter={[24, 24]}>
        {availableRooms.content.map((room) => (
          <Col xs={24} md={8} key={room.id}>
            <AvailableRoomCard room={room} onBookRoom={onBookRoom} />
          </Col>
        ))}
      </Row>
      {availableRooms.totalPages > 1 && (
        <Pagination
          align="center"
          current={availableRooms.page + 1}
          pageSize={availableRooms.size}
          total={availableRooms.totalElements}
          showSizeChanger={false}
          onChange={onPageChange}
        />
      )}
    </Space>
  );
}

interface AvailableRoomCardProps {
  room: AvailableRoomType;
  onBookRoom: (roomTypeId: string) => void;
}

function renderPriceAdjustmentDetails(adjustments: PriceAdjustmentView[]) {
  if (!adjustments.length) {
    return (
      <Space className="room-card-price-popover" orientation="vertical" size={4}>
        <Text strong>Chi tiết điều chỉnh giá</Text>
        <Text type="secondary">Không có điều chỉnh giá trong thời gian lưu trú đã chọn.</Text>
      </Space>
    );
  }

  return (
    <Space className="room-card-price-popover" orientation="vertical" size={8}>
      <Text strong>Chi tiết điều chỉnh giá</Text>
      {adjustments.map((adjustment) => (
        <div
          className="room-card-price-popover__row"
          key={`${adjustment.stayDate}-${adjustment.name}`}
        >
          <Space orientation="vertical" size={0}>
            <Text strong>{adjustment.name}</Text>
            <Text type="secondary">
              {adjustment.stayDate.split('-').reverse().join('/')} · {adjustment.label}
            </Text>
          </Space>
          <Text strong>{`${adjustment.valueLabel} · ${adjustment.amountLabel}`}</Text>
        </div>
      ))}
      <Text className="room-card-price-popover__note" type="secondary">
        Các điều chỉnh trên đã được tính vào giá của từng đêm.
      </Text>
    </Space>
  );
}

function AvailableRoomCard({ room, onBookRoom }: AvailableRoomCardProps) {
  const totalNights = room.totalNights;
  const priceAdjustments = getPriceAdjustments(room);
  const nightlyBreakdown = room.nightlyPrices
    .map(
      (night) => `${night.stayDate.split('-').reverse().join('/')}: ${formatCurrency(night.price)}`,
    )
    .join(' · ');

  return (
    <Card
      className="room-card"
      cover={
        <Image
          alt={room.name}
          className="room-card-image"
          fallback={fallbackRoomImageUrl}
          height={240}
          preview={false}
          src={room.imageUrl ?? fallbackRoomImageUrl}
        />
      }
      actions={[
        <Button
          icon={<ArrowRightOutlined />}
          key="book"
          type="primary"
          onClick={() => onBookRoom(room.id)}
        >
          Chọn phòng
        </Button>,
      ]}
    >
      <div className="room-card-content">
        <div className="room-card-title">
          <Title className="room-card-name" level={4}>
            {room.name}
          </Title>
          <Tag color="blue">Còn {room.availableRooms} phòng</Tag>
        </div>
        <Typography.Paragraph className="room-card-description" type="secondary">
          {room.description ?? 'Đang cập nhật mô tả.'}
        </Typography.Paragraph>
        <Space className="room-card-features" wrap>
          <Tag icon={<TeamOutlined />}>
            Tối đa {room.adultCapacity} người lớn · {room.childCapacity} trẻ em · {room.maxGuests}{' '}
            khách
          </Tag>
          {room.bedType && <Tag icon={<HomeOutlined />}>{room.bedType}</Tag>}
          {room.areaM2 && <Tag>{room.areaM2}m²</Tag>}
          <Tag icon={<CalendarOutlined />}>{totalNights} đêm</Tag>
        </Space>
        <div className="room-card-pricing">
          <Popover
            content={renderPriceAdjustmentDetails(priceAdjustments)}
            placement="topLeft"
            trigger={['hover', 'focus']}
          >
            <div
              aria-label="Xem chi tiết giá phòng"
              className="room-card-price-summary"
              role="button"
              tabIndex={0}
            >
              <Text className="room-card-nightly-rate">
                Từ {formatCurrency(room.basePrice)} / đêm
              </Text>
              <Statistic
                title={`Tổng tiền phòng · ${totalNights} đêm`}
                value={room.roomTotal}
                formatter={(value) => formatCurrency(Number(value))}
              />
            </div>
          </Popover>
          {nightlyBreakdown && (
            <Text className="room-card-price-breakdown" type="secondary">
              {nightlyBreakdown}
            </Text>
          )}
        </div>
      </div>
    </Card>
  );
}
