import {
  CheckCircleOutlined,
  ClockCircleOutlined,
  CloseCircleOutlined,
  CopyOutlined,
  HomeOutlined,
  InfoCircleOutlined,
  ReloadOutlined,
  SearchOutlined,
} from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import {
  Button,
  Card,
  Col,
  Descriptions,
  Divider,
  Flex,
  Image,
  Result,
  Row,
  Spin,
  Statistic,
  Tag,
  Typography,
  Alert,
} from 'antd';
import dayjs from 'dayjs';
import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';

import { MainFooter } from '@components/layout/MainFooter';
import { MainHeader } from '@components/layout/MainHeader';
import { useAuth } from '@features/authentication/providers/auth-context';

import { ApiClientError } from '../../../types/api';
import { getPaymentStatus } from '../api/booking-api';
import type { PaymentStatusResponse } from '../types/booking';
import { clearLatestPaymentResult, getPaymentResultToken } from '../utils/payment-result-storage';
import { getPaymentPollingInterval } from '../utils/payment-status-polling';
import './payment-result-page.css';

type ResultStatus = 'success' | 'error' | 'warning' | 'info';

interface ResultPresentation {
  status: ResultStatus;
  title: string;
  description: string;
}

export function PaymentResultPage() {
  const { isAuthenticated, roles } = useAuth();
  const { paymentId } = useParams<{ paymentId: string }>();
  const bookingAccessToken = paymentId ? getPaymentResultToken(paymentId) : null;
  const paymentQuery = useQuery({
    queryKey: ['payment-status', paymentId, bookingAccessToken ?? 'owner'],
    queryFn: () => getPaymentStatus(paymentId!, bookingAccessToken ?? undefined),
    enabled: Boolean(paymentId),
    refetchInterval: (query) =>
      getPaymentPollingInterval({
        paymentStatus: query.state.data?.paymentStatus,
        refundStatus: query.state.data?.refundStatus,
        error: query.state.error,
        failureCount: query.state.fetchFailureCount,
      }),
  });

  useEffect(() => {
    if (paymentId && paymentQuery.data && paymentQuery.data.paymentStatus !== 'PENDING') {
      clearLatestPaymentResult(paymentId);
    }
  }, [paymentId, paymentQuery.data]);

  return (
    <div className="booking-public-shell">
      <MainHeader />
      <main className="payment-result-page">
        <div className="payment-result-page__glow" aria-hidden="true" />
        <section className="payment-result-page__content" aria-live="polite">
          {!paymentId ? (
            <UnavailablePaymentResult />
          ) : paymentQuery.isLoading ? (
            <LoadingPaymentResult />
          ) : paymentQuery.isError ? (
            <ErrorPaymentResult
              error={paymentQuery.error}
              onRetry={() => void paymentQuery.refetch()}
            />
          ) : paymentQuery.data?.paymentStatus === 'PENDING' ? (
            <PendingPaymentResult payment={paymentQuery.data} />
          ) : paymentQuery.data ? (
            <CompletedPaymentResult
              payment={paymentQuery.data}
              canViewBooking={isAuthenticated && roles.length === 0}
            />
          ) : (
            <UnavailablePaymentResult />
          )}
        </section>
      </main>
      <MainFooter />
    </div>
  );
}

function LoadingPaymentResult() {
  return (
    <Card className="payment-result-card">
      <Result
        icon={<Spin size="large" />}
        title="Đang kiểm tra kết quả thanh toán"
        subTitle="Vui lòng chờ trong giây lát, hệ thống đang đối chiếu giao dịch của bạn."
      />
    </Card>
  );
}

