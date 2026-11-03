import {
  CalendarOutlined,
  CheckCircleOutlined,
  DollarOutlined,
  PhoneOutlined,
  ReloadOutlined,
  TeamOutlined,
} from '@ant-design/icons';
import {
  Alert,
  App,
  Button,
  Card,
  Col,
  Empty,
  DatePicker,
  Descriptions,
  Form,
  Input,
  InputNumber,
  Modal,
  Row,
  Space,
  Statistic,
  Table,
  Tag,
  Typography,
  type TableColumnsType,
} from 'antd';
import dayjs from 'dayjs';
import { useEffect, useState } from 'react';

import {
  useCheckOutGuest,
  useEarlyCheckout,
  useEarlyCheckoutQuote,
  useTodayDepartures,
} from '../api/today-departure-query';
import type { TodayDeparture } from '../types/today-departure';
import { RefundInformationForm } from '../../payment/components/refund-information-form';
import type { RefundInformation } from '../../payment/types/refund';
import { getCheckOutErrorMessage } from '../utils/check-out-error-mapper';
import { ApiClientError } from '../../../types/api';
import './today-departures-page.css';

const { Paragraph, Text, Title } = Typography;
const MAX_CHECKOUT_AMOUNT = 9_999_999_999;

const PAGE_SIZE = 20;
const HOTEL_TIME_ZONE = 'Asia/Ho_Chi_Minh';
const money = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' });
const hotelDate = new Intl.DateTimeFormat('vi-VN', {
  timeZone: HOTEL_TIME_ZONE,
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
});

const formatDate = (value: string) => dayjs(value).format('DD/MM/YYYY');

interface CheckOutFormValues {
  note?: string;
  additionalChargeAmount?: number;
  additionalChargeReason?: string;
  collectedAmount?: number;
}

