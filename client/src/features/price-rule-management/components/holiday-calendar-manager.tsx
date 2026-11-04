import { ResponsiveTable as Table } from '../../../components/data/responsive-table';
import { Alert, App, Button, DatePicker, Form, Input, Modal, Space, Tag } from 'antd';
import dayjs, { type Dayjs } from 'dayjs';
import { useState } from 'react';
import type { HolidayCalendarItem } from '../api/holiday-calendar-api';
import { useHolidayCalendar, useHolidayMutations } from '../hooks/use-holiday-calendar';
import {
  getPriceRuleErrorMessage,
  getPriceRuleFieldErrors,
} from '../utils/price-rule-error-mapper';

interface FormValues {
  name: string;
  startDate: Dayjs;
  endDate: Dayjs;
}
export function HolidayCalendarManager({ onClose }: { onClose: () => void }) {
  const { message, modal } = App.useApp();
  const [page, setPage] = useState(0);
  const [editing, setEditing] = useState<HolidayCalendarItem>();
  const [formOpen, setFormOpen] = useState(false);
  const [form] = Form.useForm<FormValues>();
  const calendar = useHolidayCalendar(page);
  const { save, status } = useHolidayMutations();
  const openForm = (item?: HolidayCalendarItem) => {
    setEditing(item);
    form.resetFields();
    if (item)
      form.setFieldsValue({
        name: item.name,
        startDate: dayjs(item.startDate),
        endDate: dayjs(item.endDate),
      });
    setFormOpen(true);
  };
  const submit = async () => {
    let values: FormValues;
    try {
      values = await form.validateFields();
    } catch {
      return;
    }
    try {
      await save.mutateAsync({
        id: editing?.id,
        values: {
          name: values.name.trim(),
          startDate: values.startDate.format('YYYY-MM-DD'),
          endDate: values.endDate.format('YYYY-MM-DD'),
        },
      });
      setFormOpen(false);
      void message.success('Đã lưu lịch ngày lễ.');
    } catch (error) {
      form.setFields(
        getPriceRuleFieldErrors(error).filter(
          (field): field is { name: keyof FormValues; errors: string[] } =>
            field.name === 'name' || field.name === 'startDate' || field.name === 'endDate',
        ),
      );
      void message.error(getPriceRuleErrorMessage(error));
    }
  };
  return (
    <>
      <Modal
        open
        title="Quản lý lịch ngày lễ"
        width={900}
        onCancel={onClose}
        footer={<Button onClick={onClose}>Đóng</Button>}
      >
        <Space orientation="vertical" style={{ width: '100%' }}>
          <Alert
            type="info"
            showIcon
            title="Lịch lễ dùng chung cho mọi hạng phòng. Quy tắc ngày lễ chỉ áp dụng tại phần giao giữa lịch đang hoạt động và khoảng ngày của quy tắc."
          />
          <Button type="primary" onClick={() => openForm()}>
            Thêm ngày lễ
          </Button>
          {calendar.isError ? (
            <Alert
              type="error"
              title="Không tải được lịch ngày lễ"
              description={getPriceRuleErrorMessage(calendar.error)}
              action={<Button onClick={() => void calendar.refetch()}>Thử lại</Button>}
            />
          ) : (
            <Table<HolidayCalendarItem>
              rowKey="id"
              loading={calendar.isLoading}
              dataSource={calendar.data?.content ?? []}
              scroll={{ x: 700 }}
              pagination={{
                current: page + 1,
                pageSize: 100,
                total: calendar.data?.totalElements,
                showSizeChanger: false,
                onChange: (value) => setPage(value - 1),
              }}
              columns={[
                { title: 'Tên ngày lễ', dataIndex: 'name' },
                {
                  title: 'Bắt đầu',
                  dataIndex: 'startDate',
                  render: (value) => dayjs(value).format('DD/MM/YYYY'),
                },
                {
                  title: 'Kết thúc',
                  dataIndex: 'endDate',
                  render: (value) => dayjs(value).format('DD/MM/YYYY'),
                },
                {
                  title: 'Trạng thái',
                  dataIndex: 'active',
                  render: (value) => <Tag>{value ? 'Đang hoạt động' : 'Ngừng hoạt động'}</Tag>,
                },
                {
                  title: 'Thao tác',
                  render: (_, item) => (
                    <Space>
                      <Button onClick={() => openForm(item)}>Sửa</Button>
                      <Button
                        loading={status.isPending && status.variables?.id === item.id}
                        onClick={() =>
                          modal.confirm({
                            title: item.active ? 'Ngừng lịch ngày lễ?' : 'Kích hoạt lịch ngày lễ?',
                            content:
                              'Thay đổi này ảnh hưởng các lần tính giá mới của mọi hạng phòng có quy tắc ngày lễ tương ứng.',
                            okText: 'Xác nhận',
                            cancelText: 'Hủy',
                            onOk: async () => {
                              try {
                                await status.mutateAsync({ id: item.id, active: !item.active });
                              } catch (error) {
                                void message.error(getPriceRuleErrorMessage(error));
                                throw error;
                              }
                            },
                          })
                        }
                      >
                        {item.active ? 'Ngừng' : 'Kích hoạt'}
                      </Button>
                    </Space>
                  ),
                },
              ]}
            />
          )}
        </Space>
      </Modal>
      <Modal
        open={formOpen}
        title={editing ? 'Cập nhật ngày lễ' : 'Thêm ngày lễ'}
        okText="Lưu ngày lễ"
        cancelText="Hủy"
        confirmLoading={save.isPending}
        onOk={() => void submit()}
        onCancel={() => {
          if (!save.isPending) setFormOpen(false);
        }}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="name"
            label="Tên ngày lễ"
            rules={[
              { required: true, whitespace: true, message: 'Vui lòng nhập tên ngày lễ' },
              {
                min: 2,
                max: 150,
                message: 'Tên ngày lễ phải có từ 2 đến 150 ký tự',
                transform: (value) => value?.trim(),
              },
            ]}
          >
            <Input maxLength={150} />
          </Form.Item>
          <Form.Item
            name="startDate"
            label="Ngày bắt đầu"
            rules={[{ required: true, message: 'Vui lòng chọn ngày bắt đầu' }]}
          >
            <DatePicker format="DD/MM/YYYY" />
          </Form.Item>
          <Form.Item
            name="endDate"
            label="Ngày kết thúc"
            dependencies={['startDate']}
            rules={[
              { required: true, message: 'Vui lòng chọn ngày kết thúc' },
              ({ getFieldValue }) => ({
                validator: (_, value) =>
                  value &&
                  getFieldValue('startDate') &&
                  value.isBefore(getFieldValue('startDate'), 'day')
                    ? Promise.reject(new Error('Ngày kết thúc phải sau hoặc bằng ngày bắt đầu'))
                    : Promise.resolve(),
              }),
            ]}
          >
            <DatePicker format="DD/MM/YYYY" />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
}
