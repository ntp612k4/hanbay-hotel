import { Form, Input, Select } from 'antd';

import type { RefundInformation } from '../types/refund';

export function RefundInformationForm({
  value,
  onChange,
  disabled = false,
}: {
  value: RefundInformation;
  onChange: (value: RefundInformation) => void;
  disabled?: boolean;
}) {
  const update = (field: keyof RefundInformation, fieldValue: string) =>
    onChange({ ...value, [field]: fieldValue });

  return (
    <Form layout="vertical" component="div" aria-label="Thông tin hoàn tiền">
      <Form.Item label="Phương thức hoàn tiền" required>
        <Select
          disabled={disabled}
          value="BANK_TRANSFER"
          options={[{ value: 'BANK_TRANSFER', label: 'Chuyển khoản ngân hàng' }]}
        />
      </Form.Item>
      <Form.Item label="Ngân hàng" required>
        <Input
          disabled={disabled}
          maxLength={100}
          value={value.bankName}
          onChange={(event) => update('bankName', event.target.value)}
        />
      </Form.Item>
      <Form.Item label="Chủ tài khoản" required>
        <Input
          disabled={disabled}
          maxLength={150}
          value={value.accountHolderName}
          onChange={(event) => update('accountHolderName', event.target.value)}
        />
      </Form.Item>
      <Form.Item label="Số tài khoản" required>
        <Input
          disabled={disabled}
          minLength={4}
          maxLength={34}
          value={value.accountNumber}
          onChange={(event) =>
            update('accountNumber', event.target.value.replace(/[^0-9A-Za-z]/g, ''))
          }
        />
      </Form.Item>
    </Form>
  );
}
