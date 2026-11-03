import {
  CalendarOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  CopyOutlined,
  HomeOutlined,
  LoadingOutlined,
  ReloadOutlined,
  SafetyCertificateOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { useMutation, useQuery } from '@tanstack/react-query';
import {
  Alert,
  App,
  Button,
  Card,
  Checkbox,
  Col,
  Descriptions,
  Divider,
  Flex,
  Grid,
  Row,
  Statistic,
  Tag,
  theme,
  Typography,
} from 'antd';
import dayjs from 'dayjs';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { ApiClientError } from '../../../types/api';
import { createPaymentRequest, getPaymentStatus } from '../api/booking-api';
import type {
  SelectedAddOn,
  CreatePaymentRequestResponse,
  CreatePendingBookingRequest,
  PendingBookingSummary,
  RoomTypeCatalogItem,
} from '../types/booking';
import { storePaymentResultToken } from '../utils/payment-result-storage';
import { getPaymentPollingInterval } from '../utils/payment-status-polling';

interface BookingPaymentStepProps {
  booking: PendingBookingSummary;
  request: CreatePendingBookingRequest;
  roomType?: RoomTypeCatalogItem;
  addOns: SelectedAddOn[];
  onEdit: () => void;
}

export function BookingPaymentPage({
  booking,
  request,
  roomType,
  addOns,
  onEdit,
}: BookingPaymentStepProps) {
  const { message } = App.useApp();
  const { token } = theme.useToken();
  const screens = Grid.useBreakpoint();
  const navigate = useNavigate();
  const [acceptedTerms, setAcceptedTerms] = useState(false);
  const nights = dayjs(request.checkOutDate).diff(dayjs(request.checkInDate), 'day');
  const paymentMutation = useMutation({
    mutationFn: () => createPaymentRequest(booking.bookingId, booking.bookingAccessToken),
    onSuccess: (createdPayment) => {
      storePaymentResultToken(createdPayment.paymentId, booking.bookingAccessToken);
    },
  });
  const payment = paymentMutation.data;
  const paymentStatusQuery = useQuery({
    queryKey: ['payment-status', payment?.paymentId],
    queryFn: () => getPaymentStatus(payment!.paymentId, booking.bookingAccessToken),
    enabled: Boolean(payment?.paymentId),
    refetchInterval: (query) =>
      getPaymentPollingInterval({
        paymentStatus: query.state.data?.paymentStatus,
        refundStatus: query.state.data?.refundStatus,
        error: query.state.error,
        failureCount: query.state.fetchFailureCount,
      }),
  });
  const paymentStatus = paymentStatusQuery.data;
  const paymentCompleted =
    paymentStatus?.paymentStatus === 'SUCCESS' && paymentStatus.bookingStatus === 'CONFIRMED';

  useEffect(() => {
    if (!paymentStatus || paymentStatus.paymentStatus === 'PENDING') return;

    navigate(`/payments/${paymentStatus.paymentId}/result`, { replace: true });
  }, [navigate, paymentStatus]);
  const paymentError =
    paymentMutation.error instanceof ApiClientError
      ? paymentMutation.error.message
      : paymentMutation.error
        ? 'Không thể tạo thông tin thanh toán. Vui lòng thử lại.'
        : undefined;
  const copyText = async (value: string, successMessage: string) => {
    try {
      await navigator.clipboard.writeText(value);
      void message.success(successMessage);
    } catch {
      void message.error('Không thể sao chép tự động. Vui lòng sao chép thủ công.');
    }
  };

  return (
    <section className="page-enter">
      <Flex vertical align="center" gap={4} style={{ marginBottom: 32, textAlign: 'center' }}>
        <Typography.Title level={2} style={{ margin: 0 }}>
          Kiểm tra lại đơn đặt phòng
        </Typography.Title>
        <Typography.Text type="secondary">
          Yêu cầu đặt phòng đã được tạo và hạng phòng đang được giữ cho bạn.
        </Typography.Text>
      </Flex>

      <Row gutter={[28, 28]}>
        <Col xs={24} xl={16}>
          <Card
            title="Tóm tắt đặt phòng"
            extra={
              <Button type="link" onClick={onEdit} disabled={Boolean(payment) || paymentCompleted}>
                Chỉnh sửa thông tin
              </Button>
            }
            style={{ borderColor: token.colorBorder }}
          >
            <Row gutter={[24, 24]}>
              <Col xs={24} md={12}>
                <Typography.Text type="secondary">Thông tin phòng</Typography.Text>
                <Flex gap={14} align="center" style={{ marginTop: 10 }}>
                  {roomType?.imageUrl ? (
                    <img
                      src={roomType.imageUrl}
                      alt={roomType.name}
                      style={{
                        width: 92,
                        height: 72,
                        borderRadius: token.borderRadiusLG,
                        objectFit: 'cover',
                      }}
                    />
                  ) : (
                    <Flex
                      align="center"
                      justify="center"
                      style={{
                        width: 92,
                        height: 72,
                        borderRadius: token.borderRadiusLG,
                        background: token.colorFillAlter,
                        fontSize: 24,
                      }}
                    >
                      <HomeOutlined />
                    </Flex>
                  )}
                  <Flex vertical>
                    <Typography.Text strong>
                      {roomType?.name ?? 'Hạng phòng đã chọn'}
                    </Typography.Text>
                    <Typography.Text type="secondary">
                      {roomType?.bedType ?? roomType?.code}
                    </Typography.Text>
                    <Typography.Text type="secondary">
                      Tối đa {roomType?.maxGuests ?? request.adults + request.children} khách
                    </Typography.Text>
                  </Flex>
                </Flex>
              </Col>
              <Col xs={24} md={12}>
                <Typography.Text type="secondary">Thông tin khách</Typography.Text>
                <Descriptions column={1} size="small" colon={false} style={{ marginTop: 8 }}>
                  <Descriptions.Item label={<UserOutlined />}>
                    {request.guestInfo.fullName}
                  </Descriptions.Item>
                  <Descriptions.Item label="Email">{request.guestInfo.email}</Descriptions.Item>
                  {request.guestInfo.phone && (
                    <Descriptions.Item label="Điện thoại">
                      {request.guestInfo.phone}
                    </Descriptions.Item>
                  )}
                </Descriptions>
              </Col>
              <Col xs={24} md={12}>
                <Typography.Text type="secondary">Ngày lưu trú</Typography.Text>
                <Flex
                  gap={12}
                  align="center"
                  style={{
                    marginTop: 10,
                    padding: 14,
                    borderRadius: token.borderRadiusLG,
                    background: token.colorFillAlter,
                  }}
                >
                  <CalendarOutlined />
                  <Flex vertical>
                    <Typography.Text strong>
                      {dayjs(request.checkInDate).format('DD/MM/YYYY')} –{' '}
                      {dayjs(request.checkOutDate).format('DD/MM/YYYY')}
                    </Typography.Text>
                    <Typography.Text type="secondary">
                      {nights} đêm · {request.adults} người lớn
                      {request.children ? ` · ${request.children} trẻ em` : ''}
                    </Typography.Text>
                  </Flex>
                </Flex>
              </Col>
              <Col xs={24} md={12}>
                <Typography.Text type="secondary">Dịch vụ tùy chọn</Typography.Text>
                {addOns.length ? (
                  <Flex vertical gap="small" style={{ marginTop: 8 }}>
                    {addOns.map((addOn) => (
                      <Flex key={addOn.id} justify="space-between" align="center">
                        <Tag color="blue">
                          {addOn.name} × {addOn.quantity}
                        </Tag>
                        <Typography.Text strong>
                          {formatMoney(addOn.price * addOn.quantity)}
                        </Typography.Text>
                      </Flex>
                    ))}
                  </Flex>
                ) : (
                  <Typography.Paragraph type="secondary" style={{ marginTop: 10 }}>
                    Không có dịch vụ bổ sung
                  </Typography.Paragraph>
                )}
              </Col>
            </Row>
            <Divider />
            <Alert
              type="warning"
              showIcon
              message="Yêu cầu đặt phòng sẽ tự động hết hiệu lực nếu chưa thanh toán trước thời hạn."
            />
            <Checkbox
              checked={acceptedTerms}
              onChange={(event) => setAcceptedTerms(event.target.checked)}
              style={{ marginTop: 20 }}
            >
              Tôi đã đọc và đồng ý với điều khoản đặt phòng và chính sách bảo mật của HANBAY HOTEL.
            </Checkbox>
          </Card>

          <Card title="Thanh toán" style={{ marginTop: 28, borderColor: token.colorBorder }}>
            {paymentCompleted ? (
              <Alert
                type="success"
                showIcon
                message="Thanh toán thành công"
                description="Đặt phòng của bạn đã được xác nhận tự động."
              />
            ) : !acceptedTerms && !payment ? (
              <Alert
                type="info"
                showIcon
                message="Xác nhận điều khoản để tiếp tục"
                description="Sau khi đồng ý điều khoản, bạn có thể tạo mã QR và thực hiện chuyển khoản."
                action={
                  <Button type="primary" disabled>
                    Tạo mã thanh toán
                  </Button>
                }
              />
            ) : acceptedTerms && !payment && !paymentMutation.isPending && !paymentError ? (
              <Flex justify="center" style={{ padding: '20px 0' }}>
                <Button type="primary" size="large" onClick={() => paymentMutation.mutate()}>
                  Tạo mã thanh toán
                </Button>
              </Flex>
            ) : paymentMutation.isPending ? (
              <Flex vertical align="center" gap={16} style={{ padding: '28px 0' }}>
                <LoadingOutlined style={{ fontSize: 36, color: token.colorPrimary }} />
                <Flex vertical align="center" gap={4}>
                  <Typography.Text strong>Đang tạo mã QR thanh toán</Typography.Text>
                  <Typography.Text type="secondary">
                    Hệ thống đang chuẩn bị thông tin chuyển khoản SePay cho booking này.
                  </Typography.Text>
                </Flex>
              </Flex>
            ) : paymentError ? (
              <Alert
                type="error"
                showIcon
                message="Không thể tạo thanh toán"
                description={paymentError}
                action={
                  <Button
                    icon={<ReloadOutlined />}
                    onClick={() => paymentMutation.mutate()}
                    type="primary"
                  >
                    Thử lại
                  </Button>
                }
              />
            ) : payment ? (
              <Flex vertical gap={16}>
                {paymentStatusQuery.isError && (
                  <Alert
                    type="warning"
                    showIcon
                    message="Tạm thời chưa thể cập nhật trạng thái thanh toán"
                    description="Bạn vẫn có thể chuyển khoản. Hệ thống sẽ tiếp tục đối soát hoặc bạn có thể thử lại ngay."
                    action={
                      <Button onClick={() => void paymentStatusQuery.refetch()}>Thử lại</Button>
                    }
                  />
                )}
                <PaymentInstructions payment={payment} onCopy={copyText} />
              </Flex>
            ) : null}
          </Card>
        </Col>

        <Col xs={24} xl={8}>
          <Card
            className={screens.xl ? 'booking-summary-sticky' : undefined}
            title="Chi tiết thanh toán"
            style={{ borderColor: token.colorBorder }}
          >
            <Descriptions column={1} size="small" colon={false}>
              <Descriptions.Item label="Mã đặt phòng">
                <Typography.Text copyable strong>
                  {booking.bookingCode}
                </Typography.Text>
              </Descriptions.Item>
              <Descriptions.Item label="Trạng thái">
                <Tag color={paymentCompleted ? 'success' : 'gold'} icon={<ClockCircleOutlined />}>
                  {paymentCompleted ? 'Đã xác nhận' : 'Chờ thanh toán'}
                </Tag>
              </Descriptions.Item>
            </Descriptions>
            <Divider />
            <Statistic
              title="Tổng tiền cần thanh toán"
              value={booking.totalAmount}
              formatter={(value) => formatMoney(Number(value))}
            />
            <Divider />
            <Statistic.Countdown
              title="Thời gian giữ phòng còn lại"
              value={dayjs(payment?.expiredAt ?? booking.expiredAt).valueOf()}
              format="mm:ss"
              onFinish={() => {
                if (payment) {
                  navigate(`/payments/${payment.paymentId}/result`, { replace: true });
                }
              }}
            />
            <Typography.Paragraph type="secondary" style={{ marginTop: 6 }}>
              Hết hạn lúc{' '}
              {dayjs(payment?.expiredAt ?? booking.expiredAt).format('HH:mm, DD/MM/YYYY')}
            </Typography.Paragraph>
            <Button
              type="primary"
              size="large"
              block
              disabled={!acceptedTerms || (!paymentError && Boolean(payment)) || paymentCompleted}
              loading={paymentMutation.isPending}
              icon={paymentError ? <ReloadOutlined /> : undefined}
              onClick={() => paymentMutation.mutate()}
              style={{ marginTop: 8 }}
            >
              {paymentMutation.isPending
                ? 'Đang tạo thanh toán'
                : paymentCompleted
                  ? 'Đã thanh toán'
                  : paymentError
                    ? 'Thử lại thanh toán'
                    : payment
                      ? 'Đang chờ chuyển khoản'
                      : 'Tạo mã thanh toán'}
            </Button>
            <Flex justify="center" gap={6} style={{ marginTop: 14 }}>
              <SafetyCertificateOutlined />
              <Typography.Text type="secondary" style={{ fontSize: 11 }}>
                Thông tin của bạn được bảo mật
              </Typography.Text>
            </Flex>
            <Divider />
            <Flex gap={10} align="center">
              <CheckCircleOutlined style={{ color: token.colorSuccess }} />
              <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                Hạng phòng của bạn đã được giữ thành công.
              </Typography.Text>
            </Flex>
          </Card>
        </Col>
      </Row>
    </section>
  );
}

interface PaymentInstructionsProps {
  payment: CreatePaymentRequestResponse;
  onCopy: (value: string, successMessage: string) => Promise<void>;
}

function PaymentInstructions({ payment, onCopy }: PaymentInstructionsProps) {
  const { token } = theme.useToken();
  const expiresAt = dayjs(payment.expiredAt);
  const [qrUnavailable, setQrUnavailable] = useState(false);

  return (
    <Row gutter={[24, 24]} align="middle">
      <Col xs={24} md={10}>
        <Flex
          align="center"
          justify="center"
          style={{
            width: '100%',
            padding: 16,
            border: `1px solid ${token.colorBorder}`,
            borderRadius: token.borderRadiusLG,
            background: token.colorBgContainer,
          }}
        >
          {qrUnavailable ? (
            <Alert
              type="warning"
              showIcon
              message="Không tải được mã QR"
              description="Bạn vẫn có thể chuyển khoản bằng thông tin tài khoản bên cạnh."
            />
          ) : (
            <img
              src={payment.paymentUrl}
              alt={`Mã QR thanh toán ${payment.transactionRef}`}
              onError={() => setQrUnavailable(true)}
              style={{
                width: 'min(100%, 260px)',
                aspectRatio: '1 / 1',
                objectFit: 'contain',
              }}
            />
          )}
        </Flex>
      </Col>
      <Col xs={24} md={14}>
        <Flex vertical gap={16}>
          <Alert
            type="success"
            showIcon
            message="Quét QR hoặc chuyển khoản theo thông tin bên dưới"
            description={`Thanh toán trước ${expiresAt.format('HH:mm, DD/MM/YYYY')} để hoàn tất đặt phòng.`}
          />
          <Descriptions column={1} size="small" bordered>
            <Descriptions.Item label="Ngân hàng">{payment.bankCode}</Descriptions.Item>
            <Descriptions.Item label="Số tài khoản">
              <Typography.Text
                copyable={{
                  text: payment.accountNumber,
                  tooltips: ['Sao chép số tài khoản', 'Đã sao chép'],
                  icon: <CopyOutlined />,
                }}
                strong
              >
                {payment.accountNumber}
              </Typography.Text>
            </Descriptions.Item>
            <Descriptions.Item label="Tên tài khoản">{payment.accountName}</Descriptions.Item>
            <Descriptions.Item label="Số tiền">
              <Typography.Text strong>{formatMoney(payment.amount)}</Typography.Text>
            </Descriptions.Item>
            <Descriptions.Item label="Nội dung">
              <Flex wrap="wrap" gap={8} align="center">
                <Typography.Text code strong>
                  {payment.transferContent}
                </Typography.Text>
                <Button
                  size="small"
                  icon={<CopyOutlined />}
                  onClick={() =>
                    void onCopy(payment.transferContent, 'Đã sao chép nội dung chuyển khoản')
                  }
                >
                  Sao chép
                </Button>
              </Flex>
            </Descriptions.Item>
          </Descriptions>
          <Typography.Text type="secondary">
            Mã giao dịch: <Typography.Text code>{payment.transactionRef}</Typography.Text>
          </Typography.Text>
        </Flex>
      </Col>
    </Row>
  );
}

function formatMoney(value: number) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value);
}
