import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  App,
  Button,
  Form,
  Image,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  Upload,
} from 'antd';
import { apiClient } from '../../lib/axios';
import type { ApiResponse, PageResponse } from '../../types/api';

export interface ManagedRoomType {
  id: string;
  code: string;
  name: string;
  description: string;
  shortDescription: string;
  basePrice: number;
  maxGuests: number;
  capacityAdults: number;
  capacityChildren: number;
  bedType: string;
  areaM2: number | null;
  status: 'ACTIVE' | 'INACTIVE';
  images: string[];
  amenityIds: string[];
}
const endpoint = '/management/room-types';
export function RoomTypeManagementPage() {
  const { message } = App.useApp();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [editing, setEditing] = useState<ManagedRoomType | null | undefined>();
  const [uploading, setUploading] = useState(false);
  const [form] = Form.useForm<ManagedRoomType>();
  const images = Form.useWatch('images', form) ?? [];
  const query = useQuery({
    queryKey: ['managed-room-types', page],
    queryFn: async () =>
      (
        await apiClient.get<ApiResponse<PageResponse<ManagedRoomType>>>(endpoint, {
          params: { page, size: 20 },
        })
      ).data.data,
  });
  const amenities = useQuery({
    queryKey: ['amenities'],
    queryFn: async () =>
      (await apiClient.get<ApiResponse<{ id: string; name: string }[]>>(`${endpoint}/amenities`))
        .data.data,
  });
  const refresh = async () => {
    await queryClient.invalidateQueries({ queryKey: ['managed-room-types'] });
    await queryClient.invalidateQueries({ queryKey: ['room-types'] });
  };
  const save = useMutation({
    mutationFn: async (values: ManagedRoomType) =>
      editing
        ? apiClient.put(`${endpoint}/${editing.id}`, values)
        : apiClient.post(endpoint, values),
    onSuccess: async () => {
      setEditing(undefined);
      await refresh();
      message.success('Đã lưu loại phòng');
    },
    onError: (error) => message.error(error.message),
  });
  const remove = useMutation({
    mutationFn: (id: string) => apiClient.delete(`${endpoint}/${id}`),
    onSuccess: async () => {
      await refresh();
      message.success('Đã xóa loại phòng');
    },
    onError: (error) => message.error(error.message),
  });
  const open = (record: ManagedRoomType | null) => {
    form.resetFields();
    form.setFieldsValue(
      record ?? {
        code: '',
        name: '',
        basePrice: 0,
        maxGuests: 2,
        capacityAdults: 2,
        capacityChildren: 0,
        status: 'ACTIVE',
        images: [],
        amenityIds: [],
      },
    );
    setEditing(record);
  };
  return (
    <Space orientation="vertical" size="large" style={{ width: '100%' }}>
      <Space style={{ justifyContent: 'space-between', width: '100%' }}>
        <Typography.Title level={2}>Quản lý loại phòng</Typography.Title>
        <Button type="primary" onClick={() => open(null)}>
          Thêm loại phòng
        </Button>
      </Space>
      {query.isError && (
        <Alert
          type="error"
          title={query.error.message}
          action={<Button onClick={() => query.refetch()}>Thử lại</Button>}
        />
      )}
      <Table
        rowKey="id"
        loading={query.isLoading}
        dataSource={query.data?.content}
        scroll={{ x: 800 }}
        pagination={{
          current: page + 1,
          pageSize: 20,
          total: query.data?.totalElements,
          showSizeChanger: false,
          onChange: (value) => setPage(value - 1),
        }}
        columns={[
          {
            title: 'Ảnh',
            render: (_, r) =>
              r.images[0] ? (
                <Image width={80} height={55} style={{ objectFit: 'cover' }} src={r.images[0]} />
              ) : (
                'Chưa có ảnh'
              ),
          },
          { title: 'Mã', dataIndex: 'code' },
          { title: 'Tên loại phòng', dataIndex: 'name' },
          {
            title: 'Giá / đêm',
            dataIndex: 'basePrice',
            render: (v) => `${Number(v).toLocaleString('vi-VN')} ₫`,
          },
          { title: 'Số khách', dataIndex: 'maxGuests' },
          {
            title: 'Trạng thái',
            dataIndex: 'status',
            render: (v) => (
              <Tag color={v === 'ACTIVE' ? 'green' : 'default'}>
                {v === 'ACTIVE' ? 'Đang kinh doanh' : 'Ngừng kinh doanh'}
              </Tag>
            ),
          },
          {
            title: 'Thao tác',
            render: (_, r) => (
              <Space>
                <Button onClick={() => open(r)}>Chi tiết / Sửa</Button>
                <Popconfirm
                  title="Xóa loại phòng này?"
                  description="Chỉ xóa được loại phòng chưa có dữ liệu tham chiếu."
                  onConfirm={() => remove.mutateAsync(r.id)}
                >
                  <Button danger loading={remove.isPending}>
                    Xóa
                  </Button>
                </Popconfirm>
              </Space>
            ),
          },
        ]}
      />
      <Modal
        title={editing ? 'Chi tiết loại phòng' : 'Thêm loại phòng'}
        open={editing !== undefined}
        onCancel={() => setEditing(undefined)}
        onOk={() => form.submit()}
        confirmLoading={save.isPending}
        okButtonProps={{ disabled: uploading }}
        okText="Lưu"
        cancelText="Hủy"
        width={760}
        forceRender
      >
        <Form form={form} layout="vertical" onFinish={(values) => save.mutate(values)}>
          <Form.Item
            name="code"
            label="Mã loại phòng"
            rules={[
              {
                required: true,
                pattern: /^[A-Z0-9_-]{1,50}$/,
                message: 'Dùng chữ hoa, số, dấu gạch ngang hoặc gạch dưới (tối đa 50 ký tự)',
              },
            ]}
          >
            <Input maxLength={50} />
          </Form.Item>
          <Form.Item
            name="name"
            label="Tên loại phòng"
            rules={[{ required: true, whitespace: true }]}
          >
            <Input maxLength={150} />
          </Form.Item>
          <Form.Item name="shortDescription" label="Mô tả ngắn">
            <Input.TextArea maxLength={500} />
          </Form.Item>
          <Form.Item name="description" label="Mô tả chi tiết">
            <Input.TextArea rows={3} maxLength={10000} />
          </Form.Item>
          <Space wrap align="start">
            <Form.Item name="basePrice" label="Giá cơ bản (VND)" rules={[{ required: true }]}>
              <InputNumber min={0} precision={0} style={{ width: 180 }} />
            </Form.Item>
            <Form.Item name="maxGuests" label="Tối đa khách" rules={[{ required: true }]}>
              <InputNumber min={1} max={100} precision={0} />
            </Form.Item>
            <Form.Item name="capacityAdults" label="Người lớn" rules={[{ required: true }]}>
              <InputNumber min={1} max={100} precision={0} />
            </Form.Item>
            <Form.Item name="capacityChildren" label="Trẻ em" rules={[{ required: true }]}>
              <InputNumber min={0} max={100} precision={0} />
            </Form.Item>
            <Form.Item name="areaM2" label="Diện tích (m²)">
              <InputNumber min={0.01} max={9999.99} precision={2} />
            </Form.Item>
          </Space>
          <Form.Item name="bedType" label="Loại giường">
            <Input maxLength={100} />
          </Form.Item>
          <Form.Item name="status" label="Trạng thái" rules={[{ required: true }]}>
            <Select
              options={[
                { value: 'ACTIVE', label: 'Đang kinh doanh' },
                { value: 'INACTIVE', label: 'Ngừng kinh doanh' },
              ]}
            />
          </Form.Item>
          <Form.Item name="amenityIds" label="Tiện nghi">
            <Select
              mode="multiple"
              loading={amenities.isLoading}
              options={amenities.data?.map((a) => ({ value: a.id, label: a.name }))}
            />
          </Form.Item>
          <Form.Item name="images" hidden>
            <Input />
          </Form.Item>
          <Typography.Paragraph>
            Ảnh JPEG / PNG, tối đa 5 MB mỗi ảnh. Ảnh đầu tiên là ảnh đại diện.
          </Typography.Paragraph>
          <Space wrap>
            {images.map((url: string, i: number) => (
              <Space orientation="vertical" key={`${url}-${i}`}>
                <Image src={url} width={100} height={75} style={{ objectFit: 'cover' }} />
                <Space>
                  <Button
                    size="small"
                    disabled={i === 0 || uploading}
                    onClick={() =>
                      form.setFieldValue('images', [
                        url,
                        ...images.filter((_: string, index: number) => index !== i),
                      ])
                    }
                  >
                    Đại diện
                  </Button>
                  <Button
                    size="small"
                    danger
                    disabled={uploading}
                    onClick={() =>
                      form.setFieldValue(
                        'images',
                        images.filter((_: string, index: number) => index !== i),
                      )
                    }
                  >
                    Bỏ
                  </Button>
                </Space>
              </Space>
            ))}
          </Space>
          <Upload
            accept="image/jpeg,image/png"
            showUploadList={false}
            disabled={uploading || images.length >= 20}
            beforeUpload={async (file) => {
              if (file.size > 5 * 1024 * 1024) {
                message.error('Ảnh vượt quá 5 MB');
                return false;
              }
              setUploading(true);
              try {
                const data = new FormData();
                data.append('file', file);
                const response = await apiClient.post<ApiResponse<{ url: string }>>(
                  '/media',
                  data,
                  { headers: { 'Content-Type': undefined }, timeout: 60000 },
                );
                form.setFieldValue('images', [
                  ...form.getFieldValue('images'),
                  response.data.data.url,
                ]);
              } catch (error) {
                message.error(error instanceof Error ? error.message : 'Tải ảnh thất bại');
              } finally {
                setUploading(false);
              }
              return false;
            }}
          >
            <Button loading={uploading} style={{ marginTop: 12 }}>
              Tải ảnh lên
            </Button>
          </Upload>
        </Form>
      </Modal>
    </Space>
  );
}
