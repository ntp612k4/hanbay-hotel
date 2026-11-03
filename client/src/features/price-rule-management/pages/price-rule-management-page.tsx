import { EditOutlined, PlusOutlined, ReloadOutlined, ThunderboltOutlined } from '@ant-design/icons';
import type { TableProps } from 'antd';
import {
  Alert,
  App,
  Button,
  Card,
  Empty,
  Flex,
  Input,
  Pagination,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  type FormInstance,
} from 'antd';
import dayjs from 'dayjs';
import { useEffect, useMemo, useState } from 'react';

import { useRoomTypeCatalog } from '../../room/api/room-type-query';
import type { ApiClientError } from '../../../types/api';
import { HolidayCalendarManager } from '../components/holiday-calendar-manager';
import { PriceRuleFormModal } from '../components/price-rule-form-modal';
import { useManagedPriceRules, usePriceRuleMutations } from '../hooks/use-price-rule-management';
import type {
  AdjustmentDirection,
  AdjustmentType,
  PriceRuleFormValues,
  PriceRuleManagementItem,
  RuleType,
} from '../types/price-rule-management';
import {
  getPriceRuleErrorMessage,
  getPriceRuleFieldErrors,
} from '../utils/price-rule-error-mapper';
import './price-rule-management-page.css';

const PAGE_SIZE = 10;
const money = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' });

const ruleTypeLabels: Record<RuleType, string> = {
  WEEKEND: 'Cuối tuần',
  HOLIDAY: 'Ngày lễ',
  SEASONAL: 'Mùa cao/thấp điểm',
  PROMOTION: 'Khuyến mãi công khai',
};

const adjustmentTypeLabels: Record<AdjustmentType, string> = {
  FIXED: 'Cố định',
  PERCENTAGE: 'Phần trăm',
};

const adjustmentDirectionLabels: Record<AdjustmentDirection, string> = {
  INCREASE: 'Tăng giá',
  DISCOUNT: 'Giảm giá',
};

const ruleTypeOptions = [
  { value: 'all', label: 'Tất cả loại quy tắc' },
  { value: 'WEEKEND', label: 'Cuối tuần' },
  { value: 'HOLIDAY', label: 'Ngày lễ' },
  { value: 'SEASONAL', label: 'Mùa cao/thấp điểm' },
  { value: 'PROMOTION', label: 'Khuyến mãi công khai' },
];

function formatAdjustment(item: PriceRuleManagementItem) {
  const value =
    item.adjustmentType === 'PERCENTAGE'
      ? `${item.adjustmentValue}%`
      : money.format(item.adjustmentValue);
  return `${item.adjustmentDirection === 'DISCOUNT' ? '-' : '+'}${value}`;
}

