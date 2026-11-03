import { MaintenanceCostPanel } from '../components/maintenance-cost-panel';
import { PlusOutlined } from '@ant-design/icons';
import {
  App,
  Alert,
  Button,
  Card,
  Descriptions,
  Drawer,
  Empty,
  Flex,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Spin,
  Table,
  Tag,
  Typography,
  type TableColumnsType,
} from 'antd';
import { useState } from 'react';
import {
  useMaintenanceDetail,
  useMaintenanceMutations,
  useMaintenanceRequests,
  usePhysicalRooms,
} from '../hooks/use-maintenance';
import type { CreateMaintenanceRequest, MaintenanceItem } from '../types/maintenance';
import {
  formatMaintenanceTime,
  maintenanceErrorMessage,
  maintenanceStatusLabels,
  nextMaintenanceStatus,
} from './maintenance-presentation';

export function MaintenancePage() {
  const { message } = App.useApp();
  const [page, setPage] = useState(0);
  const [createOpen, setCreateOpen] = useState(false);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [form] = Form.useForm<CreateMaintenanceRequest>();
  const requestsQuery = useMaintenanceRequests(page);
  const roomsQuery = usePhysicalRooms(createOpen);
  const detailQuery = useMaintenanceDetail(selectedId);
  const { create: createMutation, status: statusMutation } = useMaintenanceMutations();
  const availableRooms = (roomsQuery.data ?? []).filter((room) => room.status === 'AVAILABLE');

  const closeCreate = () => {
    if (createMutation.isPending) return;
    setCreateOpen(false);
    form.resetFields();
  };
  const submit = async () => {
    if (createMutation.isPending) return;
    let values: CreateMaintenanceRequest;
    try {
      values = await form.validateFields();
    } catch {
      return;
    }
    createMutation.mutate(
      {
        ...values,
        title: values.title.trim(),
        description: values.description?.trim() || undefined,
      },
      {
        onSuccess: () => {
          void message.success('Tạo yêu cầu bảo trì thành công.');
          form.resetFields();
          setCreateOpen(false);
          setPage(0);
        },
        onError: (error) => void message.error(maintenanceErrorMessage(error)),
      },
    );
  };
  const columns: TableColumnsType<MaintenanceItem> = [
    { title: 'Phòng', dataIndex: 'roomNumber', width: 100 },
    { title: 'Lý do', dataIndex: 'title', ellipsis: true },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      width: 140,
      render: (status: MaintenanceItem['status']) => <Tag>{maintenanceStatusLabels[status]}</Tag>,
    },
    { title: 'Ngày tạo', dataIndex: 'createdAt', width: 160, render: formatMaintenanceTime },
    {
      title: 'Thao tác',
      width: 240,
      render: (_, request) => {
        const nextStatus = nextMaintenanceStatus[request.status];
        return (
          <Space>
            <Button onClick={() => setSelectedId(request.id)}>Chi tiết</Button>
            {nextStatus && (
              <Button
                type="primary"
                disabled={statusMutation.isPending}
                loading={statusMutation.isPending && statusMutation.variables?.id === request.id}
                onClick={() =>
                  statusMutation.mutate(
                    { id: request.id, status: nextStatus },
                    {
                      onSuccess: () => void message.success('Cập nhật trạng thái thành công.'),
                      onError: (error) => void message.error(maintenanceErrorMessage(error)),
                    },
                  )
                }
              >
                {request.status === 'PENDING' ? 'Bắt đầu xử lý' : 'Hoàn thành'}
              </Button>
            )}
          </Space>
        );
      },
    },
  ];

  const detail = detailQuery.data;
  return (
    <Flex vertical gap="large">
      <Typography.Title level={2}>Quản lý bảo trì phòng</Typography.Title>
      <Space>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateOpen(true)}>
          Tạo yêu cầu bảo trì
        </Button>
      </Space>
      <Card>
        {requestsQuery.isError ? (
          <Alert
            type="error"
            showIcon
            title={maintenanceErrorMessage(requestsQuery.error)}
            action={<Button onClick={() => void requestsQuery.refetch()}>Thử lại</Button>}
          />
        ) : (
          <Table
            rowKey="id"
            loading={requestsQuery.isLoading || requestsQuery.isFetching}
            columns={columns}
            dataSource={requestsQuery.data?.content ?? []}
            scroll={{ x: 800 }}
            pagination={{
              current: page + 1,
              total: requestsQuery.data?.totalElements ?? 0,
              pageSize: 20,
              showSizeChanger: false,
              onChange: (nextPage) => setPage(nextPage - 1),
              showTotal: (total) => `Tổng ${total} yêu cầu`,
            }}
            locale={{ emptyText: <Empty description="Chưa có yêu cầu bảo trì" /> }}
          />
        )}
      </Card>
      <Modal
        open={createOpen}
        title="Tạo yêu cầu bảo trì"
        okText="Tạo yêu cầu"
        cancelText="Hủy"
        onCancel={closeCreate}
        onOk={() => void submit()}
        confirmLoading={createMutation.isPending}
        closable={!createMutation.isPending}
        mask={{ closable: !createMutation.isPending }}
        keyboard={!createMutation.isPending}
        cancelButtonProps={{ disabled: createMutation.isPending }}
        okButtonProps={{
          disabled: roomsQuery.isLoading || roomsQuery.isError || !availableRooms.length,
        }}
      >
        <Form form={form} layout="vertical" disabled={createMutation.isPending}>
          {roomsQuery.isError && (
            <Alert
              type="error"
              showIcon
              title={maintenanceErrorMessage(roomsQuery.error)}
              action={<Button onClick={() => void roomsQuery.refetch()}>Tải lại phòng</Button>}
            />
          )}
          {!roomsQuery.isLoading && !roomsQuery.isError && !availableRooms.length && (
            <Alert type="info" showIcon title="Hiện không có phòng trống để bảo trì." />
          )}
          <Form.Item
            name="roomId"
            label="Phòng"
            rules={[
              { required: true, message: 'Vui lòng chọn phòng' },
              {
                validator: async (_, value: string | undefined) => {
                  if (value && !availableRooms.some((room) => room.roomId === value))
                    throw new Error('Phòng không còn trống để bảo trì. Vui lòng chọn phòng khác.');
                },
              },
            ]}
            extra="Chỉ phòng trống mới có thể bắt đầu bảo trì. Phòng sẽ ngừng nhận khách khi tạo yêu cầu."
          >
            <Select
              loading={roomsQuery.isLoading}
              showSearch={{ optionFilterProp: 'label' }}
              disabled={roomsQuery.isLoading || roomsQuery.isError || !availableRooms.length}
              placeholder="Chọn phòng cần bảo trì"
              notFoundContent="Không tìm thấy phòng phù hợp"
              options={availableRooms.map((room) => ({
                value: room.roomId,
                label: `Phòng ${room.roomNumber} - Tầng ${room.floor}`,
              }))}
            />
          </Form.Item>
          <Form.Item
            name="title"
            label="Tiêu đề"
            rules={[
              { required: true, whitespace: true, message: 'Vui lòng nhập tiêu đề bảo trì' },
              { max: 150, message: 'Tiêu đề không được vượt quá 150 ký tự' },
            ]}
          >
            <Input maxLength={150} />
          </Form.Item>
          <Form.Item
            name="description"
            label="Mô tả"
            rules={[{ max: 2000, message: 'Mô tả không được vượt quá 2000 ký tự' }]}
          >
            <Input.TextArea maxLength={2000} showCount autoSize={{ minRows: 3, maxRows: 6 }} />
          </Form.Item>
        </Form>
      </Modal>
      <Drawer
        open={selectedId !== null}
        onClose={() => setSelectedId(null)}
        title="Chi tiết yêu cầu"
      >
        {detailQuery.isLoading ? (
          <Spin description="Đang tải..." />
        ) : detailQuery.isError ? (
          <Alert
            type="error"
            showIcon
            title={maintenanceErrorMessage(detailQuery.error)}
            action={<Button onClick={() => void detailQuery.refetch()}>Thử lại</Button>}
          />
        ) : (
          detail && (
            <>
              <Descriptions
                column={1}
                items={[
                  { key: 'room', label: 'Phòng', children: detail.roomNumber },
                  { key: 'title', label: 'Lý do', children: detail.title },
                  { key: 'description', label: 'Mô tả', children: detail.description || '—' },
                  {
                    key: 'status',
                    label: 'Trạng thái',
                    children: <Tag>{maintenanceStatusLabels[detail.status]}</Tag>,
                  },
                  { key: 'author', label: 'Người tạo', children: detail.createdByName || '—' },
                  {
                    key: 'created',
                    label: 'Ngày tạo',
                    children: formatMaintenanceTime(detail.createdAt),
                  },
                  {
                    key: 'started',
                    label: 'Bắt đầu xử lý',
                    children: formatMaintenanceTime(detail.startedAt),
                  },
                  {
                    key: 'completed',
                    label: 'Hoàn thành',
                    children: formatMaintenanceTime(detail.completedAt),
                  },
                ]}
              />
              <MaintenanceCostPanel key={detail.id} request={detail} />
            </>
          )
        )}
      </Drawer>
    </Flex>
  );
}
