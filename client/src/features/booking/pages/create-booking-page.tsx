import { ArrowLeftOutlined, ClockCircleOutlined } from '@ant-design/icons';
import { useMutation, useQuery } from '@tanstack/react-query';
import { Alert, App, Button, Flex, Form, Grid, Tag, theme } from 'antd';
import dayjs from 'dayjs';
import { useEffect, useState } from 'react';
import { Link, Navigate, useNavigate, useSearchParams } from 'react-router-dom';

import { MainFooter } from '@components/layout/MainFooter';
import { MainHeader } from '@components/layout/MainHeader';
import { BookingSteps } from '@components/booking/BookingSteps';
import { useAuth } from '@features/authentication/providers/auth-context';
import { ApiClientError } from '../../../types/api';
import { searchAvailableRoomTypes } from '@features/room';
import { createPendingBooking, updatePendingBooking } from '../api/booking-api';
import { getAddOns } from '../api/catalog-api';
import type {
  AddOnCatalogItem,
  CreatePendingBookingRequest,
  PendingBookingSummary,
} from '../types/booking';
import type { BookingFormValues } from '../types/booking-form';
import { BookingPaymentPage } from './booking-payment-page';
import { GuestInformationPage } from './guest-information-page';
import './create-booking-page.css';

const guestStepFields: (keyof BookingFormValues)[] = [
  'adults',
  'children',
  'fullName',
  'email',
  'phone',
  'identityNumber',
];
type RoomSelectionValues = Pick<BookingFormValues, 'roomTypeId' | 'stayDates'>;
type BookingInitialValues = Partial<BookingFormValues>;

type OutdatedValidationResult<T> = {
  errorFields: unknown[];
  outOfDate: true;
  values: T;
};

function isOutdatedValidationWithoutErrors<T>(
  error: unknown,
): error is OutdatedValidationResult<T> {
  if (!error || typeof error !== 'object') return false;
  const result = error as Partial<OutdatedValidationResult<T>>;
  return (
    result.outOfDate === true && Array.isArray(result.errorFields) && !result.errorFields.length
  );
}

function getInitialBookingValues(searchParams: URLSearchParams): BookingInitialValues {
  const roomTypeId = searchParams.get('roomTypeId') ?? undefined;
  const checkIn = searchParams.get('checkIn');
  const checkOut = searchParams.get('checkOut');
  const adults = Number(searchParams.get('adults')) || 2;
  const children = Number(searchParams.get('children')) || 0;
  const stayDates =
    checkIn && checkOut
      ? ([dayjs(checkIn), dayjs(checkOut)] as BookingFormValues['stayDates'])
      : undefined;

  return { roomTypeId, stayDates, adults, children };
}

function getInitialRoomSelection(values: BookingInitialValues): RoomSelectionValues | undefined {
  if (!values.roomTypeId || !values.stayDates) return undefined;
  return { roomTypeId: values.roomTypeId, stayDates: values.stayDates };
}

