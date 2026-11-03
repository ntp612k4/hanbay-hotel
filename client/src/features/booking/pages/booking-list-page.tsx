import { LeftOutlined } from '@ant-design/icons';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  App,
  Button,
  Card,
  Col,
  Descriptions,
  DatePicker,
  Drawer,
  Empty,
  Flex,
  Image,
  Input,
  Modal,
  Pagination,
  Row,
  Select,
  Skeleton,
  Space,
  Tag,
  Typography,
} from 'antd';
import dayjs from 'dayjs';
import { useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { RefundInformationForm } from '../../payment/components/refund-information-form';
import type { RefundInformation } from '../../payment/types/refund';

import {
  cancelBooking,
  createPaymentRequest,
  getCancellationEligibility,
  getBooking,
  getMyBookings,
  extendStay,
} from '../api/booking-api';
import type {
  BookingHistoryItem,
  BookingHistoryParams,
  BookingPaymentStatus,
  BookingStatus,
  ExtendStayResponse,
} from '../types/booking';
import './booking-list.css';

const PAGE_SIZE = 6;
const money = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' });

const BOOKING_STATUS: Record<BookingStatus, { label: string; color: string }> = {
  PENDING: { label: 'Chờ thanh toán', color: 'orange' },
  CONFIRMED: { label: 'Đã xác nhận', color: 'green' },
  CANCELLED: { label: 'Đã hủy', color: 'default' },
  CHECKED_IN: { label: 'Đã nhận phòng', color: 'blue' },
  CHECKED_OUT: { label: 'Đã trả phòng', color: 'cyan' },
  EXPIRED: { label: 'Đã hết hạn', color: 'default' },
};

const PAYMENT_STATUS: Record<BookingPaymentStatus, { label: string; color: string }> = {
  PENDING: { label: 'Chờ thanh toán', color: 'orange' },
  SUCCESS: { label: 'Đã thanh toán', color: 'green' },
  FAILED: { label: 'Thanh toán thất bại', color: 'red' },
  CANCELLED: { label: 'Đã hủy', color: 'default' },
  REFUNDED: { label: 'Đã hoàn tiền', color: 'purple' },
  EXPIRED: { label: 'Đã hết hạn', color: 'default' },
  PARTIALLY_REFUNDED: { label: 'Hoàn tiền một phần', color: 'purple' },
};

const STATUS_OPTIONS = [
  { value: '', label: 'Tất cả trạng thái' },
  ...Object.entries(BOOKING_STATUS).map(([value, config]) => ({
    value,
    label: config.label,
  })),
];

export function BookingListPage() {
  const navigate = useNavigate();
  const { bookingCode } = useParams<{ bookingCode: string }>();

  return (
    <main className="booking-history-page">
      <Flex align="center" gap="small" className="booking-history-page__heading">
        <Button type="text" icon={<LeftOutlined />} onClick={() => navigate('/')} />
        <div>
          <Typography.Title level={2}>Lịch sử đặt phòng</Typography.Title>
          <Typography.Text type="secondary">
            Theo dõi các lần đặt phòng và trạng thái thanh toán của bạn.
          </Typography.Text>
        </div>
      </Flex>
      <BookingHistorySection reviewBookingCode={bookingCode} />
    </main>
  );
}

export function BookingHistorySection({
  reviewBookingCode,
  openCancellation = false,
}: {
  reviewBookingCode?: string;
  openCancellation?: boolean;
} = {}) {
  const { message } = App.useApp();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [params, setParams] = useState<BookingHistoryParams>({ page: 0, size: PAGE_SIZE });
  const [selectedBooking, setSelectedBooking] = useState<BookingHistoryItem | null>(null);
  const [automaticReviewDismissed, setAutomaticReviewDismissed] = useState(false);
  const [automaticCancellationDismissed, setAutomaticCancellationDismissed] = useState(false);
  const [cancelModalOpen, setCancelModalOpen] = useState(false);
  const [cancelReason, setCancelReason] = useState('');
  const [extendModalOpen, setExtendModalOpen] = useState(false);
  const [newCheckOutDate, setNewCheckOutDate] = useState<string | null>(null);
  const [extensionResult, setExtensionResult] = useState<ExtendStayResponse | null>(null);
  const [refundInformation, setRefundInformation] = useState<RefundInformation>({
    method: 'BANK_TRANSFER',
    bankName: '',
    accountHolderName: '',
    accountNumber: '',
  });

  const historyQuery = useQuery({
    queryKey: ['booking-history', params],
    queryFn: () => getMyBookings(params),
    placeholderData: keepPreviousData,
  });

  const bookings = useMemo(() => historyQuery.data?.content ?? [], [historyQuery.data?.content]);
  const requestedBookingQuery = useQuery({
    queryKey: ['booking-detail', reviewBookingCode],
    queryFn: () => getBooking(reviewBookingCode!),
    enabled: Boolean(reviewBookingCode),
    retry: false,
  });
  const requestedBooking = requestedBookingQuery.data ?? null;
  const selected =
    selectedBooking ?? (!automaticReviewDismissed && reviewBookingCode ? requestedBooking : null);
  const eligibilityQuery = useQuery({
    queryKey: ['booking-cancellation', selected?.bookingCode],
    queryFn: () => getCancellationEligibility(selected!.bookingCode),
    enabled: selected !== null,
    retry: false,
  });
  const cancelMutation = useMutation({
    mutationFn: () =>
      cancelBooking(
        selected!.bookingId,
        cancelReason.trim() || undefined,
        eligibilityQuery.data?.refundAmount ? refundInformation : undefined,
      ),
    onSuccess: async (result) => {
      void message.success(
        result.refundAmount > 0
          ? `Đã hủy đặt phòng. Yêu cầu hoàn ${money.format(result.refundAmount)} đang được xử lý.`
          : 'Hủy đặt phòng thành công.',
      );
      setCancelModalOpen(false);
      setSelectedBooking(null);
      setAutomaticReviewDismissed(true);
      setAutomaticCancellationDismissed(true);
      setCancelReason('');
      setRefundInformation({
        method: 'BANK_TRANSFER',
        bankName: '',
        accountHolderName: '',
        accountNumber: '',
      });
      await queryClient.invalidateQueries({ queryKey: ['booking-history'] });
    },
    onError: (error: Error) => void message.error(error.message),
  });
  const resumePaymentMutation = useMutation({
    mutationFn: (booking: BookingHistoryItem) => createPaymentRequest(booking.bookingId),
    onSuccess: (payment) => navigate(`/payments/${payment.paymentId}/result`),
    onError: (error: Error) => void message.error(error.message),
  });
  const extendMutation = useMutation({
    mutationFn: () => extendStay(selected!.bookingId, newCheckOutDate!),
    onSuccess: async (result) => {
      setExtensionResult(result);
      await queryClient.invalidateQueries({ queryKey: ['booking-history'] });
      await queryClient.invalidateQueries({ queryKey: ['booking-detail'] });
    },
    onError: (error: Error) => void message.error(error.message),
  });
  const extensionPaymentMutation = useMutation({
    mutationFn: () => createPaymentRequest(extensionResult!.bookingId),
    onSuccess: (payment) => navigate(`/payments/${payment.paymentId}/result`),
    onError: (error: Error) => void message.error(error.message),
  });

  const closeExtendModal = () => {
    if (extendMutation.isPending || extensionPaymentMutation.isPending) return;
    setExtendModalOpen(false);
    setNewCheckOutDate(null);
    setExtensionResult(null);
    extendMutation.reset();
    extensionPaymentMutation.reset();
  };

  const cancellationModalOpen =
    cancelModalOpen ||
    Boolean(
      openCancellation &&
      !automaticCancellationDismissed &&
      selected &&
      eligibilityQuery.data?.canCancel,
    );

  return (
    <section aria-label="Lịch sử đặt phòng">
      <Card size="small" className="booking-history-page__filters">
        <Select
          aria-label="Lọc theo trạng thái đặt phòng"
          value={params.status ?? ''}
          options={STATUS_OPTIONS}
          onChange={(value: '' | BookingStatus) =>
            setParams((current) => ({
              ...current,
              page: 0,
              status: value || undefined,
            }))
          }
          className="booking-history-page__status"
        />
      </Card>

      {historyQuery.isLoading && <Skeleton active paragraph={{ rows: 8 }} />}

      {historyQuery.isError && (
        <Alert
          type="error"
          showIcon
          message="Không thể tải lịch sử đặt phòng"
          description={historyQuery.error.message}
          action={<Button onClick={() => void historyQuery.refetch()}>Thử lại</Button>}
        />
      )}

      {requestedBookingQuery.isError && (
        <Alert
          type="error"
          showIcon
          message="Không thể tải chi tiết đặt phòng"
          description="Đặt phòng không tồn tại hoặc không thuộc tài khoản của bạn."
          action={<Button onClick={() => navigate('/account/bookings')}>Về lịch sử</Button>}
        />
      )}

      {!historyQuery.isLoading && !historyQuery.isError && bookings.length === 0 && (
        <Empty description="Bạn chưa có đặt phòng phù hợp" />
      )}

      {bookings.length > 0 && (
        <Space orientation="vertical" size="middle" className="booking-history-page__list">
          {bookings.map((booking) => {
            const bookingStatus = BOOKING_STATUS[booking.bookingStatus];
            return (
              <Card key={booking.bookingId} loading={historyQuery.isFetching}>
                <Row gutter={[20, 16]} align="middle">
                  <Col xs={24} sm={7} md={5}>
                    <Image
                      src={booking.roomType.thumbnailUrl ?? undefined}
                      fallback="data:image/gif;base64,R0lGODlhAQABAAD/ACwAAAAAAQABAAACADs="
                      alt={booking.roomType.name}
                      preview={false}
                      className="booking-history-page__thumbnail"
                    />
                  </Col>
                  <Col xs={24} sm={11} md={13}>
                    <Space orientation="vertical" size={4}>
                      <Typography.Title level={4}>{booking.roomType.name}</Typography.Title>
                      <Typography.Text type="secondary">
                        Mã đặt phòng: {booking.bookingCode}
                      </Typography.Text>
                      <Typography.Text>
                        {dayjs(booking.checkInDate).format('DD/MM/YYYY')} –{' '}
                        {dayjs(booking.checkOutDate).format('DD/MM/YYYY')}
                      </Typography.Text>
                      <Typography.Text type="secondary">
                        {booking.adultCount} người lớn
                        {booking.childCount > 0 ? `, ${booking.childCount} trẻ em` : ''} ·{' '}
                        {booking.totalNights} đêm
                      </Typography.Text>
                    </Space>
                  </Col>
                  <Col xs={24} sm={6} md={6} className="booking-history-page__actions">
                    <Tag color={bookingStatus.color}>{bookingStatus.label}</Tag>
                    <Typography.Text strong>{money.format(booking.totalAmount)}</Typography.Text>
                    {booking.bookingStatus === 'PENDING' && (
                      <Button
                        type="primary"
                        loading={
                          resumePaymentMutation.isPending &&
                          resumePaymentMutation.variables?.bookingId === booking.bookingId
                        }
                        onClick={() => resumePaymentMutation.mutate(booking)}
                      >
                        Tiếp tục thanh toán
                      </Button>
                    )}
                    {(booking.bookingStatus === 'CONFIRMED' ||
                      booking.bookingStatus === 'CHECKED_IN') &&
                      booking.outstandingAmount > 0 && (
                        <Button
                          type="primary"
                          loading={
                            resumePaymentMutation.isPending &&
                            resumePaymentMutation.variables?.bookingId === booking.bookingId
                          }
                          onClick={() => resumePaymentMutation.mutate(booking)}
                        >
                          Thanh toán phần phát sinh
                        </Button>
                      )}
                    <Button
                      onClick={() =>
                        navigate(`/account/bookings/${encodeURIComponent(booking.bookingCode)}`)
                      }
                    >
                      Xem chi tiết
                    </Button>
                  </Col>
                </Row>
              </Card>
            );
          })}

          {historyQuery.data && historyQuery.data.totalElements > PAGE_SIZE && (
            <Pagination
              current={historyQuery.data.page + 1}
              pageSize={historyQuery.data.size}
              total={historyQuery.data.totalElements}
              showSizeChanger={false}
              onChange={(page) => setParams((current) => ({ ...current, page: page - 1 }))}
              align="center"
            />
          )}
        </Space>
      )}

      <Drawer
        title="Chi tiết đặt phòng"
        size={450}
        open={selected !== null}
        onClose={() => {
          setSelectedBooking(null);
          setAutomaticReviewDismissed(true);
          if (reviewBookingCode) navigate('/account/bookings', { replace: true });
        }}
        styles={{
          mask: { backgroundColor: 'rgba(0, 0, 0, 0.45)' },
          body: { backgroundColor: '#ffffff' },
        }}
      >
        {selected && (
          <Space orientation="vertical" size="middle" className="booking-history-page__list">
            <BookingHistoryDetail booking={selected} />
            {(selected.bookingStatus === 'CONFIRMED' ||
              selected.bookingStatus === 'CHECKED_IN') && (
              <Button
                type="primary"
                block
                onClick={() => {
                  setNewCheckOutDate(
                    dayjs(selected.checkOutDate).add(1, 'day').format('YYYY-MM-DD'),
                  );
                  setExtendModalOpen(true);
                }}
              >
                Gia hạn lưu trú
              </Button>
            )}
            {eligibilityQuery.isLoading && <Skeleton active paragraph={{ rows: 2 }} />}
            {eligibilityQuery.isError && (
              <Alert type="error" showIcon message="Không thể kiểm tra chính sách hủy" />
            )}
            {eligibilityQuery.data && (
              <Alert
                showIcon
                type={eligibilityQuery.data.canCancel ? 'info' : 'warning'}
                message={
                  eligibilityQuery.data.canCancel
                    ? 'Đặt phòng có thể hủy'
                    : 'Không thể hủy đặt phòng'
                }
                description={eligibilityQuery.data.policyDescription}
              />
            )}
            <Button
              danger
              type="primary"
              block
              disabled={!eligibilityQuery.data?.canCancel}
              onClick={() => setCancelModalOpen(true)}
            >
              Hủy đặt phòng
            </Button>
          </Space>
        )}
      </Drawer>

      <Modal
        title="Xác nhận hủy đặt phòng"
        open={cancellationModalOpen}
        okText="Xác nhận hủy"
        cancelText="Quay lại"
        okButtonProps={{
          danger: true,
          loading: cancelMutation.isPending,
          disabled:
            Boolean(eligibilityQuery.data?.refundAmount) &&
            (!refundInformation.bankName.trim() ||
              !refundInformation.accountHolderName.trim() ||
              refundInformation.accountNumber.trim().length < 4),
        }}
        onOk={() => cancelMutation.mutate()}
        onCancel={() => {
          setCancelModalOpen(false);
          setAutomaticCancellationDismissed(true);
        }}
      >
        <Space orientation="vertical" size="middle" className="booking-history-page__list">
          <Alert
            type="warning"
            showIcon
            message="Thao tác này không thể hoàn tác"
            description={eligibilityQuery.data?.policyDescription}
          />
          {eligibilityQuery.data && eligibilityQuery.data.refundAmount > 0 && (
            <>
              <Typography.Text strong>
                Số tiền dự kiến hoàn: {money.format(eligibilityQuery.data.refundAmount)} (
                {eligibilityQuery.data.refundPercent}%)
              </Typography.Text>
              <RefundInformationForm
                value={refundInformation}
                onChange={setRefundInformation}
                disabled={cancelMutation.isPending}
              />
            </>
          )}
          <Input.TextArea
            value={cancelReason}
            rows={4}
            maxLength={500}
            showCount
            placeholder="Nhập lý do hủy (không bắt buộc)"
            onChange={(event) => setCancelReason(event.target.value)}
          />
        </Space>
      </Modal>

      <Modal
        title={extensionResult ? 'Gia hạn thành công' : 'Chọn ngày trả phòng mới'}
        open={extendModalOpen}
        destroyOnHidden
        closable={!extendMutation.isPending && !extensionPaymentMutation.isPending}
        maskClosable={false}
        cancelText={extensionResult?.paymentRequired ? 'Thanh toán sau' : 'Đóng'}
        okText={
          extensionResult?.paymentRequired
            ? 'Thanh toán ngay'
            : extensionResult
              ? 'Hoàn tất'
              : 'Xác nhận gia hạn'
        }
        confirmLoading={extendMutation.isPending || extensionPaymentMutation.isPending}
        okButtonProps={{ disabled: !extensionResult && !newCheckOutDate }}
        onCancel={closeExtendModal}
        onOk={() => {
          if (extensionResult?.paymentRequired) extensionPaymentMutation.mutate();
          else if (extensionResult) closeExtendModal();
          else extendMutation.mutate();
        }}
      >
        {selected && !extensionResult && (
          <Space orientation="vertical" size="middle" className="booking-history-page__list">
            <Alert
              showIcon
              type="info"
              message="Khách sạn sẽ giữ nguyên phòng đang được gán"
              description="Nếu phòng hiện tại bận trong thời gian gia hạn, yêu cầu sẽ bị từ chối và ngày trả phòng không thay đổi."
            />
            <Typography.Text>
              Ngày trả phòng hiện tại: {dayjs(selected.checkOutDate).format('DD/MM/YYYY')}
            </Typography.Text>
            <DatePicker
              aria-label="Ngày trả phòng mới"
              value={newCheckOutDate ? dayjs(newCheckOutDate) : null}
              minDate={dayjs(selected.checkOutDate).add(1, 'day')}
              format="DD/MM/YYYY"
              onChange={(date) => setNewCheckOutDate(date?.format('YYYY-MM-DD') ?? null)}
              className="booking-history-page__status"
            />
            <Typography.Text type="secondary">
              Số tiền phát sinh sẽ được tính theo giá từng đêm và cần thanh toán sau khi gia hạn.
            </Typography.Text>
          </Space>
        )}
        {extensionResult && (
          <Space orientation="vertical" size="middle" className="booking-history-page__list">
            <Alert
              showIcon
              type="success"
              message={`Đã gia hạn thêm ${extensionResult.additionalNights} đêm`}
              description={
                extensionResult.retainedRoomId
                  ? 'Phòng vật lý hiện tại được giữ nguyên.'
                  : 'Hạng phòng được giữ nguyên; phòng vật lý sẽ được gán khi nhận phòng.'
              }
            />
            <Descriptions column={1} bordered size="small">
              <Descriptions.Item label="Ngày trả phòng mới">
                {dayjs(extensionResult.newCheckOutDate).format('DD/MM/YYYY')}
              </Descriptions.Item>
              <Descriptions.Item label="Tiền phát sinh">
                {money.format(extensionResult.additionalAmount)}
              </Descriptions.Item>
              <Descriptions.Item label="Tổng mới">
                {money.format(extensionResult.newTotalAmount)}
              </Descriptions.Item>
              <Descriptions.Item label="Cần thanh toán">
                <Typography.Text
                  strong
                  type={extensionResult.outstandingAmount > 0 ? 'danger' : undefined}
                >
                  {money.format(extensionResult.outstandingAmount)}
                </Typography.Text>
              </Descriptions.Item>
            </Descriptions>
            {extensionResult.paymentRequired && (
              <Alert
                showIcon
                type="warning"
                message="Gia hạn đã được ghi nhận, vui lòng thanh toán phần phát sinh"
                description="Bạn có thể thanh toán ngay hoặc quay lại lịch sử đặt phòng để tiếp tục thanh toán sau."
              />
            )}
          </Space>
        )}
      </Modal>
    </section>
  );
}

function BookingHistoryDetail({ booking }: { booking: BookingHistoryItem }) {
  const bookingStatus = BOOKING_STATUS[booking.bookingStatus];
  const paymentStatus = booking.paymentStatus ? PAYMENT_STATUS[booking.paymentStatus] : null;

  return (
    <Descriptions column={1} bordered size="small">
      <Descriptions.Item label="Mã đặt phòng">{booking.bookingCode}</Descriptions.Item>
      <Descriptions.Item label="Loại phòng">{booking.roomType.name}</Descriptions.Item>
      <Descriptions.Item label="Trạng thái đặt phòng">
        <Tag color={bookingStatus.color}>{bookingStatus.label}</Tag>
      </Descriptions.Item>
      <Descriptions.Item label="Trạng thái thanh toán">
        {paymentStatus ? (
          <Tag color={paymentStatus.color}>{paymentStatus.label}</Tag>
        ) : (
          'Chưa có giao dịch'
        )}
      </Descriptions.Item>
      {booking.refundStatus && (
        <Descriptions.Item label="Hoàn tiền">
          <Space>
            <Tag color={booking.refundStatus === 'SUCCESS' ? 'green' : 'orange'}>
              {booking.refundStatus === 'SUCCESS' ? 'Đã hoàn tiền' : 'Đang xử lý'}
            </Tag>
            {booking.refundAmount ? money.format(booking.refundAmount) : null}
          </Space>
        </Descriptions.Item>
      )}
      <Descriptions.Item label="Ngày nhận phòng">
        {dayjs(booking.checkInDate).format('DD/MM/YYYY')}
      </Descriptions.Item>
      <Descriptions.Item label="Ngày trả phòng">
        {dayjs(booking.checkOutDate).format('DD/MM/YYYY')}
      </Descriptions.Item>
      <Descriptions.Item label="Số khách">
        {booking.adultCount} người lớn
        {booking.childCount > 0 ? `, ${booking.childCount} trẻ em` : ''}
      </Descriptions.Item>
      <Descriptions.Item label="Số đêm">{booking.totalNights}</Descriptions.Item>
      <Descriptions.Item label="Tổng tiền">
        <Typography.Text strong>{money.format(booking.totalAmount)}</Typography.Text>
      </Descriptions.Item>
      {booking.outstandingAmount > 0 && (
        <Descriptions.Item label="Còn phải thanh toán">
          <Typography.Text type="danger" strong>
            {money.format(booking.outstandingAmount)}
          </Typography.Text>
        </Descriptions.Item>
      )}
      <Descriptions.Item label="Ngày tạo">
        {dayjs(booking.createdAt).format('DD/MM/YYYY HH:mm')}
      </Descriptions.Item>
    </Descriptions>
  );
}

export default BookingListPage;
