import { CheckCircleOutlined, TeamOutlined } from '@ant-design/icons';
import { Card, Col, Descriptions, Empty, Row, Space, Typography } from 'antd';

import type { RoomTypeDetail } from '../../features/room/types/room-detail';

export function RoomInfo({ roomType }: { roomType: RoomTypeDetail }) {
  return (
    <Space orientation="vertical" size="large" style={{ width: '100%' }}>
      <Card title="Thông tin loại phòng">
        <Descriptions column={{ xs: 1, sm: 3 }}>
          <Descriptions.Item label="Sức chứa tối đa">
            <TeamOutlined /> {roomType.maxGuests} khách
          </Descriptions.Item>
          <Descriptions.Item label="Người lớn">{roomType.capacityAdults}</Descriptions.Item>
          <Descriptions.Item label="Trẻ em">{roomType.capacityChildren}</Descriptions.Item>
        </Descriptions>
        <Typography.Title level={3}>Mô tả</Typography.Title>
        {roomType.description
          .split(/\n\s*\n/)
          .filter(Boolean)
          .map((text) => (
            <Typography.Paragraph key={text}>{text}</Typography.Paragraph>
          ))}
      </Card>
      <Card title="Tiện nghi nổi bật">
        {roomType.amenities.length === 0 ? (
          <Empty description="Chưa có thông tin tiện nghi" />
        ) : (
          <Row gutter={[16, 16]}>
            {roomType.amenities.map((amenity) => (
              <Col xs={24} sm={12} key={amenity.id}>
                <Space>
                  <CheckCircleOutlined className="amenity-icon" />
                  <Typography.Text>{amenity.name}</Typography.Text>
                </Space>
              </Col>
            ))}
          </Row>
        )}
      </Card>
    </Space>
  );
}
