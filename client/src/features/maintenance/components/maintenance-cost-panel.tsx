import { ResponsiveTable as Table } from '../../../components/data/responsive-table';
import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  App,
  Button,
  Card,
  Descriptions,
  Form,
  Input,
  InputNumber,
  Modal,
  Space,
} from 'antd';
import dayjs from 'dayjs';
import { apiClient } from '../../../lib/axios';
import type { ApiResponse } from '../../../types/api';
import type { MaintenanceDetail } from '../types/maintenance';

interface MaintenanceCost {
  amount: number;
  expenseDate: string;
  note: string;
  revision: number;
  recordedBy: string;
  recordedByName: string;
  recordedAt: string;
}
interface CostInput {
  amount: number;
  expenseDate: string;
  note: string;
  expectedRevision: number;
}
const money = (value: number) =>
  new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value);

export function MaintenanceCostPanel({ request }: { request: MaintenanceDetail }) {
  const { message } = App.useApp();
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState(false);
  const [form] = Form.useForm<CostInput>();
  const path = `/maintenance-requests/${request.id}/cost`;
  const queryKey = ['maintenance-cost', request.id];
  const query = useQuery({
    queryKey,
    queryFn: async () => (await apiClient.get<ApiResponse<MaintenanceCost | null>>(path)).data.data,
  });
  const history = useQuery({
    queryKey: ['maintenance-cost-history', request.id],
    queryFn: async () =>
      (await apiClient.get<ApiResponse<MaintenanceCost[]>>(`${path}/history`)).data.data,
  });
  const save = useMutation({
    mutationFn: async (input: CostInput) => apiClient.put(path, input),
    onSuccess: async () => {
      setEditing(false);
      message.success('Đã ghi nhận chi phí bảo trì');
      await Promise.all([
        queryClient.invalidateQueries({ queryKey }),
        queryClient.invalidateQueries({ queryKey: ['maintenance-cost-history', request.id] }),
        queryClient.invalidateQueries({ queryKey: ['manager-dashboard'] }),
      ]);
    },
    onError: (error) => message.error(error.message),
  });
  const open = () => {
    form.setFieldsValue(
      query.data
        ? { ...query.data, expectedRevision: query.data.revision }
        : { amount: 0, expenseDate: dayjs().format('YYYY-MM-DD'), note: '', expectedRevision: 0 },
    );
    setEditing(true);
  };
  return (
    <Card title="Chi phí bảo trì" style={{ marginTop: 24 }}>
      <Space orientation="vertical" style={{ width: '100%' }} size="middle">
        {query.isError ? (
          <Alert
            type="error"
            title={query.error.message}
            action={<Button onClick={() => query.refetch()}>Tải lại chi phí</Button>}
          />
        ) : query.data ? (
          <Descriptions
            column={1}
            items={[
              { key: 'amount', label: 'Tổng chi phí đã chi', children: money(query.data.amount) },
              {
                key: 'date',
                label: 'Ngày chi',
                children: dayjs(query.data.expenseDate).format('DD/MM/YYYY'),
              },
              { key: 'note', label: 'Nội dung / lý do điều chỉnh', children: query.data.note },
              { key: 'author', label: 'Người ghi nhận', children: query.data.recordedByName },
              {
                key: 'time',
                label: 'Ghi nhận lúc',
                children: dayjs(query.data.recordedAt).format('DD/MM/YYYY HH:mm'),
              },
            ]}
          />
        ) : (
          !query.isLoading && (
            <Alert
              type="warning"
              title="Chưa nhập chi phí. Báo cáo chưa tính khoản chi của yêu cầu này."
            />
          )
        )}
        <Button
          onClick={open}
          disabled={request.status === 'PENDING' || query.isLoading || query.isError}
        >
          {query.data ? 'Điều chỉnh chi phí' : 'Nhập chi phí'}
        </Button>
        {request.status === 'PENDING' && (
          <Alert type="info" title="Bắt đầu xử lý bảo trì trước khi ghi nhận chi phí." />
        )}
        {history.isError && <Alert type="error" title="Không tải được lịch sử chi phí" />}
        {!!history.data?.length && (
          <Table
            size="small"
            rowKey="revision"
            dataSource={history.data}
            pagination={false}
            scroll={{ x: 650 }}
            columns={[
              { title: 'Lần ghi', dataIndex: 'revision' },
              { title: 'Tổng chi phí', dataIndex: 'amount', render: money },
              { title: 'Ngày chi', dataIndex: 'expenseDate' },
              { title: 'Nội dung', dataIndex: 'note' },
              { title: 'Người ghi nhận', dataIndex: 'recordedByName' },
            ]}
          />
        )}
      </Space>
      <Modal
        title="Ghi nhận chi phí bảo trì"
        open={editing}
        onCancel={() => !save.isPending && setEditing(false)}
        onOk={() => form.submit()}
        okText="Lưu chi phí"
        cancelText="Hủy"
        confirmLoading={save.isPending}
        forceRender
      >
        <Alert
          type="info"
          title="Nhập tổng chi phí thực tế đã chi cho yêu cầu này, không phải số tiền cộng thêm. Nhập 0 và giải thích nếu không mất phí. Mỗi lần sửa đều lưu lịch sử."
        />
        <Form
          form={form}
          layout="vertical"
          onFinish={(values) => save.mutate(values)}
          disabled={save.isPending}
        >
          <Form.Item
            name="amount"
            label="Tổng chi phí đã chi (VND)"
            rules={[{ required: true, type: 'integer', min: 0, max: 999999999999 }]}
          >
            <InputNumber min={0} max={999999999999} precision={0} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="expenseDate" label="Ngày chi" rules={[{ required: true }]}>
            <Input
              type="date"
              min={dayjs(request.createdAt).format('YYYY-MM-DD')}
              max={dayjs().format('YYYY-MM-DD')}
            />
          </Form.Item>
          <Form.Item
            name="note"
            label="Nội dung chi phí / lý do điều chỉnh"
            rules={[{ required: true, whitespace: true, max: 2000 }]}
          >
            <Input.TextArea rows={3} maxLength={2000} />
          </Form.Item>
          <Form.Item name="expectedRevision" hidden>
            <InputNumber />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
}
