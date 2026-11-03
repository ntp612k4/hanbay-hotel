import { Col, Flex, Layout, Row, Space, theme, Typography } from 'antd';
import { Link } from 'react-router-dom';

const contactItems = [
  '92 Võ Nguyên Giáp, Sơn Trà, Đà Nẵng',
  '+84 236 86 82 88',
  'hello@hanbayhotel.com',
];

const quickLinks = [
  { label: 'Tổng quan', to: '/' },
  { label: 'Tiện nghi', href: '/#amenities' },
  { label: 'Đặt phòng', to: '/rooms/search' },
  { label: 'Gợi ý phòng', href: '/#rooms' },
];

const roomTypes = [
  'Phòng Superior',
  'Phòng Deluxe hướng biển',
  'Phòng Executive hướng thành phố',
  'Phòng Suite Skyline',
];

export function MainFooter() {
  const { token } = theme.useToken();

  return (
    <Layout.Footer
      id="contact"
      aria-label="Thông tin khách sạn HANBAY"
      style={{
        marginTop: 'auto',
        padding: `${token.paddingXL}px ${token.padding}px`,
        background: '#0f172a',
      }}
      className="main-footer"
    >
      <Row
        gutter={[token.marginXXL, token.marginXL]}
        style={{ maxWidth: 1440, marginInline: 'auto' }}
      >
        <Col xs={24} md={12} lg={9}>
          <Flex vertical gap={token.marginSM} align="flex-start">
            <Typography.Title level={3} className="main-footer__brand">
              HANBAY HOTEL
            </Typography.Title>
            <Typography.Paragraph className="main-footer__description">
              Trải nghiệm lưu trú đẳng cấp với tầm nhìn biển, dịch vụ chuẩn 5 sao và không gian nghỉ
              dưỡng tinh tế ngay trung tâm Đà Nẵng.
            </Typography.Paragraph>
            <Typography.Text className="main-footer__text">
              © 2026 Khách sạn HANBAY Đà Nẵng. Bảo lưu mọi quyền.
            </Typography.Text>
          </Flex>
        </Col>

        <FooterColumn title="Thông tin liên hệ" items={contactItems} />
        <Col xs={24} sm={12} md={8} lg={5}>
          <FooterTitle>Liên kết nhanh</FooterTitle>
          <Space orientation="vertical" size="small">
            {quickLinks.map((item) =>
              item.to ? (
                <Link key={item.label} to={item.to} className="main-footer__link">
                  {item.label}
                </Link>
              ) : (
                <a key={item.label} href={item.href} className="main-footer__link">
                  {item.label}
                </a>
              ),
            )}
          </Space>
        </Col>
        <FooterColumn title="Hạng phòng" items={roomTypes} />
      </Row>
    </Layout.Footer>
  );
}

interface FooterColumnProps {
  title: string;
  items: string[];
}

function FooterColumn({ title, items }: FooterColumnProps) {
  return (
    <Col xs={24} sm={12} md={8} lg={5}>
      <FooterTitle>{title}</FooterTitle>
      <Space orientation="vertical" size="small">
        {items.map((item) => (
          <Typography.Text key={item} className="main-footer__text">
            {item}
          </Typography.Text>
        ))}
      </Space>
    </Col>
  );
}

function FooterTitle({ children }: { children: string }) {
  return (
    <Typography.Title level={5} className="main-footer__title">
      {children}
    </Typography.Title>
  );
}
