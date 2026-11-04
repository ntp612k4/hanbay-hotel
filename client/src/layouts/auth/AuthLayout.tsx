import type { ReactNode } from 'react';
import { Card, Flex, Image, Typography } from 'antd';
import hotelBg from '../../assets/register.jpg';
import './auth-layout.css';

interface AuthLayoutProps {
  title: string;
  subtitle?: string;
  children: ReactNode;
}

export function AuthLayout({ title, subtitle, children }: AuthLayoutProps) {
  return (
    <main className="auth-page">
      <div
        className="auth-page__visual"
        aria-hidden="true"
        style={{ backgroundImage: `url(${hotelBg})` }}
      />
      <section className="auth-page__panel" aria-label={title}>
        <Card className="auth-page__card">
          <Flex vertical align="center" gap={8} className="auth-page__intro">
            <Image preview={false} width={56} src="/logo.png" alt="Khách sạn HANBAY" />
            <Typography.Title level={2}>{title}</Typography.Title>
            {subtitle && <Typography.Text type="secondary">{subtitle}</Typography.Text>}
          </Flex>
          {children}
        </Card>
      </section>
    </main>
  );
}
