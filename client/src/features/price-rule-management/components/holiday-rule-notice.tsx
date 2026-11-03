import { Alert, Button, Space } from 'antd';
import { useActiveHolidays } from '../hooks/use-holiday-calendar';

export function HolidayRuleNotice({
  startDate,
  endDate,
  onManage,
}: {
  startDate?: string;
  endDate?: string;
  onManage: () => void;
}) {
  const holidays = useActiveHolidays(true);
  const matches = holidays.data?.filter(
    (item) => startDate && endDate && item.startDate <= endDate && item.endDate >= startDate,
  );
  const title = holidays.isError
    ? 'Không kiểm tra được lịch ngày lễ. Vui lòng thử lại trước khi áp dụng quy tắc.'
    : holidays.isPending
      ? 'Đang kiểm tra lịch ngày lễ…'
      : !startDate || !endDate
        ? 'Chọn khoảng ngày để kiểm tra lịch lễ phù hợp.'
        : matches?.length
          ? 'Quy tắc chỉ áp dụng vào các ngày giao với lịch lễ đang hoạt động.'
          : 'Chưa có lịch lễ đang hoạt động trong khoảng này. Quy tắc sẽ chưa được áp dụng khi tính giá.';
  return (
    <Alert
      showIcon
      type={holidays.isError ? 'error' : matches?.length ? 'info' : 'warning'}
      title={title}
      description={matches?.map((item) => item.name).join(', ')}
      action={
        <Space>
          <Button onClick={onManage}>Quản lý lịch ngày lễ</Button>
          {holidays.isError && <Button onClick={() => void holidays.refetch()}>Thử lại</Button>}
        </Space>
      }
    />
  );
}