export function CreateBookingPage() {
  const { message } = App.useApp();
  const { token } = theme.useToken();
  const screens = Grid.useBreakpoint();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [form] = Form.useForm<BookingFormValues>();
  const { session } = useAuth();
  const initialBookingValues = getInitialBookingValues(searchParams);
  const [currentStep, setCurrentStep] = useState<0 | 1 | 2>(1);
  const [addOnQuantities, setAddOnQuantities] = useState<Record<string, number>>({});
  const [booking, setBooking] = useState<PendingBookingSummary>();
  const [submittedRequest, setSubmittedRequest] = useState<CreatePendingBookingRequest>();
  const roomSelection = getInitialRoomSelection(initialBookingValues);
  const watchedValues = Form.useWatch([], form) as BookingFormValues | undefined;

  useEffect(() => {
    if (!session?.user) return;
    const current = form.getFieldsValue(['fullName', 'email', 'phone']);
    form.setFieldsValue({
      fullName: current.fullName || session.user.fullName,
      email: current.email || session.user.email,
      phone: current.phone || session.user.phone || undefined,
    });
  }, [form, session?.user]);

  const availabilityQuery = useQuery({
    queryKey: [
      'booking',
      'selected-room',
      roomSelection?.roomTypeId,
      roomSelection?.stayDates[0].format('YYYY-MM-DD'),
      roomSelection?.stayDates[1].format('YYYY-MM-DD'),
      initialBookingValues.adults,
      initialBookingValues.children,
    ],
    queryFn: () =>
      searchAvailableRoomTypes({
        checkIn: roomSelection!.stayDates[0].format('YYYY-MM-DD'),
        checkOut: roomSelection!.stayDates[1].format('YYYY-MM-DD'),
        adults: initialBookingValues.adults ?? 2,
        children: initialBookingValues.children ?? 0,
        roomTypeId: roomSelection!.roomTypeId,
        page: 0,
        size: 1,
      }),
    enabled: Boolean(roomSelection),
  });
  const addOnsQuery = useQuery({
    queryKey: ['booking', 'add-ons'],
    queryFn: getAddOns,
    enabled: Boolean(roomSelection),
  });
  const availableRoomType = availabilityQuery.data?.content.find(
    (item) => item.id === roomSelection?.roomTypeId,
  );
  const selectedRoomType = availableRoomType
    ? {
        ...availableRoomType,
        description: availableRoomType.description ?? undefined,
        bedType: availableRoomType.bedType ?? undefined,
        areaM2: availableRoomType.areaM2 ?? undefined,
        imageUrl: availableRoomType.imageUrl ?? undefined,
      }
    : undefined;
  const selectedAddOns =
    addOnsQuery.data
      ?.filter((item) => (addOnQuantities[item.id] ?? 0) > 0)
      .map((item) => ({ ...item, quantity: addOnQuantities[item.id] })) ?? [];

  const createBookingMutation = useMutation({
    mutationFn: (payload: CreatePendingBookingRequest) =>
      booking
        ? updatePendingBooking(booking.bookingId, payload, booking.bookingAccessToken)
        : createPendingBooking(payload),
    onSuccess: (createdBooking) => {
      setBooking(createdBooking);
      setCurrentStep(2);
      void message.success('Đã giữ hạng phòng trong 15 phút.');
    },
    onError: (error) => {
      void message.error(
        error instanceof ApiClientError
          ? error.message
          : 'Không thể tạo booking. Vui lòng thử lại.',
      );
    },
  });

  const updateAddOnQuantity = (addOn: AddOnCatalogItem, quantity: number) => {
    setAddOnQuantities((current) => ({ ...current, [addOn.id]: quantity }));
  };

  const submitBooking = async () => {
    if (!roomSelection) {
      navigate('/rooms/search', { replace: true });
      return;
    }

    let values: BookingFormValues;
    try {
      values = await form.validateFields(guestStepFields);
    } catch (error) {
      if (isOutdatedValidationWithoutErrors<BookingFormValues>(error)) {
        values = error.values;
      } else {
        void message.warning('Vui lòng kiểm tra lại thông tin khách hàng.');
        return;
      }
    }

    const payload: CreatePendingBookingRequest = {
      roomTypeId: roomSelection.roomTypeId,
      checkInDate: roomSelection.stayDates[0].format('YYYY-MM-DD'),
      checkOutDate: roomSelection.stayDates[1].format('YYYY-MM-DD'),
      adults: values.adults,
      children: values.children,
      guestInfo: {
        fullName: values.fullName.trim(),
        email: values.email.trim(),
        phone: values.phone?.trim() || undefined,
        identityNumber: values.identityNumber.trim(),
      },
      addOns: selectedAddOns.map((addOn) => ({
        addOnServiceId: addOn.id,
        quantity: addOn.quantity,
      })),
    };
    setSubmittedRequest(payload);
    createBookingMutation.mutate(payload);
  };

  if (!roomSelection) {
    return <Navigate to="/rooms/search" replace />;
  }

  return (
    <div className="booking-public-shell">
      <MainHeader />
      <main className="create-booking-page">
        <div className="booking-page-shell">
          <Flex
            align="center"
            justify="space-between"
            className="page-enter"
            style={{ marginBottom: 30 }}
          >
            <Link to="/" style={{ color: token.colorTextSecondary }}>
              <ArrowLeftOutlined /> Trang chủ
            </Link>
            {!screens.xs && <Tag icon={<ClockCircleOutlined />}>GIỮ PHÒNG 15 PHÚT</Tag>}
          </Flex>
          <BookingSteps
            className="page-enter"
            style={{ maxWidth: 920, margin: `0 auto ${screens.xs ? 36 : 56}px` }}
            current={currentStep}
            paymentDisabled={!booking}
          />

          {currentStep < 2 &&
          !availabilityQuery.isLoading &&
          (availabilityQuery.isError || !selectedRoomType) ? (
            <Alert
              showIcon
              type="warning"
              message="Hạng phòng đã chọn không còn khả dụng"
              description="Vui lòng quay lại tìm kiếm để chọn hạng phòng khác."
              action={
                <Button type="primary" onClick={() => navigate('/rooms/search')}>
                  Tìm phòng khác
                </Button>
              }
            />
          ) : currentStep < 2 ? (
            <Form
              form={form}
              layout="vertical"
              initialValues={initialBookingValues}
              requiredMark={false}
            >
              <GuestInformationPage
                roomType={selectedRoomType}
                stayDates={roomSelection.stayDates}
                adults={watchedValues?.adults}
                children={watchedValues?.children}
                addOns={addOnsQuery.data}
                selectedAddOns={selectedAddOns}
                addOnQuantities={addOnQuantities}
                addOnsLoading={addOnsQuery.isLoading}
                addOnsError={addOnsQuery.isError}
                submitting={createBookingMutation.isPending || availabilityQuery.isLoading}
                onQuantityChange={updateAddOnQuantity}
                onRetryAddOns={() => void addOnsQuery.refetch()}
                onBack={() => navigate(`/rooms/search?${searchParams.toString()}`)}
                onSubmit={() => void submitBooking()}
              />
            </Form>
          ) : booking && submittedRequest ? (
            <BookingPaymentPage
              booking={booking}
              request={submittedRequest}
              roomType={selectedRoomType}
              addOns={selectedAddOns}
              onEdit={() => setCurrentStep(1)}
            />
          ) : null}
        </div>
      </main>
      <MainFooter />
    </div>
  );
}