function PendingPaymentResult({ payment }: { payment: PaymentStatusResponse }) {
  const [qrLoadFailed, setQrLoadFailed] = useState(false);

  return (
    <Card className="payment-result-card">
      <Result
        icon={<ClockCircleOutlined className="payment-result-icon payment-result-icon--pending" />}
        title="Đang chờ xác nhận thanh toán"
        subTitle="Hệ thống sẽ tự động cập nhật ngay khi ngân hàng xác nhận giao dịch."
      />
      <Divider />
      <Row gutter={[24, 24]} align="middle">
        <Col xs={24} md={10}>
          <div className="payment-result-qr">
            {!qrLoadFailed && payment.paymentUrl ? (
              <Image
                src={payment.paymentUrl}
                alt={`Mã QR thanh toán ${payment.transactionRef}`}
                preview={false}
                width="100%"
                onError={() => setQrLoadFailed(true)}
              />
            ) : (
              <Alert
                type="warning"
                showIcon
                title="Không tải được mã QR"
                description="Bạn vẫn có thể chuyển khoản bằng thông tin bên cạnh. Hãy nhập đúng số tiền và nội dung chuyển khoản."
              />
            )}
          </div>
        </Col>
        <Col xs={24} md={14}>
          <Descriptions column={1} size="small" bordered>
            <Descriptions.Item label="Ngân hàng">{payment.bankCode}</Descriptions.Item>
            <Descriptions.Item label="Số tài khoản">
              <Typography.Text copyable strong>
                {payment.accountNumber}
              </Typography.Text>
            </Descriptions.Item>
            <Descriptions.Item label="Tên tài khoản">{payment.accountName}</Descriptions.Item>
            <Descriptions.Item label="Số tiền">
              {formatMoney(payment.amount, payment.currency)}
            </Descriptions.Item>
            <Descriptions.Item label="Nội dung chuyển khoản">
              <Typography.Text
                code
                strong
                copyable={{
                  text: payment.transferContent,
                  icon: <CopyOutlined />,
                  tooltips: ['Sao chép nội dung', 'Đã sao chép'],
                }}
              >
                {payment.transferContent}
              </Typography.Text>
            </Descriptions.Item>
          </Descriptions>
        </Col>
      </Row>
      <PaymentResultDetails payment={payment} />
    </Card>
  );
}

function CompletedPaymentResult({
  payment,
  canViewBooking,
}: {
  payment: PaymentStatusResponse;
  canViewBooking: boolean;
}) {
  const presentation = getResultPresentation(payment);

  return (
    <Card className={`payment-result-card payment-result-card--${presentation.status}`}>
      <Result
        status={presentation.status}
        title={presentation.title}
        subTitle={presentation.description}
        extra={[
          ...(canViewBooking
            ? [
                <Link
                  to={`/account/bookings/${encodeURIComponent(payment.bookingCode)}`}
                  key="booking"
                >
                  <Button type="primary">Xem chi tiết đặt phòng</Button>
                </Link>,
              ]
            : []),
          <Link to="/" key="home">
            <Button icon={<HomeOutlined />}>Về trang chủ</Button>
          </Link>,
          <Link to="/rooms/search" key="rooms">
            <Button type={canViewBooking ? 'default' : 'primary'} icon={<SearchOutlined />}>
              Đặt phòng khác
            </Button>
          </Link>,
        ]}
      />
      <PaymentResultDetails payment={payment} />
    </Card>
  );
}

function ErrorPaymentResult({ error, onRetry }: { error: unknown; onRetry: () => void }) {
  const message =
    error instanceof ApiClientError
      ? error.message
      : 'Không thể tải kết quả thanh toán. Vui lòng thử lại.';

  return (
    <Card className="payment-result-card payment-result-card--error">
      <Result
        status="error"
        title="Không thể kiểm tra giao dịch"
        subTitle={message}
        extra={[
          <Button key="retry" type="primary" icon={<ReloadOutlined />} onClick={onRetry}>
            Thử lại
          </Button>,
          <Link to="/rooms/search" key="rooms">
            <Button>Tìm phòng</Button>
          </Link>,
        ]}
      />
    </Card>
  );
}

function UnavailablePaymentResult() {
  return (
    <Card className="payment-result-card payment-result-card--warning">
      <Result
        status="warning"
        title="Không tìm thấy phiên thanh toán"
        subTitle="Liên kết không hợp lệ hoặc phiên trình duyệt ẩn danh đã kết thúc. Mã truy cập thanh toán không thể khôi phục trên trình duyệt khác; hãy dùng lại trình duyệt ban đầu, đăng nhập nếu đã có tài khoản hoặc tạo đặt phòng mới."
        extra={
          <Link to="/rooms/search">
            <Button type="primary" icon={<SearchOutlined />}>
              Tìm phòng
            </Button>
          </Link>
        }
      />
    </Card>
  );
}

