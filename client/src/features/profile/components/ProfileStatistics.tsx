import { CalendarOutlined, SafetyCertificateOutlined } from '@ant-design/icons';
import { Badge, Card, Col, Row, Statistic, theme } from 'antd';
import dayjs from 'dayjs';

import type { User } from '../types/profile';

const statusLabels = { ACTIVE: 'Đang hoạt động', INACTIVE: 'Chưa kích hoạt', LOCKED: 'Đã khóa' };
const statusBadge = { ACTIVE: 'success', INACTIVE: 'warning', LOCKED: 'error' } as const;

export function ProfileStatistics({ profile }: { profile: User }) {
  const { token } = theme.useToken();
  const statusColor = {
    ACTIVE: token.colorSuccess,
    INACTIVE: token.colorWarning,
    LOCKED: token.colorError,
  }[profile.status];

  return (
    <Row gutter={[12, 12]}>
      <Col xs={24} sm={8}>
        <Card size="small" className="profile-status-card">
          <Statistic
            title="Trạng thái"
            value={statusLabels[profile.status]}
            prefix={
              <Badge
                status={statusBadge[profile.status]}
                className="profile-status-card__indicator"
              />
            }
            valueStyle={{ color: statusColor }}
          />
        </Card>
      </Col>
      <Col xs={24} sm={8}>
        <Card size="small">
          <Statistic
            title="Số vai trò"
            value={profile.roles.length}
            prefix={<SafetyCertificateOutlined />}
          />
        </Card>
      </Col>
      <Col xs={24} sm={8}>
        <Card size="small">
          <Statistic
            title="Ngày tham gia"
            value={dayjs(profile.createdAt).format('DD/MM/YYYY')}
            prefix={<CalendarOutlined />}
          />
        </Card>
      </Col>
    </Row>
  );
}
