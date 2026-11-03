import type { ReactNode } from 'react';
import { Card, Statistic, Typography } from 'antd';

const { Text } = Typography;

interface MetricCardProps {
  title: string;
  value: number;
  formatter: (value: number) => string;
  icon: ReactNode;
  tone: 'blue' | 'green' | 'amber' | 'violet';
  caption: string;
}

export function MetricCard({ title, value, formatter, icon, tone, caption }: MetricCardProps) {
  return (
    <Card className={`manager-dashboard__metric manager-dashboard__metric--${tone}`}>
      <div className="manager-dashboard__metric-heading">
        <Text type="secondary">{title}</Text>
        <span className="manager-dashboard__metric-icon" aria-hidden="true">
          {icon}
        </span>
      </div>
      <Statistic value={value} formatter={() => formatter(value)} />
      <Text type="secondary" className="manager-dashboard__metric-caption">
        {caption}
      </Text>
    </Card>
  );
}
