import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  App,
  Button,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Select,
  Space,
  Table,
  Tag,
  Typography,
} from 'antd';
import { apiClient } from '../../lib/axios';
import type { ApiResponse, PageResponse } from '../../types/api';
import type { ManagedRoomType } from './room-type-management-page';
interface ManagedRoom {
  id: string;
  roomTypeId: string;
  roomTypeName: string;
  roomNumber: string;
  floor: number;
  status: string;
  note: string;
}
const endpoint = '/management/rooms';
const statusLabels: Record<string, string> = {
  AVAILABLE: 'Sẵn sàng',
  OCCUPIED: 'Có khách',
  DIRTY: 'Cần dọn',
  CLEANING: 'Đang dọn',
  MAINTENANCE: 'Bảo trì',
};
export function RoomManagementPage() {
  const { message } = App.useApp();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [editing, setEditing] = useState<ManagedRoom | null | undefined>();
  const [form] = Form.useForm<ManagedRoom>();
  const query = useQuery({
    queryKey: ['managed-rooms', page],
    queryFn: async () =>
      (
        await apiClient.get<ApiResponse<PageResponse<ManagedRoom>>>(endpoint, {
          params: { page, size: 20 },
        })
      ).data.data,
  });
  const types = useQuery({
    queryKey: ['managed-room-type-options'],
    queryFn: async () => {
      const all: ManagedRoomType[] = [];
      for (let index = 0; ; index++) {
        const response = (
          await apiClient.get<ApiResponse<PageResponse<ManagedRoomType>>>(
            '/management/room-types',
            { params: { page: index, size: 100 } },
          )
        ).data.data;
        all.push(...response.content);
        if (response.last) return all;
      }
    },
  });
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['managed-rooms'] });
  const save = useMutation({
    mutationFn: (values: ManagedRoom) =>
      editing
        ? apiClient.put(`${endpoint}/${editing.id}`, values)
        : apiClient.post(endpoint, values),
    onSuccess: async () => {
      setEditing(undefined);
      await refresh();
      message.success('Đã lưu phòng');
    },
    onError: (error) => message.error(error.message),
  });
  const remove = useMutation({
    mutationFn: (id: string) => apiClient.delete(`${endpoint}/${id}`),
    onSuccess: async () => {
      await refresh();
      message.success('Đã xóa phòng');
    },
    onError: (error) => message.error(error.message),
  });
  const open = (record: ManagedRoom | null) => {
    form.resetFields();
    form.setFieldsValue(record ?? { floor: 1, note: '' });
    setEditing(record);
  };
  const readOnly = !!editing && editing.status !== 'AVAILABLE';
  return (
    <Space orientation="vertical" size="large" style={{ width: '100%' }}>
      <Space style={{ width: '100%', justifyContent: 'space-between' }}>
        <Typography.Title level={2}>Quản lý phòng</Typography.Title>
        <Button type="primary" onClick={() => open(null)}>
          Thêm phòng
        </Button>
      </Space>
      <Alert
        type="info"
        title="Phòng mới có trạng thái sẵn sàng. Trạng thái có khách, dọn phòng và bảo trì được cập nhật qua nghiệp vụ tương ứng."
      />
      {query.isError && (
        <Alert
          type="error"
          title={query.error.message}
          action={<Button onClick={() => query.refetch()}>Thử lại</Button>}
        />
      )}
      <Table
        rowKey="id"
        dataSource={query.data?.content}
        loading={query.isLoading}
        scroll={{ x: 750 }}
        pagination={{
          current: page + 1,
          pageSize: 20,
          total: query.data?.totalElements,
          showSizeChanger: false,
          onChange: (value) => setPage(value - 1),
        }}
        columns={[
          { title: 'Số phòng', dataIndex: 'roomNumber' },
          { title: 'Loại phòng', dataIndex: 'roomTypeName' },
          { title: 'Tầng', dataIndex: 'floor' },
          {
            title: 'Trạng thái',
            dataIndex: 'status',
            render: (value) => (
              <Tag color={value === 'AVAILABLE' ? 'green' : 'orange'}>
                {statusLabels[value] ?? value}
              </Tag>
            ),
          },
          { title: 'Ghi chú', dataIndex: 'note' },
          {
            title: 'Thao tác',
            render: (_, r) => (
              <Space>
                <Button onClick={() => open(r)}>Chi tiết / Sửa</Button>
                <Popconfirm
                  title="Xóa phòng này?"
                  description="Phòng có lịch sử sử dụng hoặc loại phòng còn đặt phòng sẽ không được xóa."
                  onConfirm={() => remove.mutateAsync(r.id)}
                >
                  <Button danger disabled={r.status !== 'AVAILABLE'} loading={remove.isPending}>
                    Xóa
                  </Button>
                </Popconfirm>
              </Space>
            ),
          },
        ]}
      />
      <Modal
        title={editing ? 'Chi tiết phòng' : 'Thêm phòng'}
        open={editing !== undefined}
        onCancel={() => setEditing(undefined)}
        onOk={() => form.submit()}
        confirmLoading={save.isPending}
        okButtonProps={{ disabled: readOnly }}
        okText="Lưu"
        cancelText="Đóng"
        forceRender
      >
        {readOnly && <Alert type="info" title="Chỉ chỉnh sửa được phòng đang sẵn sàng." />}
        {types.isError && <Alert type="error" title={types.error.message} />}
        <Form
          form={form}
          layout="vertical"
          onFinish={(values) => save.mutate(values)}
          disabled={readOnly}
        >
          <Form.Item
            name="roomNumber"
            label="Số phòng"
            rules={[
              {
                required: true,
                pattern: /^[A-Za-z0-9_-]{1,30}$/,
                message: 'Dùng chữ, số, gạch ngang hoặc gạch dưới (tối đa 30 ký tự)',
              },
            ]}
          >
            <Input maxLength={30} />
          </Form.Item>
          <Form.Item name="roomTypeId" label="Loại phòng" rules={[{ required: true }]}>
            <Select
              showSearch
              optionFilterProp="label"
              loading={types.isLoading}
              options={types.data
                ?.filter((t) => t.status === 'ACTIVE' || t.id === editing?.roomTypeId)
                .map((t) => ({ value: t.id, label: t.name }))}
            />
          </Form.Item>
          <Form.Item name="floor" label="Tầng" rules={[{ required: true }]}>
            <InputNumber min={-10} max={200} precision={0} />
          </Form.Item>
          <Form.Item name="note" label="Ghi chú">
            <Input.TextArea maxLength={2000} rows={3} />
          </Form.Item>
        </Form>
      </Modal>
    </Space>
  );
}
