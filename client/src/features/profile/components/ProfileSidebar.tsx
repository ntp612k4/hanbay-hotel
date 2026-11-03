import { ClockCircleOutlined, UserOutlined } from '@ant-design/icons';
import { Avatar, Card, Flex, Menu, Space, Tag, Typography } from 'antd';

import type { UserRole } from '../../../types/common';
import type { User } from '../types/profile';

export type ProfileSection = 'personal' | 'bookings';

const roleLabels: Record<UserRole, string> = {
  ADMIN: 'Quản trị viên',
  MANAGER: 'Quản lý',
  RECEPTIONIST: 'Lễ tân',
  HOUSEKEEPER: 'Buồng phòng',
};

export function ProfileSidebar({
  profile,
  activeSection,
  onSectionChange,
}: {
  profile: User;
  activeSection: ProfileSection;
  onSectionChange: (section: ProfileSection) => void;
}) {
  const initials = profile.fullName
    .split(/\s+/)
    .slice(-2)
    .map((part) => part[0])
    .join('')
    .toUpperCase();

  return (
    <Flex vertical gap={16}>
      <Card className="profile-summary-card">
        <Flex vertical align="center" gap={10}>
          <Avatar size={88} className="profile-summary-card__avatar">
            {initials || <UserOutlined />}
          </Avatar>
          <Typography.Text strong className="profile-summary-card__name">
            {profile.fullName}
          </Typography.Text>
          <Typography.Text type="secondary">{profile.email}</Typography.Text>
          <Space size={[4, 4]} wrap className="profile-summary-card__roles">
            {profile.roles.length ? (
              profile.roles.map((role) => (
                <Tag key={role} color="geekblue">
                  {roleLabels[role]}
                </Tag>
              ))
            ) : (
              <Tag color="geekblue">Khách hàng</Tag>
            )}
          </Space>
        </Flex>
      </Card>

      <Card className="profile-navigation-card" styles={{ body: { padding: 8 } }}>
        <Menu
          mode="inline"
          selectedKeys={[activeSection]}
          onClick={({ key }) => onSectionChange(key as ProfileSection)}
          items={[
            { key: 'personal', icon: <UserOutlined />, label: 'Thông tin cá nhân' },
            ...(profile.roles.length === 0
              ? [
                  {
                    key: 'bookings',
                    icon: <ClockCircleOutlined />,
                    label: 'Lịch sử đặt phòng',
                  },
                ]
              : []),
          ]}
        />
      </Card>
    </Flex>
  );
}
