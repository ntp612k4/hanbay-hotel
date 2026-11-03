import { Button, Card, Col, DatePicker, Form, InputNumber, Row, Typography } from 'antd';
import type { FormInstance } from 'antd';
import dayjs from 'dayjs';

import { BookingSteps } from '@components/booking/BookingSteps';
import type { SearchFormValues } from './room-search-utils';

const { RangePicker } = DatePicker;
const { Text, Title } = Typography;

interface RoomAvailabilityFormProps {
  form: FormInstance<SearchFormValues>;
  initialValues: SearchFormValues;
  searchLoading: boolean;
  onSearch: (values: SearchFormValues) => void;
}

export function RoomAvailabilityForm({
  form,
  initialValues,
  searchLoading,
  onSearch,
}: RoomAvailabilityFormProps) {
  return (
    <Card className="booking-panel" bordered>
      <div className="booking-panel-top">
        <div>
          <Text className="eyebrow">Kiểm tra phòng trống</Text>
          <Title level={3}>Tìm phòng phù hợp</Title>
        </div>
        <BookingSteps current={0} className="booking-steps" />
      </div>

      <Form
        form={form}
        layout="vertical"
        initialValues={initialValues}
        requiredMark={false}
        onFinish={(values) => onSearch(values)}
      >
        <Row gutter={[14, 4]} align="bottom">
          <Col xs={24} lg={8}>
            <Form.Item
              label="Ngày lưu trú"
              name="stayDates"
              rules={[
                { required: true, message: 'Vui lòng chọn ngày lưu trú.' },
                {
                  validator: (_, dates: SearchFormValues['stayDates'] | undefined) =>
                    dates?.[1]?.isAfter(dates[0])
                      ? Promise.resolve()
                      : Promise.reject(new Error('Ngày trả phòng phải sau ngày nhận phòng.')),
                },
              ]}
            >
              <RangePicker
                allowClear={false}
                format="DD/MM/YYYY"
                disabledDate={(date) => date.isBefore(dayjs().startOf('day'))}
                style={{ width: '100%' }}
              />
            </Form.Item>
          </Col>
          <Col xs={12} md={6} lg={3}>
            <Form.Item label="Người lớn" name="adults" rules={[{ type: 'number', min: 1 }]}>
              <InputNumber min={1} max={12} style={{ width: '100%' }} />
            </Form.Item>
          </Col>
          <Col xs={12} md={6} lg={3}>
            <Form.Item label="Trẻ em" name="children" rules={[{ type: 'number', min: 0 }]}>
              <InputNumber min={0} max={8} style={{ width: '100%' }} />
            </Form.Item>
          </Col>
          <Col xs={24} lg={10}>
            <Form.Item>
              <Button block htmlType="submit" loading={searchLoading} size="large" type="primary">
                Tìm kiếm
              </Button>
            </Form.Item>
          </Col>
        </Row>
      </Form>
    </Card>
  );
}
