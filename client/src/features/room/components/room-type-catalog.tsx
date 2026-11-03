import {
  Alert,
  Button,
  Card,
  Col,
  Empty,
  Flex,
  Image,
  Input,
  Pagination,
  Row,
  Select,
  Skeleton,
  Space,
  theme,
  Typography,
} from 'antd';
import { useState } from 'react';
import { Link } from 'react-router-dom';

import { ApiClientError } from '../../../types/api';
import { useRoomTypeCatalog } from '../api/room-type-query';
import type { RoomTypeSortField, SortDirection } from '../types/room-type';
import { ROOM_FALLBACK_IMAGE, roomPriceFormatter } from '../utils/room-presentation';

import './room-type-catalog.css';
import './room-card-content.css';

const PAGE_SIZE = 6;

export function RoomTypeCatalog() {
  const { token } = theme.useToken();
  const [page, setPage] = useState(0);
  const [searchInput, setSearchInput] = useState('');
  const [keyword, setKeyword] = useState('');
  const [sort, setSort] = useState('name-asc');
  const [sortBy, direction] = sort.split('-') as [RoomTypeSortField, SortDirection];

  const catalogQuery = useRoomTypeCatalog({ page, size: PAGE_SIZE, keyword, sortBy, direction });
  const clearSearch = () => {
    setSearchInput('');
    setKeyword('');
    setPage(0);
  };
  const errorMessage =
    catalogQuery.error instanceof ApiClientError
      ? catalogQuery.error.message
      : 'Không thể tải danh sách loại phòng. Vui lòng thử lại.';

  return (
    <section id="rooms" className="room-type-catalog" aria-labelledby="room-catalog-title">
      <div className="room-type-catalog__container">
        <Flex vertical align="center" style={{ marginBottom: token.marginXL, textAlign: 'center' }}>
          <Typography.Text
            className="room-type-catalog__kicker"
            style={{ color: token.colorPrimary }}
          >
            KHÔNG GIAN NGHỈ DƯỠNG
          </Typography.Text>
          <Typography.Title
            id="room-catalog-title"
            level={2}
            style={{ marginBlock: token.marginXS }}
          >
            Chọn căn phòng dành riêng cho bạn
          </Typography.Title>
        </Flex>

        <Flex
          className="room-type-catalog__toolbar"
          wrap="wrap"
          justify="space-between"
          gap={token.marginMD}
          style={{ marginBottom: token.marginLG }}
        >
          <Input.Search
            allowClear
            value={searchInput}
            placeholder="Tìm loại phòng..."
            aria-label="Tìm loại phòng theo tên"
            enterButton="Tìm kiếm"
            onChange={(event) => setSearchInput(event.target.value)}
            onSearch={(value) => {
              setKeyword(value.trim());
              setPage(0);
            }}
            className="room-type-catalog__search"
          />
          <Select
            aria-label="Sắp xếp loại phòng"
            value={sort}
            onChange={(value) => {
              setSort(value);
              setPage(0);
            }}
            className="room-type-catalog__sort"
            options={[
              { value: 'name-asc', label: 'Tên A–Z' },
              { value: 'name-desc', label: 'Tên Z–A' },
              { value: 'basePrice-asc', label: 'Giá thấp đến cao' },
              { value: 'basePrice-desc', label: 'Giá cao đến thấp' },
            ]}
          />
        </Flex>

        {catalogQuery.isPending && <CatalogSkeleton />}
        {catalogQuery.isError && (
          <Alert
            type="error"
            showIcon
            message="Chưa thể tải danh sách phòng"
            description={errorMessage}
            action={<Button onClick={() => void catalogQuery.refetch()}>Thử lại</Button>}
          />
        )}
        {catalogQuery.isSuccess && catalogQuery.data.content.length === 0 && (
          <Empty description="Không tìm thấy loại phòng">
            {keyword && <Button onClick={clearSearch}>Xóa tìm kiếm</Button>}
          </Empty>
        )}

        {catalogQuery.data && catalogQuery.data.content.length > 0 && (
          <Space orientation="vertical" size={token.marginMD} style={{ width: '100%' }}>
            <Typography.Text type="secondary" aria-live="polite">
              {catalogQuery.data.totalElements} loại phòng phù hợp
            </Typography.Text>
            <Row
              gutter={[token.marginLG, token.marginLG]}
              style={{ opacity: catalogQuery.isFetching ? 0.55 : 1 }}
            >
              {catalogQuery.data.content.map((room) => (
                <Col xs={24} md={12} lg={8} key={room.id}>
                  <Card
                    className="room-type-card"
                    hoverable
                    cover={
                      <Image
                        preview={false}
                        height={250}
                        src={room.thumbnailUrl || ROOM_FALLBACK_IMAGE}
                        fallback={ROOM_FALLBACK_IMAGE}
                        alt={`Không gian ${room.name}`}
                        style={{ objectFit: 'cover' }}
                      />
                    }
                    style={{ height: '100%' }}
                  >
                    <Link
                      className="room-type-card__detail-link"
                      to={`/room-types/${room.id}`}
                      aria-label={`Xem chi tiết ${room.name}`}
                    />
                    <Flex className="room-type-card__content" vertical gap={token.marginSM}>
                      <Typography.Title
                        className="room-type-card__title"
                        level={3}
                        style={{ margin: 0 }}
                      >
                        {room.name}
                      </Typography.Title>
                      <Typography.Paragraph className="room-card-description" type="secondary">
                        {room.description || 'Không gian nghỉ dưỡng tiện nghi và thoải mái.'}
                      </Typography.Paragraph>
                      <Space wrap>
                        <Typography.Text>{room.capacityAdults} người lớn</Typography.Text>
                        <Typography.Text>{room.capacityChildren} trẻ em</Typography.Text>
                      </Space>
                      <Flex
                        className="room-type-card__footer"
                        align="center"
                        justify="space-between"
                        gap={token.marginSM}
                        style={{ marginTop: 'auto' }}
                      >
                        <Typography.Text strong>
                          Từ {roomPriceFormatter.format(room.basePrice)} / đêm
                        </Typography.Text>
                        <Link className="room-type-card__booking-link" to="/rooms/search">
                          <Button type="primary">Đặt chỗ ngay</Button>
                        </Link>
                      </Flex>
                    </Flex>
                  </Card>
                </Col>
              ))}
            </Row>
            {catalogQuery.data.totalPages > 1 && (
              <Flex justify="center">
                <Pagination
                  current={catalogQuery.data.page + 1}
                  pageSize={PAGE_SIZE}
                  total={catalogQuery.data.totalElements}
                  showSizeChanger={false}
                  onChange={(nextPage) => setPage(nextPage - 1)}
                />
              </Flex>
            )}
          </Space>
        )}
      </div>
    </section>
  );
}

function CatalogSkeleton() {
  return (
    <Row gutter={[24, 24]} role="status" aria-label="Đang tải danh sách loại phòng">
      {Array.from({ length: 3 }, (_, index) => (
        <Col xs={24} md={12} lg={8} key={index}>
          <Card cover={<Skeleton.Image active style={{ width: '100%', height: 250 }} />}>
            <Skeleton active paragraph={{ rows: 3 }} />
          </Card>
        </Col>
      ))}
    </Row>
  );
}
