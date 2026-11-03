import { DatePicker, Form, Input, InputNumber, Modal, Select, type FormInstance } from 'antd';
import dayjs from 'dayjs';
import { useEffect } from 'react';

import type { RoomTypeCatalogItem } from '../../room/types/room-type';
import type {
  PriceRuleFormValues,
  PriceRuleManagementItem,
  RuleType,
} from '../types/price-rule-management';
import { HolidayRuleNotice } from './holiday-rule-notice';
import { allowsDirectionSelection, directionForRuleType } from '../utils/price-rule-policy';

const ruleTypeOptions = [
  { value: 'WEEKEND', label: 'Cuối tuần' },
  { value: 'HOLIDAY', label: 'Ngày lễ' },
  { value: 'SEASONAL', label: 'Mùa cao/thấp điểm' },
  { value: 'PROMOTION', label: 'Khuyến mãi công khai' },
];

const adjustmentTypeOptions = [
  { value: 'FIXED', label: 'Số tiền cố định' },
  { value: 'PERCENTAGE', label: 'Phần trăm' },
];

const adjustmentDirectionOptions = [
  { value: 'INCREASE', label: 'Tăng giá' },
  { value: 'DISCOUNT', label: 'Giảm giá' },
];

interface PriceRuleFormModalProps {
  open: boolean;
  editing: PriceRuleManagementItem | null;
  loading: boolean;
  roomTypes: RoomTypeCatalogItem[];
  roomTypesLoading: boolean;
  onManageHolidays: () => void;
  onCancel: () => void;
  onSubmit: (values: PriceRuleFormValues, form: FormInstance<PriceRuleFormValues>) => void;
}

