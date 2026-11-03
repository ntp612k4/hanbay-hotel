import { Spin } from 'antd';

export function LoadingPage() {
  return (
    <div role="status" aria-label="Đang tải trang" aria-live="polite">
      <Spin fullscreen size="large" description="Đang tải..." />
    </div>
  );
}