export function PriceRuleManagementPage() {
  const { message, modal } = App.useApp();
  const [calendarOpen, setCalendarOpen] = useState(false);
  const [page, setPage] = useState(0);
  const [searchInput, setSearchInput] = useState('');
  const [search, setSearch] = useState('');
  const [roomTypeId, setRoomTypeId] = useState<string | undefined>();
  const [ruleType, setRuleType] = useState<RuleType | undefined>();
  const [active, setActive] = useState<boolean | undefined>();
  const [sort, setSort] = useState('startDate,desc');
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<PriceRuleManagementItem | null>(null);

  const params = useMemo(
    () => ({ page, size: PAGE_SIZE, search, roomTypeId, ruleType, active, sort }),
    [active, page, roomTypeId, ruleType, search, sort],
  );
  const priceRulesQuery = useManagedPriceRules(params);
  const roomTypesQuery = useRoomTypeCatalog({
    page: 0,
    size: 100,
    sortBy: 'name',
    direction: 'asc',
  });
  const { createMutation, updateMutation, statusMutation } = usePriceRuleMutations();

  useEffect(() => {
    const totalPages = priceRulesQuery.data?.totalPages;
    let nextPage: number | undefined;

    if (totalPages !== undefined && totalPages > 0 && page >= totalPages) {
      nextPage = totalPages - 1;
    } else if (totalPages === 0 && page !== 0) {
      nextPage = 0;
    }

    if (nextPage === undefined) return;

    const timeoutId = window.setTimeout(() => setPage(nextPage), 0);
    return () => window.clearTimeout(timeoutId);
  }, [page, priceRulesQuery.data?.totalPages]);

  const closeForm = () => {
    setFormOpen(false);
    setEditing(null);
  };

  const submitForm = (values: PriceRuleFormValues, form: FormInstance<PriceRuleFormValues>) => {
    const payload = {
      roomTypeId: values.roomTypeId,
      name: values.name.trim(),
      ruleType: values.ruleType,
      adjustmentType: values.adjustmentType,
      adjustmentValue: values.adjustmentValue,
      adjustmentDirection: values.adjustmentDirection,
      startDate: values.startDate.format('YYYY-MM-DD'),
      endDate: values.endDate.format('YYYY-MM-DD'),
      priority: values.priority,
    };
    const onError = (error: unknown) => {
      const fieldErrors = getPriceRuleFieldErrors(error);
      if (fieldErrors.length) form.setFields(fieldErrors as never);
      void message.error(getPriceRuleErrorMessage(error));
    };

    if (editing) {
      updateMutation.mutate(
        { id: editing.id, payload },
        {
          onSuccess: () => {
            void message.success('Cập nhật quy tắc giá thành công.');
            closeForm();
          },
          onError,
        },
      );
      return;
    }

    createMutation.mutate(payload, {
      onSuccess: () => {
        void message.success('Tạo quy tắc giá thành công.');
        setPage(0);
        closeForm();
      },
      onError,
    });
  };

  const confirmStatus = (item: PriceRuleManagementItem) => {
    const nextActive = !item.active;
    modal.confirm({
      title: nextActive ? 'Kích hoạt quy tắc giá?' : 'Ngừng kích hoạt quy tắc giá?',
      content: nextActive
        ? `${item.name} sẽ được áp dụng cho các lần tính giá mới.`
        : `${item.name} sẽ không được áp dụng cho các lần tính giá mới. Booking đã tạo không bị thay đổi.`,
      okText: nextActive ? 'Kích hoạt' : 'Ngừng kích hoạt',
      okButtonProps: { danger: !nextActive },
      cancelText: 'Hủy',
      onOk: () =>
        statusMutation.mutateAsync({ id: item.id, active: nextActive }).then(
          () => void message.success('Cập nhật trạng thái quy tắc giá thành công.'),
          (error: unknown) => void message.error(getPriceRuleErrorMessage(error)),
        ),
    });
  };

  const columns: TableProps<PriceRuleManagementItem>['columns'] = [
    { title: 'Tên quy tắc', dataIndex: 'name', sorter: true, width: 230 },
    {
      title: 'Hạng phòng',
      dataIndex: ['roomType', 'name'],
      width: 190,
      render: (_, item) => item.roomType.name,
    },
    {
      title: 'Loại',
      dataIndex: 'ruleType',
      sorter: true,
      width: 230,
      render: (value: RuleType) => (
        <Tag color={value === 'PROMOTION' ? 'blue' : 'orange'}>{ruleTypeLabels[value]}</Tag>
      ),
    },
    {
      title: 'Kiểu',
      dataIndex: 'adjustmentType',
      width: 130,
      render: (value: AdjustmentType) => adjustmentTypeLabels[value],
    },
    {
      title: 'Hướng',
      dataIndex: 'adjustmentDirection',
      width: 110,
      render: (value: AdjustmentDirection) => (
        <Tag color={value === 'DISCOUNT' ? 'green' : 'orange'}>
          {adjustmentDirectionLabels[value]}
        </Tag>
      ),
    },
    {
      title: 'Giá trị',
      dataIndex: 'adjustmentValue',
      sorter: true,
      align: 'right',
      width: 150,
      render: (_, item) => formatAdjustment(item),
    },
    {
      title: 'Bắt đầu',
      dataIndex: 'startDate',
      sorter: true,
      width: 130,
      render: (value: string) => dayjs(value).format('DD/MM/YYYY'),
    },
    {
      title: 'Kết thúc',
      dataIndex: 'endDate',
      sorter: true,
      width: 130,
      render: (value: string) => dayjs(value).format('DD/MM/YYYY'),
    },
    { title: 'Ưu tiên', dataIndex: 'priority', sorter: true, align: 'right', width: 110 },
    {
      title: 'Trạng thái',
      dataIndex: 'active',
      sorter: true,
      width: 150,
      render: (value: boolean) =>
        value ? <Tag color="green">Đang hoạt động</Tag> : <Tag>Ngừng hoạt động</Tag>,
    },
    {
      title: 'Cập nhật',
      dataIndex: 'updatedAt',
      width: 150,
      render: (value: string | null, item) =>
        dayjs(value ?? item.createdAt).format('DD/MM/YYYY HH:mm'),
    },
    {
      title: 'Thao tác',
      key: 'actions',
      fixed: 'right',
      width: 220,
      render: (_, item) => (
        <Space>
          <Button
            icon={<EditOutlined />}
            onClick={() => {
              setEditing(item);
              setFormOpen(true);
            }}
          >
            Sửa
          </Button>
          <Button
            danger={item.active}
            loading={statusMutation.isPending && statusMutation.variables?.id === item.id}
            icon={<ThunderboltOutlined />}
            onClick={() => confirmStatus(item)}
          >
            {item.active ? 'Ngừng' : 'Kích hoạt'}
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <Space orientation="vertical" size="large" className="price-rule-management-page">
      <Flex justify="space-between" align="flex-start" gap="middle" wrap>
        <div>
          <Typography.Title level={2}>Quản lý quy tắc giá</Typography.Title>
          <Typography.Text type="secondary">
            Cấu hình phụ thu và khuyến mãi cho từng hạng phòng theo thời gian áp dụng.
          </Typography.Text>
        </div>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={() => {
            setEditing(null);
            setFormOpen(true);
          }}
        >
          Thêm quy tắc
        </Button>
      </Flex>

      <Button onClick={() => setCalendarOpen(true)}>Quản lý lịch ngày lễ</Button>
      {calendarOpen && <HolidayCalendarManager onClose={() => setCalendarOpen(false)} />}
      <Card>
        <Flex gap="middle" wrap className="price-rule-management-toolbar">
          <Input.Search
            aria-label="Tìm quy tắc giá"
            allowClear
            placeholder="Tìm theo tên quy tắc"
            maxLength={150}
            value={searchInput}
            onChange={(event) => setSearchInput(event.target.value)}
            onSearch={(value) => {
              setSearch(value.trim());
              setPage(0);
            }}
          />
          <Select
            aria-label="Lọc hạng phòng"
            allowClear
            placeholder="Tất cả hạng phòng"
            loading={roomTypesQuery.isLoading}
            value={roomTypeId}
            options={(roomTypesQuery.data?.content ?? []).map((roomType) => ({
              value: roomType.id,
              label: roomType.name,
            }))}
            onChange={(value) => {
              setRoomTypeId(value);
              setPage(0);
            }}
          />
          <Select
            aria-label="Lọc loại quy tắc"
            value={ruleType ?? 'all'}
            options={ruleTypeOptions}
            onChange={(value) => {
              setRuleType(value === 'all' ? undefined : (value as RuleType));
              setPage(0);
            }}
          />
          <Select
            aria-label="Lọc trạng thái quy tắc giá"
            value={active === undefined ? 'all' : String(active)}
            options={[
              { value: 'all', label: 'Tất cả trạng thái' },
              { value: 'true', label: 'Đang hoạt động' },
              { value: 'false', label: 'Ngừng hoạt động' },
            ]}
            onChange={(value) => {
              setActive(value === 'all' ? undefined : value === 'true');
              setPage(0);
            }}
          />
          <Button icon={<ReloadOutlined />} onClick={() => void priceRulesQuery.refetch()}>
            Tải lại
          </Button>
        </Flex>

        {priceRulesQuery.isError ? (
          <Alert
            type="error"
            showIcon
            message="Không tải được danh mục quy tắc giá"
            description={getPriceRuleErrorMessage(priceRulesQuery.error as ApiClientError)}
            action={
              <Button size="small" onClick={() => void priceRulesQuery.refetch()}>
                Thử lại
              </Button>
            }
          />
        ) : (
          <>
            <Table<PriceRuleManagementItem>
              rowKey="id"
              loading={priceRulesQuery.isLoading}
              dataSource={priceRulesQuery.data?.content ?? []}
              columns={columns}
              pagination={false}
              scroll={{ x: 1920 }}
              locale={{
                emptyText: (
                  <Empty
                    description={search ? 'Không tìm thấy quy tắc phù hợp' : 'Chưa có quy tắc giá'}
                  />
                ),
              }}
              onChange={(_, __, sorter) => {
                const selected = Array.isArray(sorter) ? sorter[0] : sorter;
                if (!selected?.field || !selected.order) return;
                setSort(
                  `${String(selected.field)},${selected.order === 'ascend' ? 'asc' : 'desc'}`,
                );
                setPage(0);
              }}
            />
            {(priceRulesQuery.data?.totalElements ?? 0) > 0 && (
              <Pagination
                align="center"
                current={(priceRulesQuery.data?.page ?? 0) + 1}
                pageSize={PAGE_SIZE}
                total={priceRulesQuery.data?.totalElements ?? 0}
                showSizeChanger={false}
                showTotal={(total) => `${total} quy tắc`}
                onChange={(nextPage) => setPage(nextPage - 1)}
              />
            )}
          </>
        )}
      </Card>

      <PriceRuleFormModal
        open={formOpen}
        editing={editing}
        loading={createMutation.isPending || updateMutation.isPending}
        roomTypes={roomTypesQuery.data?.content ?? []}
        roomTypesLoading={roomTypesQuery.isLoading}
        onManageHolidays={() => setCalendarOpen(true)}
        onCancel={closeForm}
        onSubmit={submitForm}
      />
    </Space>
  );
}
