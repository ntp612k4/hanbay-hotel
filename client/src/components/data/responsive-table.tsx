import { Table, type TableProps } from 'antd';
import { useEffect, useRef, useState } from 'react';
import './responsive-table.css';

/** Keep wide tables inside their container and unpin columns in narrow panels. */
export function ResponsiveTable<T extends object>({
  columns,
  scroll,
  size = 'middle',
  ...props
}: TableProps<T>) {
  const container = useRef<HTMLDivElement>(null);
  const [layout, setLayout] = useState({ width: 0, overflowing: false });

  useEffect(() => {
    const element = container.current;
    if (!element) return;
    const measure = () => {
      const content = element.querySelector<HTMLElement>('.ant-table-content, .ant-table-body');
      const width = element.clientWidth;
      const overflowing = Boolean(content && content.scrollWidth > content.clientWidth + 1);
      setLayout((previous) =>
        previous.width === width && previous.overflowing === overflowing
          ? previous
          : { width, overflowing },
      );
    };
    measure();
    if (typeof ResizeObserver === 'undefined') return;
    const observer = new ResizeObserver(measure);
    observer.observe(element);
    element.querySelectorAll('table').forEach((table) => observer.observe(table));
    return () => observer.disconnect();
  }, []);

  const visibleColumns =
    layout.width >= 1000 ? columns : columns?.map((column) => ({ ...column, fixed: undefined }));

  return (
    <div ref={container} className="responsive-table">
      <Table<T>
        {...props}
        size={size}
        columns={visibleColumns}
        scroll={{ ...scroll, x: scroll?.x ?? 'max-content' }}
      />
      {layout.overflowing && (
        <p className="responsive-table__hint">Cuộn ngang trong bảng để xem đầy đủ thông tin.</p>
      )}
    </div>
  );
}
