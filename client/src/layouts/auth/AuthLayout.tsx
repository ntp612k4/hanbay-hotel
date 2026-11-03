import type { ReactNode } from 'react';
import { Card, Col, Flex, Grid, Image, Row, theme, Typography } from 'antd';

import hotelBg from '../../assets/register.jpg';

interface AuthLayoutProps {
  title: string;
  subtitle?: string;
  children: ReactNode;
}

export function AuthLayout({ title, subtitle, children }: AuthLayoutProps) {
  const { token } = theme.useToken();
  const screens = Grid.useBreakpoint();
  return (
    <main style={{ minHeight: '100vh', background: token.colorBgLayout }}>
      <Row style={{ minHeight: '100vh' }}>
        <Col xs={0} lg={13} xl={15}>
          <Image
            preview={false}
            src={hotelBg}
            alt="Sảnh khách sạn HANBAY"
            width="100%"
            height="100%"
            style={{ display: 'block', objectFit: 'cover' }}
          />
        </Col>
        <Col xs={24} lg={11} xl={9}>
          <Flex
            align="center"
            justify="center"
            style={{ minHeight: '100%', padding: screens.sm ? 32 : 16 }}
          >
            <Card style={{ width: '100%', maxWidth: 560 }}>
              <Flex
                vertical
                align="center"
                gap={8}
                style={{ marginBottom: 24, textAlign: 'center' }}
              >
                <Image preview={false} width={72} src="/logo.png" alt="Khách sạn HANBAY" />
                <Typography.Title level={2} style={{ margin: 0 }}>
                  {title}
                </Typography.Title>
                {subtitle && <Typography.Text type="secondary">{subtitle}</Typography.Text>}
              </Flex>
              {children}
            </Card>
          </Flex>
        </Col>
      </Row>
    </main>
  );
}
