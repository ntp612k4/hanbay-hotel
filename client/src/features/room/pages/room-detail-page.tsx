import { ArrowLeftOutlined } from '@ant-design/icons';
import { Button, Col, Flex, Result, Row, Space, Typography } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';

import { MainFooter } from '../../../components/layout/MainFooter';
import { MainHeader } from '../../../components/layout/MainHeader';
import { BookingCard } from '../../../components/room/BookingCard';
import { RoomDetailSkeleton } from '../../../components/room/RoomDetailSkeleton';
import { RoomGallery } from '../../../components/room/RoomGallery';
import { RoomInfo } from '../../../components/room/RoomInfo';
import { getRoomTypeDetail } from '../api/room-detail-api';
import './room-detail-page.css';

export function RoomDetailPage() {
  const { roomTypeId } = useParams();
  const detailQuery = useQuery({
    queryKey: ['room-types', 'detail', roomTypeId],
    queryFn: () => getRoomTypeDetail(roomTypeId!),
    enabled: Boolean(roomTypeId),
    retry: 1,
  });

  if (detailQuery.isLoading) {
    return (
      <>
        <MainHeader />
        <RoomDetailSkeleton />
      </>
    );
  }

  if (detailQuery.isError || !detailQuery.data) {
    return (
      <>
        <MainHeader />
        <Result
          status="404"
          title="Không tìm thấy loại phòng"
          subTitle="Loại phòng này không tồn tại hoặc hiện không còn kinh doanh."
          extra={
            <Space>
              <Button onClick={() => void detailQuery.refetch()}>Thử lại</Button>
              <Link to="/room-types">
                <Button type="primary">Xem các loại phòng</Button>
              </Link>
            </Space>
          }
        />
      </>
    );
  }

  const roomType = detailQuery.data;
  return (
    <Flex vertical className="room-detail-shell">
      <MainHeader />
      <main className="room-detail-page">
        <Space orientation="vertical" size="large" style={{ width: '100%' }}>
          <Link to="/room-types">
            <Button type="text" icon={<ArrowLeftOutlined />}>
              Quay lại danh sách loại phòng
            </Button>
          </Link>
          <div>
            <Typography.Title>{roomType.name}</Typography.Title>
            <Typography.Paragraph type="secondary" className="room-detail-lead">
              {roomType.shortDescription || roomType.description}
            </Typography.Paragraph>
          </div>
          <RoomGallery images={roomType.images} roomName={roomType.name} />
          <Row gutter={[32, 32]} align="top">
            <Col xs={24} lg={16}>
              <RoomInfo roomType={roomType} />
            </Col>
            <Col xs={24} lg={8}>
              <BookingCard roomType={roomType} />
            </Col>
          </Row>
        </Space>
      </main>
      <MainFooter />
    </Flex>
  );
}
