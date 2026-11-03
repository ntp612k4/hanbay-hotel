export interface GuestInfo {
  fullName: string;
  email: string;
  phone?: string;
  identityNumber: string;
}

export interface CreatePendingBookingRequest {
  roomTypeId: string;
  checkInDate: string;
  checkOutDate: string;
  adults: number;
  children: number;
  guestInfo: GuestInfo;
  addOns: AddOnSelection[];
}

export interface AddOnSelection {
  addOnServiceId: string;
  quantity: number;
}

export interface PendingBookingSummary {
  bookingId: string;
  bookingCode: string;
  status: 'PENDING';
  totalAmount: number;
  expiredAt: string;
  bookingAccessToken: string;
}

export interface CreatePaymentRequestResponse {
  paymentId: string;
  bookingId: string;
  bookingCode: string;
  transactionRef: string;
  amount: number;
  currency: 'VND';
  status: 'PENDING';
  paymentUrl: string;
  expiredAt: string;
  bankCode: string;
  accountNumber: string;
  accountName: string;
  transferContent: string;
}

export interface PaymentStatusResponse {
  paymentId: string;
  bookingId: string;
  bookingCode: string;
  transactionRef: string;
  paymentStatus: BookingPaymentStatus;
  bookingStatus: BookingStatus;
  roomTypeName: string;
  checkInDate: string;
  checkOutDate: string;
  adultCount: number;
  childCount: number;
  totalNights: number;
  amount: number;
  paidAmount: number;
  currency: 'VND';
  refundStatus: PaymentRefundStatus | null;
  refundAmount: number | null;
  refundReason: string | null;
  paymentUrl: string;
  bankCode: string;
  accountNumber: string;
  accountName: string;
  transferContent: string;
  expiredAt: string;
  paidAt: string | null;
}

export type BookingStatus =
  'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'CHECKED_IN' | 'CHECKED_OUT' | 'EXPIRED';

export type BookingPaymentStatus =
  'PENDING' | 'SUCCESS' | 'FAILED' | 'CANCELLED' | 'REFUNDED' | 'EXPIRED' | 'PARTIALLY_REFUNDED';

export type PaymentRefundStatus = 'PENDING' | 'PROCESSING' | 'SUCCESS' | 'FAILED' | 'CANCELLED';

export interface BookingHistoryRoomType {
  id: string;
  name: string;
  thumbnailUrl: string | null;
}

export interface BookingHistoryItem {
  bookingId: string;
  bookingCode: string;
  roomType: BookingHistoryRoomType;
  checkInDate: string;
  checkOutDate: string;
  adultCount: number;
  childCount: number;
  totalNights: number;
  totalAmount: number;
  outstandingAmount: number;
  currency: 'VND';
  bookingStatus: BookingStatus;
  paymentId: string | null;
  paymentStatus: BookingPaymentStatus | null;
  refundStatus: PaymentRefundStatus | null;
  refundAmount: number | null;
  createdAt: string;
}

export interface BookingHistoryParams {
  page: number;
  size: number;
  status?: BookingStatus;
}

export type RefundStatus = 'NOT_REQUIRED' | 'PENDING' | 'PARTIALLY_REFUNDED' | 'REFUNDED';

export interface CancellationEligibility {
  canCancel: boolean;
  reasonCode: string | null;
  message: string | null;
  refundPercent: number;
  refundAmount: number;
  currency: 'VND';
  policyDescription: string;
}

export interface CancelBookingResponse {
  bookingId: string;
  bookingCode: string;
  status: 'CANCELLED';
  cancelledAt: string;
  refundStatus: RefundStatus;
  refundPercent: number;
  refundAmount: number;
  currency: 'VND';
}

export interface ExtendStayResponse {
  bookingId: string;
  bookingCode: string;
  previousCheckOutDate: string;
  newCheckOutDate: string;
  additionalNights: number;
  additionalAmount: number;
  newTotalAmount: number;
  outstandingAmount: number;
  paymentRequired: boolean;
  retainedRoomId: string | null;
  currency: 'VND';
}

export interface CheckInGuestRequest {
  roomId: string;
}

export interface AssignedRoom {
  roomId: string;
  roomNumber: string;
  floor: number;
  status: 'OCCUPIED';
}

export interface AvailablePhysicalRoom {
  roomId: string;
  roomNumber: string;
  floor: number;
}

export interface CheckInGuestResponse {
  bookingId: string;
  bookingCode: string;
  bookingStatus: 'CHECKED_IN';
  room: AssignedRoom;
  checkedInAt: string;
}

export interface RoomTypeCatalogItem {
  id: string;
  code: string;
  name: string;
  description?: string;
  basePrice: number;
  maxGuests: number;
  adultCapacity: number;
  childCapacity: number;
  bedType?: string;
  areaM2?: number;
  imageUrl?: string;
  roomSubtotal: number;
  roomDiscount: number;
  roomTotal: number;
  nightlyPrices: { stayDate: string; price: number }[];
}

export interface AddOnCatalogItem {
  id: string;
  code: string;
  name: string;
  description?: string;
  price: number;
}

export interface SelectedAddOn extends AddOnCatalogItem {
  quantity: number;
}
export type { RefundInformation } from '../../payment/types/refund';
