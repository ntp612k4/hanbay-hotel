package com.tpanh.hotel.booking.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tpanh.hotel.booking.domain.Booking;
import com.tpanh.hotel.booking.domain.BookingNight;
import com.tpanh.hotel.booking.domain.BookingStatus;
import com.tpanh.hotel.booking.dto.request.EarlyCheckoutRequest;
import com.tpanh.hotel.booking.repository.BookingRepository;
import com.tpanh.hotel.common.security.CurrentUserProvider;
import com.tpanh.hotel.payment.domain.Payment;
import com.tpanh.hotel.payment.domain.PaymentRefundStatus;
import com.tpanh.hotel.payment.domain.PaymentStatus;
import com.tpanh.hotel.payment.domain.RefundMethod;
import com.tpanh.hotel.payment.dto.request.RefundInformationRequest;
import com.tpanh.hotel.payment.repository.PaymentRepository;
import com.tpanh.hotel.payment.service.CounterPaymentService;
import com.tpanh.hotel.pricing.service.PricingService;
import com.tpanh.hotel.room.domain.Room;
import com.tpanh.hotel.room.domain.RoomStatus;
import com.tpanh.hotel.room.domain.RoomType;
import com.tpanh.hotel.room.repository.RoomRepository;
import com.tpanh.hotel.room.repository.RoomTypeRepository;
import com.tpanh.hotel.room.service.RoomAvailabilityCalculator;
import com.tpanh.hotel.user.domain.User;
import com.tpanh.hotel.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StayModificationServiceImplTest {
    private static final Instant NOW = Instant.parse("2026-09-08T08:00:00Z");

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @Mock
    private RoomAvailabilityCalculator availabilityCalculator;

    @Mock
    private PricingService pricingService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private CounterPaymentService counterPaymentService;

    private StayModificationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new StayModificationServiceImpl(
                bookingRepository,
                paymentRepository,
                roomRepository,
                roomTypeRepository,
                availabilityCalculator,
                pricingService,
                userRepository,
                currentUserProvider,
                Clock.fixed(NOW, ZoneOffset.UTC),
                counterPaymentService);
    }

    @Test
    void offsetsEarlyCheckoutRefundAgainstDamageChargeBeforeCreatingRefund() {
        UUID bookingId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        UUID receptionistId = UUID.randomUUID();
        RoomType roomType = RoomType.builder().id(UUID.randomUUID()).build();
        Room room = Room.builder()
                .id(roomId)
                .roomType(roomType)
                .roomNumber("501")
                .floor(5)
                .status(RoomStatus.OCCUPIED)
                .build();
        Booking booking = Booking.builder()
                .id(bookingId)
                .bookingCode("BK-EARLY-001")
                .roomType(roomType)
                .assignedRoom(room)
                .checkInDate(LocalDate.of(2026, 9, 6))
                .checkOutDate(LocalDate.of(2026, 9, 10))
                .status(BookingStatus.CHECKED_IN)
                .totalAmount(2_000_000L)
                .paidAmount(2_000_000L)
                .build();
        booking.getNights()
                .addAll(List.of(
                        night(booking, LocalDate.of(2026, 9, 6), 500_000L),
                        night(booking, LocalDate.of(2026, 9, 7), 500_000L),
                        night(booking, LocalDate.of(2026, 9, 8), 500_000L),
                        night(booking, LocalDate.of(2026, 9, 9), 500_000L)));
        Payment payment = Payment.builder()
                .booking(booking)
                .paymentCode("PAY-20260908080000-ABCDEF12")
                .amount(2_000_000L)
                .status(PaymentStatus.SUCCESS)
                .expiresAt(NOW)
                .build();
        User receptionist = User.builder().id(receptionistId).build();
        EarlyCheckoutRequest request = new EarlyCheckoutRequest(
                LocalDate.of(2026, 9, 8),
                new RefundInformationRequest(RefundMethod.BANK_TRANSFER, "MB Bank", "NGUYEN VAN A", "0123456789"),
                "Hư hỏng đã được xác nhận",
                300_000L,
                "Vỡ ly minibar",
                0L);
        when(bookingRepository.findByIdWithAssignedRoomForUpdate(bookingId)).thenReturn(Optional.of(booking));
        when(roomRepository.findByIdForUpdate(roomId)).thenReturn(Optional.of(room));
        when(currentUserProvider.currentUserId()).thenReturn(Optional.of(receptionistId));
        when(userRepository.findById(receptionistId)).thenReturn(Optional.of(receptionist));
        when(paymentRepository.findFirstByBookingIdAndStatusOrderByPaidAtDesc(bookingId, PaymentStatus.SUCCESS))
                .thenReturn(Optional.of(payment));

        var response = service.earlyCheckout(bookingId, request);

        assertThat(response.refundAmount()).isEqualTo(500_000L);
        assertThat(response.refundStatus()).isEqualTo(PaymentRefundStatus.PENDING);
        assertThat(booking.getTotalAmount()).isEqualTo(2_300_000L);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CHECKED_OUT);
        assertThat(room.getStatus()).isEqualTo(RoomStatus.DIRTY);
        assertThat(payment.getRefunds()).singleElement().satisfies(refund -> assertThat(refund.getAmount())
                .isEqualByComparingTo("500000"));
        verify(counterPaymentService).settleAtCheckout(booking, 0L, receptionistId);
    }

    private BookingNight night(Booking booking, LocalDate stayDate, long price) {
        return BookingNight.builder()
                .booking(booking)
                .stayDate(stayDate)
                .pricePerNight(price)
                .build();
    }
}