function PaymentResultDetails({ payment }: { payment: PaymentStatusResponse }) {
  const successful = payment.paymentStatus === 'SUCCESS';
  const informational =
    payment.paymentStatus === 'REFUNDED' || payment.paymentStatus === 'PARTIALLY_REFUNDED';

  return (
    <div className="payment-result-details">
      <Flex justify="space-between" align="center" wrap="wrap" gap={12}>
        <div>
          <Typography.Text className="payment-result-details__eyebrow">
            HANBAY HOTEL
          </Typography.Text>
          <Typography.Title level={2}>Thông tin giao dịch</Typography.Title>
        </div>
        <Tag
          color={successful ? 'success' : getPaymentStatusColor(payment.paymentStatus)}
          icon={
            successful ? (
              <CheckCircleOutlined />
            ) : informational ? (
              <InfoCircleOutlined />
            ) : (
              <CloseCircleOutlined />
            )
          }
        >
          {getPaymentStatusLabel(payment.paymentStatus)}
        </Tag>
      </Flex>
      <Divider />
      <Descriptions column={{ xs: 1, sm: 2 }} colon={false}>
        <Descriptions.Item label="Hạng phòng">
          <strong>{payment.roomTypeName}</strong>
        </Descriptions.Item>
        <Descriptions.Item label="Mã đặt phòng">
          <strong>{payment.bookingCode}</strong>
        </Descriptions.Item>
        <Descriptions.Item label="Mã giao dịch">
          <span className="payment-result-details__reference">{payment.transactionRef}</span>
        </Descriptions.Item>
        <Descriptions.Item label="Trạng thái đặt phòng">
          {getBookingStatusLabel(payment.bookingStatus)}
        </Descriptions.Item>
        <Descriptions.Item label="Thời gian lưu trú">
          {dayjs(payment.checkInDate).format('DD/MM/YYYY')} –{' '}
          {dayjs(payment.checkOutDate).format('DD/MM/YYYY')} ({payment.totalNights} đêm)
        </Descriptions.Item>
        <Descriptions.Item label="Số khách">
          {payment.adultCount} người lớn
          {payment.childCount > 0 ? `, ${payment.childCount} trẻ em` : ''}
        </Descriptions.Item>
        <Descriptions.Item label="Thời điểm thanh toán">
          {payment.paidAt ? dayjs(payment.paidAt).format('HH:mm, DD/MM/YYYY') : 'Chưa ghi nhận'}
        </Descriptions.Item>
        {payment.refundStatus && (
          <Descriptions.Item label="Trạng thái hoàn tiền">
            {getRefundStatusLabel(payment.refundStatus)}
          </Descriptions.Item>
        )}
        {payment.refundReason && (
          <Descriptions.Item label="Lý do hoàn tiền">{payment.refundReason}</Descriptions.Item>
        )}
      </Descriptions>
      <div className="payment-result-details__amounts">
        <Statistic
          title="Số tiền cần thanh toán"
          value={payment.amount}
          formatter={(value) => formatMoney(Number(value), payment.currency)}
        />
        <Statistic
          title="Số tiền đã ghi nhận"
          value={payment.paidAmount}
          formatter={(value) => formatMoney(Number(value), payment.currency)}
        />
        {payment.refundAmount !== null && (
          <Statistic
            title="Số tiền hoàn"
            value={payment.refundAmount}
            formatter={(value) => formatMoney(Number(value), payment.currency)}
          />
        )}
      </div>
    </div>
  );
}

