import { ResponsiveTable as Table } from '../../../components/data/responsive-table';
import { EditOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import type { TableProps } from 'antd';
import {
  App,
  Alert,
  Button,
  Card,
  Empty,
  Flex,
  Input,
  Pagination,
  Select,
  Space,
  Tag,
  Typography,
  type FormInstance,
} from 'antd';
import dayjs from 'dayjs';
import { useEffect, useMemo, useState } from 'react';

import type { ApiClientError } from '../../../types/api';
import { AddOnFormModal } from '../components/addon-form-modal';
import { useAddOnMutations, useManagedAddOns } from '../hooks/use-addon-management';
import type { AddOnFormValues, AddOnManagementItem } from '../types/addon-management';
import { getAddOnErrorMessage, getAddOnFieldErrors } from '../utils/addon-error-mapper';
import './addon-management-page.css';

const PAGE_SIZE = 10;
const money = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' });

export function AddOnManagementPage() {
  const { message, modal } = App.useApp();
  const [page, setPage] = useState(0);
  const [searchInput, setSearchInput] = useState('');
  const [search, setSearch] = useState('');
  const [active, setActive] = useState<boolean | undefined>();
  const [sort, setSort] = useState('name,asc');
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<AddOnManagementItem | null>(null);
  const params = useMemo(
    () => ({ page, size: PAGE_SIZE, search, active, sort }),
    [active, page, search, sort],
  );
  const addOnsQuery = useManagedAddOns(params);
  const { createMutation, updateMutation, statusMutation } = useAddOnMutations();

  useEffect(() => {
    const totalPages = addOnsQuery.data?.totalPages;
    let nextPage: number | undefined;

    if (totalPages !== undefined && totalPages > 0 && page >= totalPages) {
      nextPage = totalPages - 1;
    } else if (totalPages === 0 && page !== 0) {
      nextPage = 0;
    }

    if (nextPage === undefined) return;

    const timeoutId = window.setTimeout(() => setPage(nextPage), 0);
    return () => window.clearTimeout(timeoutId);
  }, [addOnsQuery.data?.totalPages, page]);

  const closeForm = () => {
    setFormOpen(false);
    setEditing(null);
  };

  const submitForm = (values: AddOnFormValues, form: FormInstance<AddOnFormValues>) => {
    const description = values.description?.trim() || null;
    const onError = (error: unknown) => {
      const fieldErrors = getAddOnFieldErrors(error);
      if (fieldErrors.length) form.setFields(fieldErrors as never);
      void message.error(getAddOnErrorMessage(error));
    };
    if (editing) {
      updateMutation.mutate(
        {
          id: editing.id,
          payload: { name: values.name.trim(), description, price: values.price },
        },
        {
          onSuccess: () => {
            void message.success('Cập nhật dịch vụ bổ sung thành công.');
            closeForm();
          },
          onError,
        },
      );
      return;
    }
    createMutation.mutate(
      {
        code: values.code.trim(),
        name: values.name.trim(),
        description,
        price: values.price,
      },
      {
        onSuccess: () => {
          void message.success('Tạo dịch vụ bổ sung thành công.');
          setPage(0);
          closeForm();
        },
        onError,
      },
    );
  };

  const confirmStatus = (item: AddOnManagementItem) => {
    const nextActive = !item.active;
    modal.confirm({
      title: nextActive ? 'Kích hoạt dịch vụ?' : 'Ngừng cung cấp dịch vụ?',
      content: nextActive
        ? `${item.name} sẽ xuất hiện trở lại trong quy trình đặt phòng.`
        : `${item.name} sẽ không thể được chọn cho lượt đặt phòng mới. Các lượt đặt phòng cũ không bị thay đổi.`,
      okText: nextActive ? 'Kích hoạt' : 'Ngừng cung cấp',
      okButtonProps: { danger: !nextActive },
      cancelText: 'Hủy',
      onOk: () =>
        statusMutation.mutateAsync({ id: item.id, active: nextActive }).then(
          () => void message.success('Cập nhật trạng thái dịch vụ thành công.'),
          (error: unknown) => void message.error(getAddOnErrorMessage(error)),
        ),
    });
  };

  const columns: TableProps<AddOnManagementItem>['columns'] = [
    { title: 'Mã', dataIndex: 'code', sorter: true, width: 190 },
    { title: 'Tên dịch vụ', dataIndex: 'name', sorter: true, width: 220 },
    {
      title: 'Mô tả',
      dataIndex: 'description',
      ellipsis: true,
      render: (value: string | null) => value || '—',
    },
    {
      title: 'Giá',
      dataIndex: 'price',
      sorter: true,
      align: 'right',
      width: 160,
      render: (value: number) => money.format(value),
    },
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
            onClick={() => confirmStatus(item)}
          >
            {item.active ? 'Ngừng' : 'Kích hoạt'}
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <Space orientation="vertical" size="large" className="addon-management-page">
      <Flex justify="space-between" align="flex-start" gap="middle" wrap>
        <div>
          <Typography.Title level={2}>Quản lý dịch vụ bổ sung</Typography.Title>
          <Typography.Text type="secondary">
            Quản lý các dịch vụ khách có thể chọn khi đặt phòng.
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
          Thêm dịch vụ
        </Button>
      </Flex>

      <Card>
        <Flex gap="middle" wrap className="addon-management-toolbar">
          <Input.Search
            aria-label="Tìm dịch vụ bổ sung"
            allowClear
            placeholder="Tìm theo mã hoặc tên dịch vụ"
            maxLength={150}
            value={searchInput}
            onChange={(event) => setSearchInput(event.target.value)}
            onSearch={(value) => {
              setSearch(value.trim());
              setPage(0);
            }}
          />
          <Select
            aria-label="Lọc trạng thái dịch vụ"
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
          <Button icon={<ReloadOutlined />} onClick={() => void addOnsQuery.refetch()}>
            Tải lại
          </Button>
        </Flex>

        {addOnsQuery.isError ? (
          <Alert
            type="error"
            showIcon
            title="Không tải được danh mục dịch vụ"
            description={getAddOnErrorMessage(addOnsQuery.error as ApiClientError)}
            action={
              <Button size="small" onClick={() => void addOnsQuery.refetch()}>
                Thử lại
              </Button>
            }
          />
        ) : (
          <>
            <Table<AddOnManagementItem>
              rowKey="id"
              loading={addOnsQuery.isLoading}
              dataSource={addOnsQuery.data?.content ?? []}
              columns={columns}
              pagination={false}
              scroll={{ x: 1250 }}
              locale={{
                emptyText: (
                  <Empty
                    description={
                      search ? 'Không tìm thấy dịch vụ phù hợp' : 'Chưa có dịch vụ bổ sung'
                    }
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
            {(addOnsQuery.data?.totalElements ?? 0) > 0 && (
              <Pagination
                align="center"
                current={(addOnsQuery.data?.page ?? 0) + 1}
                pageSize={PAGE_SIZE}
                total={addOnsQuery.data?.totalElements ?? 0}
                showSizeChanger={false}
                showTotal={(total) => `${total} dịch vụ`}
                onChange={(nextPage) => setPage(nextPage - 1)}
              />
            )}
          </>
        )}
      </Card>

      <AddOnFormModal
        open={formOpen}
        editing={editing}
        loading={createMutation.isPending || updateMutation.isPending}
        onCancel={closeForm}
        onSubmit={submitForm}
      />
    </Space>
  );
}
