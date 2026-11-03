import { MailOutlined, PhoneOutlined } from '@ant-design/icons';
import { Card, Descriptions, Space } from 'antd';
import dayjs from 'dayjs';

import type { User } from '../types/profile';

export function ProfileSidePanels({ profile }: { profile: User }) {
  return (
    <Card title="Thông tin tài khoản" size="small" className="profile-side-panel">
      <Descriptions column={1} size="small">
        <Descriptions.Item
          label={
            <Space size={8}>
              <MailOutlined />
              <span>Email</span>
            </Space>
          }
        >
          {profile.email}
        </Descriptions.Item>
        <Descriptions.Item
          label={
            <Space size={8}>
              <PhoneOutlined />
              <span>Điện thoại</span>
            </Space>
          }
        >
          {profile.phone || 'Chưa cập nhật'}
        </Descriptions.Item>
        <Descriptions.Item label="Cập nhật gần nhất">
          {dayjs(profile.updatedAt).format('DD/MM/YYYY HH:mm')}
        </Descriptions.Item>
      </Descriptions>
    </Card>
  );
}
