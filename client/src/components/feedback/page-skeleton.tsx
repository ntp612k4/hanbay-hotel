export function PageSkeleton() {
  return (
    <div
      role="status"
      aria-label="Đang tải trang"
      style={{ minHeight: '100vh', background: 'var(--color-surface)' }}
    />
  );
}
