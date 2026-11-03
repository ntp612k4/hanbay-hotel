import { Card, Col, Row, Skeleton, Space } from 'antd';

export function RoomDetailSkeleton() {
  return (
    <main className="room-detail-page" aria-label="Đang tải chi tiết loại phòng">
      <Space orientation="vertical" size="large" style={{ width: '100%' }}>
        <Skeleton active paragraph={{ rows: 2 }} />
        <Skeleton.Image active className="room-gallery-skeleton" />
        <Row gutter={[32, 32]}>
          <Col xs={24} lg={16}>
            <Card>
              <Skeleton active />
            </Card>
          </Col>
          <Col xs={24} lg={8}>
            <Card>
              <Skeleton active />
            </Card>
          </Col>
        </Row>
      </Space>
    </main>
  );
}
