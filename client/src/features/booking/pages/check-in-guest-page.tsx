import {
  CheckCircleOutlined,
  IdcardOutlined,
  LoginOutlined,
  NumberOutlined,
} from '@ant-design/icons';
import { useMutation, useQuery } from '@tanstack/react-query';
import {
  Alert,
  App,
  Button,
  Card,
  Col,
  Descriptions,
  Form,
  Input,
  Radio,
  Row,
  Space,
  Tag,
  Typography,
} from 'antd';
import dayjs from 'dayjs';
import { useState } from 'react';

import { checkInGuest, getAvailablePhysicalRooms } from '../api/booking-api';
import type { CheckInGuestResponse } from '../types/booking';
import { getCheckInErrorMessage } from '../utils/check-in-error-mapper';
import './check-in-guest-page.css';

interface CheckInFormValues {
  bookingCode: string;
  roomId: string;
}

export function CheckInGuestPage() {
  const { message } = App.useApp();
  const [form] = Form.useForm<CheckInFormValues>();
  const [result, setResult] = useState<CheckInGuestResponse | null>(null);
  const [searchedBookingCode, setSearchedBookingCode] = useState('');

  const roomsQuery = useQuery({
    queryKey: ['available-physical-rooms', searchedBookingCode],
    queryFn: () => getAvailablePhysicalRooms(searchedBookingCode),
    enabled: searchedBookingCode.length > 0,
    retry: false,
  });

  const checkInMutation = useMutation({
    mutationFn: ({ bookingCode, roomId }: CheckInFormValues) =>
      checkInGuest(bookingCode.trim().toUpperCase(), roomId.trim()),
    onSuccess: (data) => {
      setResult(data);
      void message.success(`Check-in thành công, phòng ${data.room.roomNumber}`);
    },
    onMutate: () => setResult(null),
  });

  const resetForm = () => {
    form.resetFields();
    checkInMutation.reset();
    setSearchedBookingCode('');
    setResult(null);
  };

  const loadAvailableRooms = async () => {
    const { bookingCode } = await form.validateFields(['bookingCode']);
    form.setFieldValue('roomId', undefined);
    setResult(null);
    const normalizedCode = bookingCode.trim().toUpperCase();
    setSearchedBookingCode(normalizedCode);
    if (normalizedCode === searchedBookingCode) {
      void roomsQuery.refetch();
    }
  };

  return (
    <main className="check-in-page">
      <header className="check-in-page__header">
        <Typography.Text className="check-in-page__eyebrow">FRONT DESK</Typography.Text>
        <Typography.Title level={2}>Check-in khách</Typography.Title>
        <Typography.Paragraph type="secondary">
          Nhập mã booking đã xác nhận và phòng vật lý còn trống để hoàn tất nhận phòng.
        </Typography.Paragraph>
      </header>

      <Row gutter={[24, 24]} align="top">
        <Col xs={24} lg={result ? 11 : 16} xl={result ? 10 : 14}>
          <Card title="Thông tin check-in" className="check-in-page__card">
            <Form
              form={form}
              layout="vertical"
              requiredMark="optional"
              onFinish={(values) => checkInMutation.mutate(values)}
            >
              <Form.Item
                name="bookingCode"
                label="Mã booking"
                normalize={(value: string) => value.toUpperCase()}
                rules={[
                  { required: true, whitespace: true, message: 'Vui lòng nhập mã booking' },
                  { max: 50, message: 'Mã booking không được vượt quá 50 ký tự' },
                ]}
              >
                <Input
                  prefix={<IdcardOutlined />}
                  placeholder="Ví dụ: BK20260819000001"
                  autoComplete="off"
                  size="large"
                  onChange={() => {
                    setSearchedBookingCode('');
                    form.setFieldValue('roomId', undefined);
                  }}
                />
              </Form.Item>

              <Button
                className="check-in-page__load-rooms"
                onClick={() => void loadAvailableRooms()}
                loading={roomsQuery.isFetching}
              >
                Tìm phòng khả dụng
              </Button>

              {roomsQuery.isError && (
                <Alert
                  className="check-in-page__error"
                  type="error"
                  showIcon
                  message="Không thể tải phòng khả dụng"
                  description={getCheckInErrorMessage(roomsQuery.error)}
                />
              )}

              {roomsQuery.isSuccess && roomsQuery.data.length === 0 && (
                <Alert
                  className="check-in-page__error"
                  type="warning"
                  showIcon
                  message="Không còn phòng khả dụng thuộc hạng phòng đã đặt"
                />
              )}

              <Form.Item
                name="roomId"
                label="Phòng vật lý"
                extra="Danh sách được lọc theo hạng phòng của booking và trạng thái AVAILABLE."
                rules={[{ required: true, message: 'Vui lòng chọn phòng' }]}
              >
                <Radio.Group
                  className="check-in-page__room-options"
                  disabled={!roomsQuery.isSuccess || roomsQuery.data.length === 0}
                >
                  {(roomsQuery.data ?? []).map((room) => (
                    <Radio.Button key={room.roomId} value={room.roomId}>
                      <span className="check-in-page__room-label">
                        <NumberOutlined />
                        <span>
                          Phòng {room.roomNumber} · Tầng {room.floor}
                        </span>
                        <CheckCircleOutlined className="check-in-page__room-selected-icon" />
                      </span>
                    </Radio.Button>
                  ))}
                </Radio.Group>
              </Form.Item>

              {checkInMutation.isError && (
                <Alert
                  className="check-in-page__error"
                  type="error"
                  showIcon
                  message="Không thể check-in"
                  description={getCheckInErrorMessage(checkInMutation.error)}
                />
              )}

              <Space wrap>
                <Button
                  type="primary"
                  htmlType="submit"
                  size="large"
                  icon={<LoginOutlined />}
                  loading={checkInMutation.isPending}
                >
                  Xác nhận check-in
                </Button>
                <Button size="large" onClick={resetForm} disabled={checkInMutation.isPending}>
                  Làm mới
                </Button>
              </Space>
            </Form>
          </Card>
        </Col>

        {result && (
          <Col xs={24} lg={13} xl={14}>
            <Card className="check-in-page__result">
              <Space orientation="vertical" size="large" className="check-in-page__result-content">
                <CheckCircleOutlined className="check-in-page__success-icon" />
                <div className="check-in-page__result-heading">
                  <Typography.Title level={3}>Check-in thành công</Typography.Title>
                  <Typography.Text type="secondary">
                    Có thể bàn giao chìa khóa phòng cho khách.
                  </Typography.Text>
                </div>
                <Descriptions bordered column={1} size="middle" className="check-in-page__details">
                  <Descriptions.Item label="Mã booking">{result.bookingCode}</Descriptions.Item>
                  <Descriptions.Item label="Số phòng">
                    <Typography.Text strong>{result.room.roomNumber}</Typography.Text>
                  </Descriptions.Item>
                  <Descriptions.Item label="Tầng">{result.room.floor}</Descriptions.Item>
                  <Descriptions.Item label="Booking">
                    <Tag color="blue">Đã nhận phòng</Tag>
                  </Descriptions.Item>
                  <Descriptions.Item label="Phòng">
                    <Tag color="green">Đang sử dụng</Tag>
                  </Descriptions.Item>
                  <Descriptions.Item label="Thời gian">
                    {dayjs(result.checkedInAt).format('DD/MM/YYYY HH:mm')}
                  </Descriptions.Item>
                </Descriptions>
              </Space>
            </Card>
          </Col>
        )}
      </Row>
    </main>
  );
}