export function TodayDeparturesPage() {
  const { message } = App.useApp();
  const [form] = Form.useForm<CheckOutFormValues>();
  const [page, setPage] = useState(0);
  const [selectedSnapshot, setSelectedDeparture] = useState<TodayDeparture | null>(null);
  const additionalChargeAmount = Form.useWatch('additionalChargeAmount', form) ?? 0;
  const collectedAmount = Form.useWatch('collectedAmount', form) ?? 0;
  const [earlyDate, setEarlyDate] = useState<string | undefined>();
  const [refundInformation, setRefundInformation] = useState<RefundInformation>({
    method: 'BANK_TRANSFER',
    bankName: '',
    accountHolderName: '',
    accountNumber: '',
  });
  const departuresQuery = useTodayDepartures({ page, size: PAGE_SIZE });
  const checkOutMutation = useCheckOutGuest();
  const earlyCheckoutMutation = useEarlyCheckout();
  const earlyCheckoutQuote = useEarlyCheckoutQuote(selectedSnapshot?.bookingId, earlyDate);
  const todayAtHotel = dayjs(
    new Date().toLocaleString('en-US', { timeZone: HOTEL_TIME_ZONE }),
  ).startOf('day');
  const pageContent = departuresQuery.data?.content;
  const departures = Array.isArray(pageContent) ? pageContent : [];
  const selectedDeparture =
    departures.find((departure) => departure.bookingId === selectedSnapshot?.bookingId) ??
    selectedSnapshot;
  const submitting = checkOutMutation.isPending || earlyCheckoutMutation.isPending;
  const outstandingOnPage = departures.reduce(
    (total, departure) => total + departure.outstandingAmount,
    0,
  );

  useEffect(() => {
    const totalPages = departuresQuery.data?.totalPages;
    if (totalPages !== undefined && page > 0 && page >= totalPages) {
      // Đồng bộ state phân trang với dữ liệu máy chủ sau khi trang hiện tại không còn tồn tại.
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setPage(Math.max(totalPages - 1, 0));
    }
  }, [departuresQuery.data?.totalPages, page]);

  const closeCheckOutModal = () => {
    setSelectedDeparture(null);
    form.resetFields();
    checkOutMutation.reset();
    earlyCheckoutMutation.reset();
    setEarlyDate(undefined);
    setRefundInformation({
      method: 'BANK_TRANSFER',
      bankName: '',
      accountHolderName: '',
      accountNumber: '',
    });
  };

  const selectedIsEarly = Boolean(
    selectedDeparture && todayAtHotel.isBefore(dayjs(selectedDeparture.checkOutDate), 'day'),
  );
  const finalTotalAmount = selectedDeparture
    ? selectedDeparture.totalAmount + Number(additionalChargeAmount)
    : 0;
  const finalPaidAmount = selectedDeparture
    ? selectedDeparture.paidAmount + Number(collectedAmount)
    : 0;
  const amountToCollect = Math.max(finalTotalAmount - (selectedDeparture?.paidAmount ?? 0), 0);
  const overCollection = Number(collectedAmount) > amountToCollect;
  const finalOutstandingAmount = Math.max(finalTotalAmount - finalPaidAmount, 0);
  const earlyBaseRefund = earlyCheckoutQuote.data?.refundAmount ?? 0;
  const earlyBalanceBeforeRefund =
    (selectedDeparture?.outstandingAmount ?? 0) + Number(additionalChargeAmount);
  const adjustedEarlyRefund = Math.max(earlyBaseRefund - earlyBalanceBeforeRefund, 0);
  const earlyRequiredCollection = Math.max(earlyBalanceBeforeRefund - earlyBaseRefund, 0);
  const earlyCollectionInvalid = Number(collectedAmount) !== earlyRequiredCollection;
  const submitEarlyCheckout = async () => {
    if (!selectedDeparture || !earlyDate || !earlyCheckoutQuote.data || submitting) return;
    let values: CheckOutFormValues;
    try {
      values = await form.validateFields();
    } catch {
      return;
    }
    earlyCheckoutMutation.mutate(
      {
        bookingId: selectedDeparture.bookingId,
        payload: {
          actualCheckOutDate: earlyDate,
          note: values.note?.trim() || undefined,
          additionalChargeAmount: values.additionalChargeAmount || undefined,
          additionalChargeReason: values.additionalChargeReason?.trim() || undefined,
          collectedAmount: values.collectedAmount || undefined,
          refundInformation: adjustedEarlyRefund > 0 ? refundInformation : undefined,
        },
      },
      {
        onSuccess: (data) => {
          void message.success(
            data.refundAmount > 0
              ? `Trả phòng sớm thành công. Yêu cầu hoàn ${money.format(data.refundAmount)} đang được xử lý.`
              : 'Trả phòng sớm thành công.',
          );
          closeCheckOutModal();
        },
        onError: (error) => void message.error(getCheckOutErrorMessage(error)),
      },
    );
  };

  const submitCheckOut = async () => {
    if (!selectedDeparture || submitting) return;

    let values: CheckOutFormValues;
    try {
      values = await form.validateFields();
    } catch {
      return;
    }
    const note = values.note?.trim();
    const additionalChargeReason = values.additionalChargeReason?.trim();
    const payload = {
      ...(note ? { note } : {}),
      ...(values.additionalChargeAmount
        ? { additionalChargeAmount: values.additionalChargeAmount }
        : {}),
      ...(additionalChargeReason ? { additionalChargeReason } : {}),
      ...(values.collectedAmount ? { collectedAmount: values.collectedAmount } : {}),
    };

    checkOutMutation.mutate(
      {
        bookingId: selectedDeparture.bookingId,
        payload: Object.keys(payload).length > 0 ? payload : undefined,
      },
      {
        onSuccess: (data) => {
          void message.success(`Trả phòng thành công, phòng ${data.room.roomNumber} cần dọn.`);
          closeCheckOutModal();
        },
        onError: (error) => {
          if (error instanceof ApiClientError && error.data?.errors) {
            const fieldErrors = error.data.errors;
            const fields: (keyof CheckOutFormValues)[] = [
              'note',
              'additionalChargeAmount',
              'additionalChargeReason',
              'collectedAmount',
            ];
            form.setFields(
              fields.flatMap((name) => {
                const errors =
                  fieldErrors[name] ??
                  (name === 'additionalChargeReason'
                    ? fieldErrors.additionalChargeReasonValid
                    : undefined);
                return errors ? [{ name, errors: Array.isArray(errors) ? errors : [errors] }] : [];
              }),
            );
          }
          void message.error(getCheckOutErrorMessage(error));
        },
      },
    );
  };

  const columns: TableColumnsType<TodayDeparture> = [
    {
      title: 'Mã đặt phòng',
      dataIndex: 'bookingCode',
      fixed: 'left',
      width: 150,
      render: (value: string) => <Text code>{value}</Text>,
    },
    {
      title: 'Khách chính',
      key: 'guest',
      width: 190,
      render: (_, departure) => (
        <Space className="today-departures-page__cell-stack" orientation="vertical" size={2}>
          <Text strong>{departure.guestName}</Text>
          <Text type="secondary">
            <PhoneOutlined /> {departure.guestPhone ?? 'Chưa cập nhật số điện thoại'}
          </Text>
        </Space>
      ),
    },
    {
      title: 'Hạng phòng',
      dataIndex: 'roomTypeName',
      width: 170,
      render: (value: string) => <Text>{value}</Text>,
    },
    {
      title: 'Lưu trú',
      key: 'stay',
      width: 170,
      render: (_, departure) => (
        <Space className="today-departures-page__cell-stack" orientation="vertical" size={2}>
          <Text>{formatDate(departure.checkInDate)}</Text>
          <Text type="secondary">Trả phòng: {formatDate(departure.checkOutDate)}</Text>
        </Space>
      ),
    },
    {
      title: 'Số khách',
      key: 'guests',
      width: 150,
      render: (_, departure) => (
        <div className="today-departures-page__guest-count">
          <span>
            <TeamOutlined /> {departure.adults} người lớn
          </span>
          {departure.children > 0 && (
            <span className="today-departures-page__guest-count-children">
              {departure.children} trẻ em
            </span>
          )}
        </div>
      ),
    },
    {
      title: 'Trạng thái',
      key: 'status',
      width: 145,
      render: (_, departure) =>
        dayjs(departure.checkOutDate).isBefore(todayAtHotel, 'day') ? (
          <Tag color="error">Quá hạn trả phòng</Tag>
        ) : (
          <Tag color="processing">Đang lưu trú</Tag>
        ),
    },
    {
      title: 'Tổng tiền',
      dataIndex: 'totalAmount',
      align: 'right',
      width: 145,
      className: 'today-departures-page__money-cell',
      render: (value: number) => money.format(value),
    },
    {
      title: 'Đã thanh toán',
      dataIndex: 'paidAmount',
      align: 'right',
      width: 155,
      className: 'today-departures-page__money-cell',
      render: (value: number) => <Text type="success">{money.format(value)}</Text>,
    },
    {
      title: 'Còn phải thu',
      dataIndex: 'outstandingAmount',
      align: 'right',
      width: 165,
      className: 'today-departures-page__money-cell',
      render: (value: number) =>
        value > 0 ? (
          <Text type="danger">{money.format(value)}</Text>
        ) : (
          <Tag color="success">Đã đủ</Tag>
        ),
    },
    {
      title: 'Thao tác',
      key: 'actions',
      align: 'right',
      fixed: 'right',
      width: 155,
      render: (_, departure) => (
        <Space orientation="vertical" size={4} align="end">
          <Button
            type="primary"
            icon={<CheckCircleOutlined />}
            disabled={submitting}
            loading={
              checkOutMutation.isPending && selectedDeparture?.bookingId === departure.bookingId
            }
            onClick={() => {
              setSelectedDeparture(departure);
              if (todayAtHotel.isBefore(dayjs(departure.checkOutDate), 'day'))
                setEarlyDate(todayAtHotel.format('YYYY-MM-DD'));
              form.resetFields();
              form.setFieldsValue({
                additionalChargeAmount: 0,
                collectedAmount: 0,
              });
              checkOutMutation.reset();
            }}
          >
            {todayAtHotel.isBefore(dayjs(departure.checkOutDate), 'day')
              ? 'Trả phòng sớm'
              : 'Trả phòng'}
          </Button>
          {departure.outstandingAmount > 0 && (
            <Text type="secondary" className="today-departures-page__action-note">
              Cần thu thêm
            </Text>
          )}
        </Space>
      ),
    },
  ];

  return (
    <div className="today-departures-page">
      <div className="today-departures-page__header">
        <div>
          <Text className="today-departures-page__eyebrow">VẬN HÀNH LỄ TÂN</Text>
          <Title level={2}>Khách đang lưu trú</Title>
          <Paragraph type="secondary">
            Theo dõi toàn bộ khách đang ở, trả phòng đúng hạn hoặc xử lý trả phòng sớm tính đến ngày{' '}
            {hotelDate.format(new Date())}.
          </Paragraph>
        </div>
        <Button
          icon={<ReloadOutlined />}
          loading={departuresQuery.isFetching}
          onClick={() => void departuresQuery.refetch()}
        >
          Làm mới
        </Button>
      </div>

      <Row gutter={[16, 16]} className="today-departures-page__summary">
        <Col xs={24} md={8}>
          <Card>
            <Statistic
              title="Tổng khách đang lưu trú"
              value={departuresQuery.data?.totalElements ?? 0}
              suffix="lượt đặt phòng"
              prefix={<CalendarOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card>
            <Statistic
              title="Đang hiển thị"
              value={departures.length}
              suffix="khách"
              prefix={<TeamOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card>
            <Statistic
              title="Còn phải thu trên trang"
              value={outstandingOnPage}
              formatter={(value) => money.format(Number(value))}
              prefix={<DollarOutlined />}
              styles={outstandingOnPage > 0 ? { content: { color: '#c2410c' } } : undefined}
            />
          </Card>
        </Col>
      </Row>

      {departuresQuery.isError && (
        <Alert
          showIcon
          type="error"
          message="Không thể tải danh sách khách đang lưu trú"
          description={departuresQuery.error.message}
          action={<Button onClick={() => void departuresQuery.refetch()}>Thử lại</Button>}
          className="today-departures-page__alert"
        />
      )}

      {(!departuresQuery.isError || departuresQuery.data !== undefined) && (
        <Card className="today-departures-page__table-card" title="Danh sách khách đang lưu trú">
          <Table<TodayDeparture>
            childrenColumnName="nestedRows"
            columns={columns}
            dataSource={departures}
            loading={departuresQuery.isLoading || departuresQuery.isFetching}
            rowKey="bookingId"
            locale={{
              emptyText: (
                <Empty
                  image={Empty.PRESENTED_IMAGE_SIMPLE}
                  description="Hiện chưa có khách đang lưu trú"
                />
              ),
            }}
            pagination={{
              current: page + 1,
              pageSize: PAGE_SIZE,
              total: departuresQuery.data?.totalElements ?? 0,
              showSizeChanger: false,
              showTotal: (total) => `Tổng ${total} lượt đặt phòng`,
              onChange: (nextPage) => setPage(nextPage - 1),
            }}
            scroll={{ x: 1440 }}
          />
        </Card>
      )}

      <Modal
        title={selectedIsEarly ? 'Xác nhận trả phòng sớm' : 'Xác nhận trả phòng'}
        open={selectedDeparture !== null}
        okText={selectedIsEarly ? 'Xác nhận và tạo hoàn tiền' : 'Xác nhận trả phòng'}
        cancelText="Hủy"
        closable={!submitting}
        mask={{ closable: !submitting }}
        keyboard={!submitting}
        cancelButtonProps={{ disabled: submitting }}
        destroyOnHidden
        confirmLoading={checkOutMutation.isPending || earlyCheckoutMutation.isPending}
        okButtonProps={{
          disabled: selectedIsEarly
            ? earlyCheckoutQuote.isLoading ||
              earlyCheckoutQuote.isError ||
              earlyCollectionInvalid ||
              Boolean(
                adjustedEarlyRefund > 0 &&
                (!refundInformation.bankName.trim() ||
                  !refundInformation.accountHolderName.trim() ||
                  refundInformation.accountNumber.trim().length < 4),
              )
            : finalOutstandingAmount > 0 || overCollection,
        }}
        onCancel={closeCheckOutModal}
        onOk={() => void (selectedIsEarly ? submitEarlyCheckout() : submitCheckOut())}
      >
        {selectedDeparture && (
          <Space className="today-departures-page__checkout-modal" orientation="vertical" size={12}>
            <Paragraph>
              Hoàn tất trả phòng cho mã đặt phòng <Text code>{selectedDeparture.bookingCode}</Text>{' '}
              của khách <Text strong>{selectedDeparture.guestName}</Text>.
            </Paragraph>
            <div className="today-departures-page__checkout-money">
              <Text>Tổng tiền: {money.format(selectedDeparture.totalAmount)}</Text>
              <Text type="success">
                Đã thanh toán: {money.format(selectedDeparture.paidAmount)}
              </Text>
              <Text type={selectedDeparture.outstandingAmount > 0 ? 'danger' : 'secondary'}>
                Còn phải thu: {money.format(selectedDeparture.outstandingAmount)}
              </Text>
            </div>
            {selectedIsEarly && (
              <>
                <DatePicker
                  aria-label="Ngày trả phòng thực tế"
                  value={earlyDate ? dayjs(earlyDate) : null}
                  minDate={dayjs(selectedDeparture.checkInDate)}
                  maxDate={dayjs(selectedDeparture.checkOutDate).subtract(1, 'day')}
                  onChange={(date) => setEarlyDate(date?.format('YYYY-MM-DD'))}
                  format="DD/MM/YYYY"
                />
                {earlyCheckoutQuote.isLoading && (
                  <Alert showIcon type="info" message="Đang tính chính sách hoàn tiền..." />
                )}
                {earlyCheckoutQuote.isError && (
                  <Alert
                    showIcon
                    type="error"
                    message="Không thể tính hoàn tiền"
                    description={earlyCheckoutQuote.error.message}
                    action={
                      <Button onClick={() => void earlyCheckoutQuote.refetch()}>Thử lại</Button>
                    }
                  />
                )}
                {earlyCheckoutQuote.data && (
                  <>
                    <Descriptions column={1} bordered size="small">
                      <Descriptions.Item label="Số đêm không sử dụng">
                        {earlyCheckoutQuote.data.unusedNights}
                      </Descriptions.Item>
                      <Descriptions.Item label="Tiền đêm chưa sử dụng">
                        {money.format(earlyCheckoutQuote.data.unusedAmount)}
                      </Descriptions.Item>
                      <Descriptions.Item
                        label={`Phí trả phòng sớm (${earlyCheckoutQuote.data.penaltyPercent}%)`}
                      >
                        {money.format(earlyCheckoutQuote.data.penaltyAmount)}
                      </Descriptions.Item>
                      <Descriptions.Item label="Dự kiến hoàn">
                        <Text strong type="success">
                          {money.format(adjustedEarlyRefund)}
                        </Text>
                      </Descriptions.Item>
                      <Descriptions.Item label="Cần thu thêm tại quầy">
                        <Text strong type={earlyRequiredCollection > 0 ? 'danger' : 'success'}>
                          {money.format(earlyRequiredCollection)}
                        </Text>
                      </Descriptions.Item>
                    </Descriptions>
                    {adjustedEarlyRefund > 0 && (
                      <RefundInformationForm
                        value={refundInformation}
                        onChange={setRefundInformation}
                        disabled={earlyCheckoutMutation.isPending}
                      />
                    )}
                  </>
                )}
              </>
            )}
            {!selectedIsEarly && finalOutstandingAmount > 0 && (
              <Alert
                showIcon
                type="warning"
                message="Chưa thể hoàn tất trả phòng vì còn phải thu."
                description="Hãy nhập số tiền thu thêm tại quầy để công nợ cuối cùng về 0 trước khi xác nhận."
              />
            )}
            {!selectedIsEarly && overCollection && (
              <Alert
                showIcon
                type="error"
                title="Số tiền thu thêm vượt số tiền còn phải thu sau khi cộng phí phát sinh."
              />
            )}
            {selectedIsEarly && earlyCollectionInvalid && earlyCheckoutQuote.data && (
              <Alert
                showIcon
                type="warning"
                message="Số tiền thu tại quầy chưa khớp số tiền cần thu sau khi bù trừ hoàn tiền."
                description={`Cần thu chính xác ${money.format(earlyRequiredCollection)} để hoàn tất trả phòng sớm.`}
              />
            )}
            <Form form={form} layout="vertical" disabled={submitting}>
              {!selectedIsEarly && (
                <Descriptions column={1} bordered size="small">
                  <Descriptions.Item label="Phí phát sinh">
                    {money.format(Number(additionalChargeAmount))}
                  </Descriptions.Item>
                  <Descriptions.Item label="Tổng sau kiểm tra">
                    {money.format(finalTotalAmount)}
                  </Descriptions.Item>
                  <Descriptions.Item label="Đã thanh toán sau thu thêm">
                    {money.format(finalPaidAmount)}
                  </Descriptions.Item>
                  <Descriptions.Item label="Còn phải thu sau điều chỉnh">
                    <Text type={finalOutstandingAmount > 0 ? 'danger' : 'success'} strong>
                      {money.format(finalOutstandingAmount)}
                    </Text>
                  </Descriptions.Item>
                </Descriptions>
              )}
              <Form.Item
                name="additionalChargeAmount"
                label="Phí phát sinh / đền bù"
                rules={[
                  {
                    type: 'integer',
                    min: 0,
                    max: MAX_CHECKOUT_AMOUNT,
                    message: 'Nhập số tiền nguyên từ 0 đến 9.999.999.999 đồng',
                  },
                ]}
              >
                <InputNumber
                  min={0}
                  max={MAX_CHECKOUT_AMOUNT}
                  precision={0}
                  className="today-departures-page__number-input"
                  addonAfter="đ"
                />
              </Form.Item>
              <Form.Item
                noStyle
                shouldUpdate={(previous, current) =>
                  previous.additionalChargeAmount !== current.additionalChargeAmount
                }
              >
                {({ getFieldValue }) => (
                  <Form.Item
                    name="additionalChargeReason"
                    label="Lý do phát sinh"
                    rules={[
                      {
                        max: 255,
                        message: 'Lý do phát sinh không được vượt quá 255 ký tự',
                      },
                      {
                        validator: async (_, value: string | undefined) => {
                          const amount = Number(getFieldValue('additionalChargeAmount') ?? 0);
                          if (amount > 0 && !value?.trim()) {
                            throw new Error('Lý do phát sinh là bắt buộc khi có phí phát sinh');
                          }
                        },
                      },
                    ]}
                  >
                    <Input placeholder="Ví dụ: Mất thẻ phòng, hư remote, vỡ ly minibar" />
                  </Form.Item>
                )}
              </Form.Item>
              <Form.Item
                name="collectedAmount"
                label="Tiền mặt thu cho khoản còn phải trả"
                rules={[
                  {
                    type: 'integer',
                    min: 0,
                    max: Math.min(
                      selectedIsEarly ? earlyRequiredCollection : amountToCollect,
                      MAX_CHECKOUT_AMOUNT,
                    ),
                    message:
                      'Số tiền thu thêm phải là số nguyên và không vượt số tiền còn phải thu',
                  },
                ]}
                help="Booking ban đầu đã thanh toán đủ. Chỉ thu thêm nếu có gia hạn hoặc phí phát sinh chưa thanh toán; nhập tiền mặt thực nhận sau khi trả tiền thừa."
              >
                <InputNumber
                  disabled={
                    (selectedIsEarly ? earlyRequiredCollection : amountToCollect) === 0 &&
                    Number(collectedAmount) === 0
                  }
                  min={0}
                  max={MAX_CHECKOUT_AMOUNT}
                  precision={0}
                  className="today-departures-page__number-input"
                  addonAfter="đ"
                />
              </Form.Item>
              <Form.Item
                name="note"
                label="Ghi chú"
                rules={[{ max: 500, message: 'Ghi chú không được vượt quá 500 ký tự' }]}
              >
                <Input.TextArea
                  rows={4}
                  maxLength={500}
                  showCount
                  placeholder="Ví dụ: Khách đã trả đủ chìa khóa"
                />
              </Form.Item>
            </Form>
          </Space>
        )}
      </Modal>
    </div>
  );
}