function getResultPresentation(payment: PaymentStatusResponse): ResultPresentation {
  if (payment.refundStatus === 'PENDING' || payment.refundStatus === 'PROCESSING') {
    return {
      status: 'info',
      title:
        payment.bookingStatus === 'CONFIRMED'
          ? 'Thanh toán thành công, đang xử lý hoàn tiền'
          : 'Đã nhận tiền, đang xử lý hoàn tiền',
      description:
        payment.refundReason ??
        'Hệ thống đã ghi nhận yêu cầu hoàn tiền. Khách sạn sẽ cập nhật khi giao dịch hoàn tất.',
    };
  }

  if (payment.refundStatus === 'FAILED') {
    return {
      status: 'error',
      title: 'Hoàn tiền chưa thành công',
      description: 'Vui lòng liên hệ khách sạn để được hỗ trợ xử lý khoản tiền đã nhận.',
    };
  }

  if (payment.refundStatus === 'SUCCESS' && payment.bookingStatus === 'CONFIRMED') {
    return {
      status: 'success',
      title: 'Thanh toán thành công',
      description: 'Đặt phòng đã được xác nhận và phần tiền dư đã được hoàn lại.',
    };
  }

  if (payment.refundStatus === 'SUCCESS') {
    return {
      status: 'info',
      title: 'Khoản tiền đã được hoàn lại',
      description: 'Khoản tiền đã nhận cho đặt phòng không thể xác nhận đã được hoàn lại.',
    };
  }

  if (payment.refundStatus === 'CANCELLED') {
    return {
      status: 'warning',
      title: 'Yêu cầu hoàn tiền đã bị hủy',
      description: 'Vui lòng liên hệ khách sạn để kiểm tra và xử lý khoản tiền đã nhận.',
    };
  }

  if (payment.paymentStatus === 'SUCCESS' && payment.bookingStatus === 'CONFIRMED') {
    return {
      status: 'success',
      title: 'Thanh toán thành công',
      description: `Đặt phòng ${payment.bookingCode} đã được xác nhận. Cảm ơn bạn đã lựa chọn HANBAY HOTEL.`,
    };
  }

  if (payment.paymentStatus === 'REFUNDED' || payment.paymentStatus === 'PARTIALLY_REFUNDED') {
    return {
      status: 'info',
      title: 'Giao dịch đã được hoàn tiền',
      description:
        payment.paymentStatus === 'REFUNDED'
          ? 'Toàn bộ số tiền của giao dịch đã được hoàn lại.'
          : 'Một phần số tiền của giao dịch đã được hoàn lại.',
    };
  }

  if (payment.paymentStatus === 'EXPIRED' || payment.bookingStatus === 'EXPIRED') {
    return {
      status: 'warning',
      title: 'Yêu cầu thanh toán đã hết hạn',
      description:
        'Thời gian giữ phòng đã kết thúc. Vui lòng tìm phòng và thực hiện lại giao dịch.',
    };
  }

  if (payment.paymentStatus === 'CANCELLED' || payment.bookingStatus === 'CANCELLED') {
    return {
      status: 'warning',
      title: 'Thanh toán đã bị hủy',
      description: 'Giao dịch không còn hiệu lực và đặt phòng chưa được xác nhận.',
    };
  }

  return {
    status: 'error',
    title: 'Thanh toán không thành công',
    description:
      'Ngân hàng chưa ghi nhận giao dịch. Vui lòng kiểm tra lại hoặc tạo một đặt phòng mới.',
  };
}

function getRefundStatusLabel(status: NonNullable<PaymentStatusResponse['refundStatus']>) {
  const labels: Record<NonNullable<PaymentStatusResponse['refundStatus']>, string> = {
    PENDING: 'Chờ xử lý',
    PROCESSING: 'Đang xử lý',
    SUCCESS: 'Đã hoàn tiền',
    FAILED: 'Hoàn tiền thất bại',
    CANCELLED: 'Đã hủy hoàn tiền',
  };
  return labels[status];
}

function getPaymentStatusLabel(status: PaymentStatusResponse['paymentStatus']) {
  const labels: Record<PaymentStatusResponse['paymentStatus'], string> = {
    PENDING: 'Đang xử lý',
    SUCCESS: 'Đã thanh toán',
    FAILED: 'Thất bại',
    CANCELLED: 'Đã hủy',
    REFUNDED: 'Đã hoàn tiền',
    EXPIRED: 'Hết hạn',
    PARTIALLY_REFUNDED: 'Hoàn tiền một phần',
  };
  return labels[status];
}

function getPaymentStatusColor(status: PaymentStatusResponse['paymentStatus']) {
  if (status === 'PENDING') return 'processing';
  if (status === 'SUCCESS') return 'success';
  if (status === 'EXPIRED' || status === 'CANCELLED') return 'warning';
  if (status === 'REFUNDED' || status === 'PARTIALLY_REFUNDED') return 'blue';
  return 'error';
}

function getBookingStatusLabel(status: PaymentStatusResponse['bookingStatus']) {
  const labels: Record<PaymentStatusResponse['bookingStatus'], string> = {
    PENDING: 'Chờ thanh toán',
    CONFIRMED: 'Đã xác nhận',
    CANCELLED: 'Đã hủy',
    CHECKED_IN: 'Đã nhận phòng',
    CHECKED_OUT: 'Đã trả phòng',
    EXPIRED: 'Hết hạn',
  };
  return labels[status];
}

function formatMoney(value: number, currency: string) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency }).format(value);
}
