import { CalendarOutlined, FilterOutlined, UndoOutlined } from '@ant-design/icons';
import { Alert, Button, Card, Form, Input, Space } from 'antd';
import dayjs from 'dayjs';
import { useState } from 'react';
import type { FormEvent } from 'react';

interface DashboardFiltersProps {
  from: string;
  to: string;
  onApply: (from: string, to: string) => void;
  onReset: () => void;
}

const MAX_REPORT_DAYS = 366;

export function DashboardFilters({ from, to, onApply, onReset }: DashboardFiltersProps) {
  const [draftFrom, setDraftFrom] = useState(from);
  const [draftTo, setDraftTo] = useState(dayjs(to).subtract(1, 'day').format('YYYY-MM-DD'));
  const [error, setError] = useState<string>();

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const start = dayjs(draftFrom);
    const inclusiveEnd = dayjs(draftTo);

    if (!draftFrom || !draftTo || !start.isValid() || !inclusiveEnd.isValid()) {
      setError('Vui lòng chọn đầy đủ ngày bắt đầu và ngày kết thúc.');
      return;
    }
    if (inclusiveEnd.isBefore(start, 'day')) {
      setError('Ngày kết thúc không được trước ngày bắt đầu.');
      return;
    }

    const exclusiveEnd = inclusiveEnd.add(1, 'day');
    if (exclusiveEnd.diff(start, 'day') > MAX_REPORT_DAYS) {
      setError(`Khoảng báo cáo không được vượt quá ${MAX_REPORT_DAYS} ngày.`);
      return;
    }

    setError(undefined);
    onApply(start.format('YYYY-MM-DD'), exclusiveEnd.format('YYYY-MM-DD'));
  };

  const reset = () => {
    setDraftFrom(from);
    setDraftTo(dayjs(to).subtract(1, 'day').format('YYYY-MM-DD'));
    setError(undefined);
    onReset();
  };

  return (
    <Card
      title={
        <Space>
          <FilterOutlined />
          Lọc theo khoảng ngày
        </Space>
      }
    >
      <Form component="form" layout="vertical" onSubmitCapture={submit}>
        <Space wrap align="end" size="middle">
          <Form.Item label="Từ ngày" required>
            <Input
              type="date"
              value={draftFrom}
              aria-label="Từ ngày"
              onChange={(event) => {
                setDraftFrom(event.target.value);
                setError(undefined);
              }}
            />
          </Form.Item>
          <Form.Item label="Đến ngày" required>
            <Input
              type="date"
              value={draftTo}
              aria-label="Đến ngày"
              onChange={(event) => {
                setDraftTo(event.target.value);
                setError(undefined);
              }}
            />
          </Form.Item>
          <Space className="manager-dashboard__filter-actions" wrap>
            <Button
              htmlType="submit"
              type="primary"
              icon={<CalendarOutlined />}
              aria-label="Áp dụng"
            >
              Áp dụng
            </Button>
            <Button
              htmlType="button"
              icon={<UndoOutlined />}
              aria-label="Tháng hiện tại"
              onClick={reset}
            >
              Tháng hiện tại
            </Button>
          </Space>
        </Space>
        {error && <Alert type="error" showIcon message={error} />}
      </Form>
    </Card>
  );
}