export function PriceRuleFormModal({
  open,
  editing,
  loading,
  roomTypes,
  roomTypesLoading,
  onCancel,
  onManageHolidays,
  onSubmit,
}: PriceRuleFormModalProps) {
  const [form] = Form.useForm<PriceRuleFormValues>();
  const ruleType = Form.useWatch('ruleType', form);
  const adjustmentType = Form.useWatch('adjustmentType', form);
  const startDate = Form.useWatch('startDate', form);
  const endDate = Form.useWatch('endDate', form);

  useEffect(() => {
    if (!open) return;
    if (editing) {
      form.setFieldsValue({
        roomTypeId: editing.roomType.id,
        name: editing.name,
        ruleType: editing.ruleType,
        adjustmentType: editing.adjustmentType,
        adjustmentValue: editing.adjustmentValue,
        adjustmentDirection: editing.adjustmentDirection,
        startDate: dayjs(editing.startDate),
        endDate: dayjs(editing.endDate),
        priority: editing.priority,
      });
    } else {
      form.resetFields();
      form.setFieldsValue({
        adjustmentType: 'PERCENTAGE',
        adjustmentDirection: 'INCREASE',
        ruleType: 'WEEKEND',
        priority: 10,
      });
    }
  }, [editing, form, open]);

  return (
    <Modal
      title={editing ? 'Cập nhật quy tắc giá' : 'Thêm quy tắc giá'}
      open={open}
      okText={editing ? 'Lưu thay đổi' : 'Tạo quy tắc'}
      cancelText="Hủy"
      confirmLoading={loading}
      destroyOnHidden
      width={720}
      onCancel={onCancel}
      onOk={() => void form.validateFields().then((values) => onSubmit(values, form))}
    >
      <Form form={form} layout="vertical" requiredMark="optional">
        {open && ruleType === 'HOLIDAY' && (
          <HolidayRuleNotice
            startDate={startDate?.format('YYYY-MM-DD')}
            endDate={endDate?.format('YYYY-MM-DD')}
            onManage={onManageHolidays}
          />
        )}
        <Form.Item
          name="roomTypeId"
          label="Hạng phòng"
          rules={[{ required: true, message: 'Vui lòng chọn hạng phòng' }]}
        >
          <Select
            showSearch
            loading={roomTypesLoading}
            placeholder="Chọn hạng phòng"
            optionFilterProp="label"
            options={roomTypes.map((roomType) => ({
              value: roomType.id,
              label: roomType.name,
            }))}
          />
        </Form.Item>
        <Form.Item
          name="name"
          label="Tên quy tắc"
          rules={[
            { required: true, whitespace: true, message: 'Vui lòng nhập tên quy tắc' },
            { min: 2, max: 150, message: 'Tên quy tắc phải có từ 2 đến 150 ký tự' },
          ]}
        >
          <Input placeholder="Weekend surcharge Deluxe" maxLength={150} showCount />
        </Form.Item>
        <Form.Item
          name="ruleType"
          label="Loại quy tắc"
          rules={[{ required: true, message: 'Vui lòng chọn loại quy tắc' }]}
        >
          <Select
            options={ruleTypeOptions}
            onChange={(value: RuleType) => {
              form.setFieldValue(
                'adjustmentDirection',
                directionForRuleType(value, form.getFieldValue('adjustmentDirection')),
              );
            }}
          />
        </Form.Item>
        <Form.Item
          name="adjustmentDirection"
          label="Hướng điều chỉnh"
          extra={
            ruleType === 'SEASONAL'
              ? 'Chọn tăng giá mùa cao điểm hoặc giảm giá mùa thấp điểm.'
              : 'Hướng được hệ thống cố định theo loại quy tắc.'
          }
          rules={[{ required: true, message: 'Vui lòng chọn hướng điều chỉnh' }]}
        >
          <Select
            options={adjustmentDirectionOptions}
            disabled={!allowsDirectionSelection(ruleType)}
          />
        </Form.Item>
        <Form.Item
          name="adjustmentType"
          label="Kiểu điều chỉnh"
          rules={[{ required: true, message: 'Vui lòng chọn kiểu điều chỉnh' }]}
        >
          <Select options={adjustmentTypeOptions} />
        </Form.Item>
        <Form.Item
          name="adjustmentValue"
          label={adjustmentType === 'FIXED' ? 'Giá trị điều chỉnh (VND)' : 'Giá trị điều chỉnh (%)'}
          dependencies={['adjustmentType']}
          rules={[
            { required: true, message: 'Vui lòng nhập giá trị điều chỉnh' },
            {
              type: 'number',
              min: 0,
              message: 'Giá trị điều chỉnh phải lớn hơn hoặc bằng 0',
            },
            ({ getFieldValue }) => ({
              validator(_, value: number | undefined) {
                if (
                  getFieldValue('adjustmentType') === 'PERCENTAGE' &&
                  value !== undefined &&
                  value > 100
                ) {
                  return Promise.reject(new Error('Phần trăm điều chỉnh không được vượt quá 100'));
                }
                return Promise.resolve();
              },
            }),
          ]}
        >
          <InputNumber<number>
            min={0}
            max={adjustmentType === 'PERCENTAGE' ? 100 : undefined}
            precision={0}
            step={adjustmentType === 'PERCENTAGE' ? 5 : 50_000}
            style={{ width: '100%' }}
            formatter={(value) => (value == null ? '' : Number(value).toLocaleString('vi-VN'))}
            parser={(value) => Number(value?.replaceAll('.', '').replaceAll(',', '') ?? 0)}
          />
        </Form.Item>
        <Form.Item
          name="startDate"
          label="Ngày bắt đầu"
          rules={[{ required: true, message: 'Vui lòng chọn ngày bắt đầu' }]}
        >
          <DatePicker style={{ width: '100%' }} format="DD/MM/YYYY" />
        </Form.Item>
        <Form.Item
          name="endDate"
          label="Ngày kết thúc"
          dependencies={['startDate']}
          rules={[
            { required: true, message: 'Vui lòng chọn ngày kết thúc' },
            () => ({
              validator(_, value) {
                if (startDate && value && value.isBefore(startDate, 'day')) {
                  return Promise.reject(new Error('Ngày kết thúc phải sau hoặc bằng ngày bắt đầu'));
                }
                return Promise.resolve();
              },
            }),
          ]}
        >
          <DatePicker style={{ width: '100%' }} format="DD/MM/YYYY" />
        </Form.Item>
        <Form.Item
          name="priority"
          label="Độ ưu tiên"
          rules={[
            { required: true, message: 'Vui lòng nhập độ ưu tiên' },
            { type: 'number', min: 0, max: 1000, message: 'Độ ưu tiên phải từ 0 đến 1000' },
          ]}
        >
          <InputNumber<number> min={0} max={1000} precision={0} style={{ width: '100%' }} />
        </Form.Item>
      </Form>
    </Modal>
  );
}
