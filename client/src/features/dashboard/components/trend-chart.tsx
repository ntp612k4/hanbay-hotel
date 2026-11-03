import { Typography } from 'antd';
import dayjs from 'dayjs';

const { Text } = Typography;

interface TrendPoint {
  date: string;
  value: number;
}

interface TrendChartProps {
  title: string;
  description: string;
  points: TrendPoint[];
  color: string;
  formatValue: (value: number) => string;
}

const WIDTH = 720;
const HEIGHT = 240;
const PADDING_X = 32;
const PADDING_Y = 24;

export function TrendChart({ title, description, points, color, formatValue }: TrendChartProps) {
  const values = points.map((point) => point.value);
  const minValue = Math.min(...values, 0);
  const maxValue = Math.max(...values, 0);
  const range = Math.max(maxValue - minValue, 1);
  const plotWidth = WIDTH - PADDING_X * 2;
  const plotHeight = HEIGHT - PADDING_Y * 2;
  const valueToY = (value: number) => PADDING_Y + ((maxValue - value) / range) * plotHeight;
  const zeroY = valueToY(0);
  const coordinates = points.map((point, index) => ({
    ...point,
    x:
      PADDING_X + (points.length === 1 ? plotWidth / 2 : (index / (points.length - 1)) * plotWidth),
    y: valueToY(point.value),
  }));
  const path = coordinates.map((point) => `${point.x},${point.y}`).join(' ');
  const first = points.at(0);
  const last = points.at(-1);

  return (
    <section className="manager-dashboard__chart" aria-label={title}>
      <div className="manager-dashboard__chart-heading">
        <div>
          <h3>{title}</h3>
          <Text type="secondary">{description}</Text>
        </div>
        <Text strong>{formatValue(points.at(-1)?.value ?? 0)}</Text>
      </div>
      <svg viewBox={`0 0 ${WIDTH} ${HEIGHT}`} role="img" aria-label={`${title}: ${description}`}>
        <title>{title}</title>
        {[0, 1, 2, 3].map((line) => {
          const y = PADDING_Y + (line / 3) * plotHeight;
          return (
            <line
              key={line}
              x1={PADDING_X}
              x2={WIDTH - PADDING_X}
              y1={y}
              y2={y}
              className="manager-dashboard__grid-line"
            />
          );
        })}
        {minValue < 0 && maxValue > 0 && (
          <line
            x1={PADDING_X}
            x2={WIDTH - PADDING_X}
            y1={zeroY}
            y2={zeroY}
            className="manager-dashboard__zero-line"
          />
        )}
        <polyline
          points={path}
          fill="none"
          stroke={color}
          strokeWidth="4"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
        {coordinates.map((point) => (
          <circle key={point.date} cx={point.x} cy={point.y} r="4" fill={color}>
            <title>{`${dayjs(point.date).format('DD/MM')}: ${formatValue(point.value)}`}</title>
          </circle>
        ))}
      </svg>
      <div className="manager-dashboard__chart-axis" aria-hidden="true">
        <span>{first ? dayjs(first.date).format('DD/MM') : ''}</span>
        <span>{last ? dayjs(last.date).format('DD/MM') : ''}</span>
      </div>
    </section>
  );
}
