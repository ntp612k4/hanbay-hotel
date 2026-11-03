import { Form, Input, InputNumber, Modal, type FormInstance } from 'antd';
import { useEffect } from 'react';

import type { AddOnFormValues, AddOnManagementItem } from '../types/addon-management';

interface AddOnFormModalProps {
  open: boolean;
  editing: AddOnManagementItem | null;
  loading: boolean;
  onCancel: () => void;
  onSubmit: (values: AddOnFormValues, form: FormInstance<AddOnFormValues>) => void;
}

export function AddOnFormModal({
  open,
  editing,
  loading,
  onCancel,
  onSubmit,
}: AddOnFormModalProps) {
  const [form] = Form.useForm<AddOnFormValues>();

  useEffect(() => {
    if (!open) return;
    if (editing) {
      form.setFieldsValue({
        code: editing.code,
        name: editing.name,
        description: editing.description ?? undefined,
        price: editing.price,
      });
    } else {
      form.resetFields();
    }
  }, [editing, form, open]);

  return (
    <Modal
      title={editing ? 'Cập nhật dịch vụ bổ sung' : 'Thêm dịch vụ bổ sung'}
      open={open}
      okText={editing ? 'Lưu thay đổi' : 'Tạo dịch vụ'}
      cancelText="Hủy"
      confirmLoading={loading}
      destroyOnHidden
      onCancel={onCancel}
      onOk={() => void form.validateFields().then((values) => onSubmit(values, form))}
    >
      <Form form={form} layout="vertical" requiredMark="optional">
        <Form.Item
          name="code"
          label="Mã dịch vụ"
          extra={editing ? 'Mã dịch vụ không thể thay đổi sau khi tạo.' : undefined}
          rules={[
            { required: true, whitespace: true, message: 'Vui lòng nhập mã dịch vụ' },
            { min: 2, max: 50, message: 'Mã dịch vụ phải có từ 2 đến 50 ký tự' },
            {
              pattern: /^[A-Z][A-Z0-9_]*$/,
              message: 'Chỉ dùng chữ in hoa, số và dấu gạch dưới',
            },
          ]}
        >
          <Input disabled={editing !== null} placeholder="AIRPORT_PICKUP" maxLength={50} />
        </Form.Item>
        <Form.Item
          name="name"
          label="Tên dịch vụ"
          rules={[
            { required: true, whitespace: true, message: 'Vui lòng nhập tên dịch vụ' },
            { min: 2, max: 150, message: 'Tên dịch vụ phải có từ 2 đến 150 ký tự' },
          ]}
        >
          <Input placeholder="Đón tại sân bay" maxLength={150} showCount />
        </Form.Item>
        <Form.Item
          name="description"
          label="Mô tả"
          rules={[{ max: 2000, message: 'Mô tả không được vượt quá 2000 ký tự' }]}
        >
          <Input.TextArea rows={4} maxLength={2000} showCount />
        </Form.Item>
        <Form.Item
          name="price"
          label="Giá hiện hành (VND)"
          rules={[
            { required: true, message: 'Vui lòng nhập giá dịch vụ' },
            { type: 'number', min: 0, message: 'Giá dịch vụ phải lớn hơn hoặc bằng 0' },
          ]}
        >
          <InputNumber<number>
            min={0}
            precision={0}
            step={50_000}
            style={{ width: '100%' }}
            formatter={(value) => (value == null ? '' : Number(value).toLocaleString('vi-VN'))}
            parser={(value) => Number(value?.replaceAll('.', '').replaceAll(',', '') ?? 0)}
          />
        </Form.Item>
      </Form>
    </Modal>
  );
}
