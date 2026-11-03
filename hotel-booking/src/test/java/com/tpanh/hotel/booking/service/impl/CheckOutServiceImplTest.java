package com.tpanh.hotel.booking.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tpanh.hotel.booking.domain.Booking;
import com.tpanh.hotel.booking.domain.BookingStatus;
import com.tpanh.hotel.booking.dto.request.CheckOutGuestRequest;
import com.tpanh.hotel.booking.exception.CheckOutBookingNotFoundException;
import com.tpanh.hotel.booking.exception.CheckOutConflictException;
import com.tpanh.hotel.booking.repository.BookingRepository;
import com.tpanh.hotel.common.exception.DomainException;
import com.tpanh.hotel.common.security.CurrentUserProvider;
import com.tpanh.hotel.room.domain.Room;
import com.tpanh.hotel.room.domain.RoomStatus;
import com.tpanh.hotel.room.domain.RoomType;
import com.tpanh.hotel.room.repository.RoomRepository;
import com.tpanh.hotel.user.domain.User;
import com.tpanh.hotel.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CheckOutServiceImplTest {

    private static final UUID BOOKING_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final String BOOKING_CODE = "BK20260824000001";
    private static final UUID ROOM_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID ROOM_TYPE_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final UUID RECEPTIONIST_ID = UUID.fromString("50000000-0000-0000-0000-000000000001");
    private static final Instant NOW = Instant.parse("2026-08-24T03:30:00Z");

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private com.tpanh.hotel.payment.repository.PaymentRepository paymentRepository;

    private final Clock clock = Clock.fixed(NOW, ZoneId.of("Asia/Ho_Chi_Minh"));

    @InjectMocks
    private CheckOutServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CheckOutServiceImpl(
                bookingRepository,
                roomRepository,
                userRepository,
                currentUserProvider,
                clock,
                new com.tpanh.hotel.payment.service.CounterPaymentService(paymentRepository, clock));
    }

    @Test
    void checksOutGuestAndMarksRoomDirtyWithinLockedFlow() {
        RoomType roomType = roomType();
        Room room = room(roomType, RoomStatus.OCCUPIED);
        Booking booking = booking(BookingStatus.CHECKED_IN, room, 5_635_000L, 5_635_000L);
        User receptionist = User.builder().id(RECEPTIONIST_ID).build();

        when(bookingRepository.findByIdWithAssignedRoomForUpdate(BOOKING_ID)).thenReturn(Optional.of(booking));
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));
        when(currentUserProvider.currentUserId()).thenReturn(Optional.of(RECEPTIONIST_ID));
        when(userRepository.findById(RECEPTIONIST_ID)).thenReturn(Optional.of(receptionist));

        var response = service.checkOut(BOOKING_ID, request("Khách đã trả đủ chìa khóa"));

        assertThat(response.bookingId()).isEqualTo(BOOKING_ID);
        assertThat(response.bookingCode()).isEqualTo(BOOKING_CODE);
        assertThat(response.bookingStatus()).isEqualTo(BookingStatus.CHECKED_OUT);
        assertThat(response.checkedOutAt()).isEqualTo(NOW);
        assertThat(response.totalAmount()).isEqualTo(5_635_000L);
        assertThat(response.paidAmount()).isEqualTo(5_635_000L);
        assertThat(response.outstandingAmount()).isZero();
        assertThat(response.additionalChargeAmount()).isZero();
        assertThat(response.additionalChargeReason()).isNull();
        assertThat(response.collectedAmount()).isZero();
        assertThat(response.room().status()).isEqualTo(RoomStatus.DIRTY);
        assertThat(room.getStatus()).isEqualTo(RoomStatus.DIRTY);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CHECKED_OUT);
        assertThat(booking.getStatusHistories()).singleElement().satisfies(history -> {
            assertThat(history.getOldStatus()).isEqualTo(BookingStatus.CHECKED_IN);
            assertThat(history.getNewStatus()).isEqualTo(BookingStatus.CHECKED_OUT);
            assertThat(history.getChangedBy()).isSameAs(receptionist);
            assertThat(history.getReason()).contains("Khách đã trả đủ chìa khóa");
        });
        var lockOrder = inOrder(bookingRepository, roomRepository);
        lockOrder.verify(bookingRepository).findByIdWithAssignedRoomForUpdate(BOOKING_ID);
        lockOrder.verify(roomRepository).findByIdForUpdate(ROOM_ID);
    }

    @Test
    void checksOutGuestWithInspectionChargeAndCollectedAmount() {
        RoomType roomType = roomType();
        Room room = room(roomType, RoomStatus.OCCUPIED);
        Booking booking = booking(BookingStatus.CHECKED_IN, room, 1_000_000L, 1_000_000L);
        User receptionist = User.builder().id(RECEPTIONIST_ID).build();
        CheckOutGuestRequest request =
                new CheckOutGuestRequest("Khách xác nhận biên bản", 150_000L, "Mất thẻ phòng", 150_000L);

        when(bookingRepository.findByIdWithAssignedRoomForUpdate(BOOKING_ID)).thenReturn(Optional.of(booking));
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));
        when(currentUserProvider.currentUserId()).thenReturn(Optional.of(RECEPTIONIST_ID));
        when(userRepository.findById(RECEPTIONIST_ID)).thenReturn(Optional.of(receptionist));

        var response = service.checkOut(BOOKING_ID, request);

        var receipt = org.mockito.ArgumentCaptor.forClass(com.tpanh.hotel.payment.domain.Payment.class);
        verify(paymentRepository).save(receipt.capture());
        assertThat(receipt.getValue().getAmount()).isEqualTo(150_000L);
        assertThat(receipt.getValue().getStatus()).isEqualTo(com.tpanh.hotel.payment.domain.PaymentStatus.SUCCESS);
        assertThat(receipt.getValue().getTransactions()).singleElement().satisfies(transaction -> {
            assertThat(transaction.getAmount()).isEqualTo(150_000L);
            assertThat(transaction.getRawPayload()).containsEntry("collectedBy", RECEPTIONIST_ID.toString());
        });
        assertThat(response.totalAmount()).isEqualTo(1_150_000L);
        assertThat(response.paidAmount()).isEqualTo(1_150_000L);
        assertThat(response.outstandingAmount()).isZero();
        assertThat(response.additionalChargeAmount()).isEqualTo(150_000L);
        assertThat(response.additionalChargeReason()).isEqualTo("Mất thẻ phòng");
        assertThat(response.collectedAmount()).isEqualTo(150_000L);
        assertThat(booking.getTotalAmount()).isEqualTo(1_150_000L);
        assertThat(booking.getPaidAmount()).isEqualTo(1_150_000L);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CHECKED_OUT);
        assertThat(room.getStatus()).isEqualTo(RoomStatus.DIRTY);
        assertThat(booking.getStatusHistories()).singleElement().satisfies(history -> {
            assertThat(history.getReason()).contains("Khách xác nhận biên bản");
            assertThat(history.getReason()).contains("Phí phát sinh: 150000 VND - Mất thẻ phòng");
            assertThat(history.getReason()).contains("Đã thu thêm tại quầy: 150000 VND");
        });
    }

    @Test
    void rejectsCheckoutWhenInspectionChargeIsNotFullyCollected() {
        RoomType roomType = roomType();
        Room room = room(roomType, RoomStatus.OCCUPIED);
        Booking booking = booking(BookingStatus.CHECKED_IN, room, 1_000_000L, 1_000_000L);
        CheckOutGuestRequest request = new CheckOutGuestRequest(null, 150_000L, "Vỡ ly minibar", 100_000L);
        when(bookingRepository.findByIdWithAssignedRoomForUpdate(BOOKING_ID)).thenReturn(Optional.of(booking));
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));

        assertConflict("PAYMENT_REQUIRED", () -> service.checkOut(BOOKING_ID, request));

        assertThat(booking.getTotalAmount()).isEqualTo(1_000_000L);
        assertThat(booking.getPaidAmount()).isEqualTo(1_000_000L);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CHECKED_IN);
        assertThat(room.getStatus()).isEqualTo(RoomStatus.OCCUPIED);
    }

    @Test
    void allowsCheckoutWhenCollectedAmountCoversExistingOutstandingBalance() {
        RoomType roomType = roomType();
        Room room = room(roomType, RoomStatus.OCCUPIED);
        Booking booking = booking(BookingStatus.CHECKED_IN, room, 1_000_000L, 900_000L);
        User receptionist = User.builder().id(RECEPTIONIST_ID).build();
        CheckOutGuestRequest request = new CheckOutGuestRequest("Thu nốt tiền phòng", null, null, 100_000L);
        when(bookingRepository.findByIdWithAssignedRoomForUpdate(BOOKING_ID)).thenReturn(Optional.of(booking));
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));
        when(currentUserProvider.currentUserId()).thenReturn(Optional.of(RECEPTIONIST_ID));
        when(userRepository.findById(RECEPTIONIST_ID)).thenReturn(Optional.of(receptionist));

        var response = service.checkOut(BOOKING_ID, request);

        assertThat(response.totalAmount()).isEqualTo(1_000_000L);
        assertThat(response.paidAmount()).isEqualTo(1_000_000L);
        assertThat(response.outstandingAmount()).isZero();
        assertThat(response.collectedAmount()).isEqualTo(100_000L);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CHECKED_OUT);
        assertThat(room.getStatus()).isEqualTo(RoomStatus.DIRTY);
    }

    @Test
    void cancelsPendingQrPaymentsWhenCheckoutIsCompletedAtTheCounter() {
        Room room = room(roomType(), RoomStatus.OCCUPIED);
        Booking booking = booking(BookingStatus.CHECKED_IN, room, 1_000_000L, 1_000_000L);
        User receptionist = User.builder().id(RECEPTIONIST_ID).build();
        var pendingQr = com.tpanh.hotel.payment.domain.Payment.builder()
                .booking(booking)
                .paymentCode("PAY-20260824033000-ABCDEF12")
                .amount(100_000L)
                .status(com.tpanh.hotel.payment.domain.PaymentStatus.PENDING)
                .expiresAt(NOW.plusSeconds(900))
                .build();
        when(bookingRepository.findByIdWithAssignedRoomForUpdate(BOOKING_ID)).thenReturn(Optional.of(booking));
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));
        when(currentUserProvider.currentUserId()).thenReturn(Optional.of(RECEPTIONIST_ID));
        when(userRepository.findById(RECEPTIONIST_ID)).thenReturn(Optional.of(receptionist));
        when(paymentRepository.findAllByBookingIdAndStatusOrderByCreatedAtDesc(
                        BOOKING_ID, com.tpanh.hotel.payment.domain.PaymentStatus.PENDING))
                .thenReturn(List.of(pendingQr));

        service.checkOut(BOOKING_ID, null);

        assertThat(pendingQr.getStatus()).isEqualTo(com.tpanh.hotel.payment.domain.PaymentStatus.CANCELLED);
        assertThat(pendingQr.getCancelledAt()).isEqualTo(NOW);
    }

    @Test
    void rejectsMissingBooking() {
        when(bookingRepository.findByIdWithAssignedRoomForUpdate(BOOKING_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.checkOut(BOOKING_ID, null))
                .isInstanceOf(CheckOutBookingNotFoundException.class)
                .extracting(exception -> ((DomainException) exception).getErrorCode())
                .isEqualTo("BOOKING_NOT_FOUND");

        verify(roomRepository, never()).findByIdForUpdate(ROOM_ID);
    }

    @Test
    void rejectsBookingThatIsNotCheckedInBeforeLockingRoom() {
        Booking booking =
                booking(BookingStatus.CONFIRMED, room(roomType(), RoomStatus.OCCUPIED), 1_000_000L, 1_000_000L);
        when(bookingRepository.findByIdWithAssignedRoomForUpdate(BOOKING_ID)).thenReturn(Optional.of(booking));

        assertConflict("BOOKING_NOT_CHECKED_IN", () -> service.checkOut(BOOKING_ID, null));

        verify(roomRepository, never()).findByIdForUpdate(ROOM_ID);
    }

    @Test
    void rejectsBookingThatWasAlreadyCheckedOutWithSpecificCode() {
        Booking booking =
                booking(BookingStatus.CHECKED_OUT, room(roomType(), RoomStatus.DIRTY), 1_000_000L, 1_000_000L);
        when(bookingRepository.findByIdWithAssignedRoomForUpdate(BOOKING_ID)).thenReturn(Optional.of(booking));

        assertConflict("BOOKING_ALREADY_CHECKED_OUT", () -> service.checkOut(BOOKING_ID, null));

        verify(roomRepository, never()).findByIdForUpdate(ROOM_ID);
    }

    @Test
    void rejectsCheckedInBookingWithoutAssignedRoom() {
        Booking booking = booking(BookingStatus.CHECKED_IN, null, 1_000_000L, 1_000_000L);
        when(bookingRepository.findByIdWithAssignedRoomForUpdate(BOOKING_ID)).thenReturn(Optional.of(booking));

        assertConflict("ROOM_NOT_ASSIGNED", () -> service.checkOut(BOOKING_ID, null));

        verify(roomRepository, never()).findByIdForUpdate(ROOM_ID);
    }

    @Test
    void rejectsAssignedRoomThatIsNotOccupied() {
        RoomType roomType = roomType();
        Room room = room(roomType, RoomStatus.AVAILABLE);
        Booking booking = booking(BookingStatus.CHECKED_IN, room, 1_000_000L, 1_000_000L);
        when(bookingRepository.findByIdWithAssignedRoomForUpdate(BOOKING_ID)).thenReturn(Optional.of(booking));
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));

        assertConflict("ROOM_NOT_OCCUPIED", () -> service.checkOut(BOOKING_ID, null));
    }

    @Test
    void rejectsBookingWithOutstandingBalance() {
        RoomType roomType = roomType();
        Room room = room(roomType, RoomStatus.OCCUPIED);
        Booking booking = booking(BookingStatus.CHECKED_IN, room, 1_000_000L, 900_000L);
        when(bookingRepository.findByIdWithAssignedRoomForUpdate(BOOKING_ID)).thenReturn(Optional.of(booking));
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));

        assertConflict("PAYMENT_REQUIRED", () -> service.checkOut(BOOKING_ID, null));

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CHECKED_IN);
        assertThat(room.getStatus()).isEqualTo(RoomStatus.OCCUPIED);
    }

    @Test
    void treatsOverpaymentAsZeroOutstandingAmount() {
        RoomType roomType = roomType();
        Room room = room(roomType, RoomStatus.OCCUPIED);
        Booking booking = booking(BookingStatus.CHECKED_IN, room, 1_000_000L, 1_100_000L);
        User receptionist = User.builder().id(RECEPTIONIST_ID).build();
        when(bookingRepository.findByIdWithAssignedRoomForUpdate(BOOKING_ID)).thenReturn(Optional.of(booking));
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));
        when(currentUserProvider.currentUserId()).thenReturn(Optional.of(RECEPTIONIST_ID));
        when(userRepository.findById(RECEPTIONIST_ID)).thenReturn(Optional.of(receptionist));

        var response = service.checkOut(BOOKING_ID, null);

        assertThat(response.outstandingAmount()).isZero();
        assertThat(response.bookingStatus()).isEqualTo(BookingStatus.CHECKED_OUT);
        assertThat(response.room().status()).isEqualTo(RoomStatus.DIRTY);
    }

    @Test
    void rejectsCollectionAboveBalanceIncludingDamageChargeWithoutChangingBooking() {
        Room room = room(roomType(), RoomStatus.OCCUPIED);
        Booking booking = booking(BookingStatus.CHECKED_IN, room, 1_000_000L, 1_000_000L);
        when(bookingRepository.findByIdWithAssignedRoomForUpdate(BOOKING_ID)).thenReturn(Optional.of(booking));
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));
        assertConflict(
                "CHECKOUT_OVER_COLLECTION",
                () -> service.checkOut(BOOKING_ID, new CheckOutGuestRequest(null, 200_000L, "Vỡ đèn", 300_000L)));
        assertThat(booking.getTotalAmount()).isEqualTo(1_000_000L);
        assertThat(booking.getPaidAmount()).isEqualTo(1_000_000L);
        assertThat(room.getStatus()).isEqualTo(RoomStatus.OCCUPIED);
        org.mockito.Mockito.verifyNoInteractions(paymentRepository);
    }

    @Test
    void rejectsOverflowAsValidationErrorBeforeAnyMutation() {
        Room room = room(roomType(), RoomStatus.OCCUPIED);
        Booking booking = booking(BookingStatus.CHECKED_IN, room, Long.MAX_VALUE, Long.MAX_VALUE);
        when(bookingRepository.findByIdWithAssignedRoomForUpdate(BOOKING_ID)).thenReturn(Optional.of(booking));
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));
        assertThatThrownBy(() -> service.checkOut(BOOKING_ID, new CheckOutGuestRequest(null, 1L, "Vỡ ly", 1L)))
                .isInstanceOf(com.tpanh.hotel.common.exception.RequestValidationException.class);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CHECKED_IN);
        org.mockito.Mockito.verifyNoInteractions(paymentRepository);
    }

    @Test
    void rejectsOrdinaryCheckoutBeforeScheduledDate() {
        Booking booking = booking(BookingStatus.CHECKED_IN, room(roomType(), RoomStatus.OCCUPIED), 100L, 100L);
        when(bookingRepository.findByIdWithAssignedRoomForUpdate(BOOKING_ID)).thenReturn(Optional.of(booking));
        var earlierService = new CheckOutServiceImpl(
                bookingRepository,
                roomRepository,
                userRepository,
                currentUserProvider,
                Clock.fixed(NOW.minusSeconds(86400), ZoneId.of("Asia/Ho_Chi_Minh")),
                new com.tpanh.hotel.payment.service.CounterPaymentService(paymentRepository, clock));
        assertConflict("EARLY_CHECKOUT_REQUIRED", () -> earlierService.checkOut(BOOKING_ID, null));
        org.mockito.Mockito.verifyNoInteractions(roomRepository, paymentRepository);
    }

    private void assertConflict(String code, Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(CheckOutConflictException.class)
                .extracting(exception -> ((DomainException) exception).getErrorCode())
                .isEqualTo(code);
    }

    private CheckOutGuestRequest request(String note) {
        return new CheckOutGuestRequest(note, null, null, null);
    }

    private Booking booking(BookingStatus status, Room assignedRoom, long totalAmount, long paidAmount) {
        return Booking.builder()
                .id(BOOKING_ID)
                .bookingCode(BOOKING_CODE)
                .roomType(roomType())
                .assignedRoom(assignedRoom)
                .checkInDate(LocalDate.of(2026, 8, 23))
                .checkOutDate(LocalDate.of(2026, 8, 24))
                .status(status)
                .totalAmount(totalAmount)
                .paidAmount(paidAmount)
                .build();
    }

    private Room room(RoomType roomType, RoomStatus status) {
        return Room.builder()
                .id(ROOM_ID)
                .roomType(roomType)
                .roomNumber("0501")
                .floor(5)
                .status(status)
                .build();
    }

    private RoomType roomType() {
        return RoomType.builder().id(ROOM_TYPE_ID).build();
    }
}
