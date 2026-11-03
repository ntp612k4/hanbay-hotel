package com.tpanh.hotel.payment.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tpanh.hotel.booking.domain.Booking;
import com.tpanh.hotel.booking.domain.BookingStatus;
import com.tpanh.hotel.booking.repository.BookingRepository;
import com.tpanh.hotel.booking.service.BookingAccessTokenService;
import com.tpanh.hotel.common.security.CurrentUserProvider;
import com.tpanh.hotel.payment.config.PaymentProperties;
import com.tpanh.hotel.payment.domain.Payment;
import com.tpanh.hotel.payment.domain.PaymentRefund;
import com.tpanh.hotel.payment.domain.PaymentRefundStatus;
import com.tpanh.hotel.payment.domain.PaymentStatus;
import com.tpanh.hotel.payment.dto.request.CreatePaymentRequest;
import com.tpanh.hotel.payment.dto.result.SePayQrInfo;
import com.tpanh.hotel.payment.exception.PaymentAlreadyCompletedException;
import com.tpanh.hotel.payment.exception.PaymentBookingNotFoundException;
import com.tpanh.hotel.payment.exception.PaymentBookingNotPayableException;
import com.tpanh.hotel.payment.repository.PaymentRepository;
import com.tpanh.hotel.payment.repository.PaymentTransactionRepository;
import com.tpanh.hotel.payment.service.SePayService;
import com.tpanh.hotel.room.domain.RoomType;
import java.math.BigDecimal;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-08-11T08:00:00Z");

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private BookingAccessTokenService bookingAccessTokenService;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private SePayService sePayService;

    private PaymentServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient()
                .when(bookingAccessTokenService.matches("access-token", "access-token-hash"))
                .thenReturn(true);
        lenient()
                .when(sePayService.generateQr(anyLong(), anyString()))
                .thenAnswer(invocation -> qrInfo(invocation.getArgument(1)));
        service = new PaymentServiceImpl(
                bookingRepository,
                paymentRepository,
                paymentTransactionRepository,
                new PaymentProperties(15),
                sePayService,
                Clock.fixed(NOW, ZoneOffset.UTC),
                bookingAccessTokenService,
                currentUserProvider);
    }

    @Test
    void createsPendingPaymentForPayableBooking() {
        Booking booking = booking(BookingStatus.PENDING, NOW.plusSeconds(900), 2_450_000L);
        when(bookingRepository.findByIdForUpdate(booking.getId())).thenReturn(Optional.of(booking));
        when(paymentRepository.findAllByBookingIdAndStatusOrderByCreatedAtDesc(booking.getId(), PaymentStatus.PENDING))
                .thenReturn(List.of());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sePayService.generateQr(anyLong(), anyString()))
                .thenAnswer(invocation -> qrInfo(invocation.getArgument(1)));

        var response = service.createPaymentRequest(new CreatePaymentRequest(booking.getId()), "access-token");

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment payment = paymentCaptor.getValue();
        assertThat(payment.getBooking()).isSameAs(booking);
        assertThat(payment.getPaymentCode()).startsWith("PAY-20260811080000-");
        assertThat(payment.getAmount()).isEqualTo(2_450_000L);
        assertThat(payment.getCurrency()).isEqualTo("VND");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getExpiresAt()).isEqualTo(booking.getExpiredAt());
        assertThat(response.transactionRef()).isEqualTo(payment.getPaymentCode());
        assertThat(response.paymentUrl()).isEqualTo("https://vietqr.app/img?des=" + payment.getPaymentCode());
        assertThat(response.bankCode()).isEqualTo("TESTBANK");
        assertThat(response.accountNumber()).isEqualTo("0000000000");
        assertThat(response.accountName()).isEqualTo("HANBAY HOTEL DEV");
        assertThat(response.transferContent()).isEqualTo(payment.getPaymentCode());
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);
        verify(sePayService).generateQr(2_450_000L, payment.getPaymentCode());
    }

    @Test
    void rejectsMissingBooking() {
        UUID bookingId = UUID.randomUUID();
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createPaymentRequest(new CreatePaymentRequest(bookingId), "access-token"))
                .isInstanceOf(PaymentBookingNotFoundException.class);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void rejectsBookingThatIsNotPending() {
        Booking booking = booking(BookingStatus.CANCELLED, NOW.plusSeconds(900), 2_450_000L);
        when(bookingRepository.findByIdForUpdate(booking.getId())).thenReturn(Optional.of(booking));

        assertThatThrownBy(
                        () -> service.createPaymentRequest(new CreatePaymentRequest(booking.getId()), "access-token"))
                .isInstanceOf(PaymentBookingNotPayableException.class);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void rejectsExpiredPendingBooking() {
        Booking booking = booking(BookingStatus.PENDING, NOW, 2_450_000L);
        when(bookingRepository.findByIdForUpdate(booking.getId())).thenReturn(Optional.of(booking));

        assertThatThrownBy(
                        () -> service.createPaymentRequest(new CreatePaymentRequest(booking.getId()), "access-token"))
                .isInstanceOf(PaymentBookingNotPayableException.class);
    }

    @Test
    void rejectsInvalidBookingAmount() {
        Booking booking = booking(BookingStatus.PENDING, NOW.plusSeconds(900), 0L);
        when(bookingRepository.findByIdForUpdate(booking.getId())).thenReturn(Optional.of(booking));

        assertThatThrownBy(
                        () -> service.createPaymentRequest(new CreatePaymentRequest(booking.getId()), "access-token"))
                .isInstanceOf(PaymentAlreadyCompletedException.class);
    }

    @Test
    void rejectsBookingWithSuccessfulPayment() {
        Booking booking = booking(BookingStatus.PENDING, NOW.plusSeconds(900), 2_450_000L);
        booking.recordPayment(2_450_000L);
        when(bookingRepository.findByIdForUpdate(booking.getId())).thenReturn(Optional.of(booking));

        assertThatThrownBy(
                        () -> service.createPaymentRequest(new CreatePaymentRequest(booking.getId()), "access-token"))
                .isInstanceOf(PaymentAlreadyCompletedException.class);
    }

    @Test
    void returnsActivePendingPaymentWithoutCreatingDuplicate() {
        Booking booking = booking(BookingStatus.PENDING, NOW.plusSeconds(900), 2_450_000L);
        Payment existingPayment = payment(booking, PaymentStatus.PENDING, NOW.plusSeconds(600));
        when(bookingRepository.findByIdForUpdate(booking.getId())).thenReturn(Optional.of(booking));
        when(paymentRepository.findAllByBookingIdAndStatusOrderByCreatedAtDesc(booking.getId(), PaymentStatus.PENDING))
                .thenReturn(List.of(existingPayment));
        when(sePayService.generateQr(anyLong(), anyString()))
                .thenAnswer(invocation -> qrInfo(invocation.getArgument(1)));

        var response = service.createPaymentRequest(new CreatePaymentRequest(booking.getId()), "access-token");

        assertThat(response.transactionRef()).isEqualTo(existingPayment.getPaymentCode());
        assertThat(response.paymentUrl()).isEqualTo("https://vietqr.app/img?des=" + existingPayment.getPaymentCode());
        verify(paymentRepository, never()).save(any());
        verify(sePayService).generateQr(2_450_000L, existingPayment.getPaymentCode());
    }

    @Test
    void expiresPendingPaymentBeforeCreatingNewPayment() {
        Booking booking = booking(BookingStatus.PENDING, NOW.plusSeconds(900), 2_450_000L);
        Payment expiredPayment = payment(booking, PaymentStatus.PENDING, NOW.minusSeconds(1));
        when(bookingRepository.findByIdForUpdate(booking.getId())).thenReturn(Optional.of(booking));
        when(paymentRepository.findAllByBookingIdAndStatusOrderByCreatedAtDesc(booking.getId(), PaymentStatus.PENDING))
                .thenReturn(List.of(expiredPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sePayService.generateQr(anyLong(), anyString()))
                .thenAnswer(invocation -> qrInfo(invocation.getArgument(1)));

        service.createPaymentRequest(new CreatePaymentRequest(booking.getId()), "access-token");

        assertThat(expiredPayment.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
        assertThat(expiredPayment.getFailedAt()).isNull();
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void returnsPaymentStatusForGuestWithValidBookingAccessToken() {
        Booking booking = booking(BookingStatus.PENDING, NOW.plusSeconds(900), 2_450_000L);
        Payment payment = payment(booking, PaymentStatus.PENDING, NOW.plusSeconds(600));
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(paymentTransactionRepository.sumSuccessfulPaymentAmount(payment.getId()))
                .thenReturn(500_000L);
        when(currentUserProvider.currentUserId()).thenReturn(Optional.empty());

        var response = service.getPaymentStatus(payment.getId(), "access-token");

        assertThat(response.paymentId()).isEqualTo(payment.getId());
        assertThat(response.bookingId()).isEqualTo(booking.getId());
        assertThat(response.bookingCode()).isEqualTo(booking.getBookingCode());
        assertThat(response.transactionRef()).isEqualTo(payment.getPaymentCode());
        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(response.bookingStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(response.roomTypeName()).isEqualTo("Junior Suite");
        assertThat(response.checkInDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(response.checkOutDate()).isEqualTo(LocalDate.of(2026, 9, 5));
        assertThat(response.adultCount()).isEqualTo(2);
        assertThat(response.childCount()).isEqualTo(1);
        assertThat(response.totalNights()).isEqualTo(4);
        assertThat(response.amount()).isEqualTo(2_450_000L);
        assertThat(response.paidAmount()).isEqualTo(500_000L);
        assertThat(response.currency()).isEqualTo("VND");
        assertThat(response.expiredAt()).isEqualTo(payment.getExpiresAt());
        assertThat(response.paidAt()).isNull();
        verify(bookingAccessTokenService).matches("access-token", booking.getAccessTokenHash());
    }

    @Test
    void rejectsPaymentStatusRequestWithoutCheckoutAccess() {
        Booking booking = booking(BookingStatus.PENDING, NOW.plusSeconds(900), 2_450_000L);
        Payment payment = payment(booking, PaymentStatus.PENDING, NOW.plusSeconds(600));
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(currentUserProvider.currentUserId()).thenReturn(Optional.empty());
        when(bookingAccessTokenService.matches("invalid-token", booking.getAccessTokenHash()))
                .thenReturn(false);

        assertThatThrownBy(() -> service.getPaymentStatus(payment.getId(), "invalid-token"))
                .isInstanceOf(PaymentBookingNotFoundException.class);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void marksPendingPaymentExpiredBeforeReturningStatus() {
        Booking booking = booking(BookingStatus.PENDING, NOW.plusSeconds(900), 2_450_000L);
        Payment payment = payment(booking, PaymentStatus.PENDING, NOW);
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(paymentRepository.markPendingExpired(payment.getId(), NOW)).thenAnswer(invocation -> {
            payment.markExpired(NOW);
            return 1;
        });
        when(currentUserProvider.currentUserId()).thenReturn(Optional.empty());

        var response = service.getPaymentStatus(payment.getId(), "access-token");

        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.EXPIRED);
        assertThat(response.bookingStatus()).isEqualTo(BookingStatus.PENDING);
        verify(paymentRepository).markPendingExpired(payment.getId(), NOW);
    }

    @Test
    void returnsPaidAmountForPaymentAndPrioritizesPendingRefund() {
        Booking booking = booking(BookingStatus.CONFIRMED, NOW.plusSeconds(900), 2_450_000L);
        Payment payment = payment(booking, PaymentStatus.SUCCESS, NOW.plusSeconds(600));
        payment.addRefund(PaymentRefund.builder()
                .amount(BigDecimal.valueOf(100_000L))
                .status(PaymentRefundStatus.SUCCESS)
                .reason("Đã hoàn một phần")
                .idempotencyKey("completed-refund")
                .requestedAt(NOW.minusSeconds(60))
                .build());
        payment.addRefund(PaymentRefund.builder()
                .amount(BigDecimal.valueOf(200_000L))
                .status(PaymentRefundStatus.PENDING)
                .reason("Hoàn phần tiền thanh toán dư")
                .idempotencyKey("pending-refund")
                .requestedAt(NOW)
                .build());
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(paymentTransactionRepository.sumSuccessfulPaymentAmount(payment.getId()))
                .thenReturn(2_650_000L);

        var response = service.getPaymentStatus(payment.getId(), "access-token");

        assertThat(response.paidAmount()).isEqualTo(2_650_000L);
        assertThat(response.refundStatus()).isEqualTo(PaymentRefundStatus.PENDING);
        assertThat(response.refundAmount()).isEqualByComparingTo("200000");
        assertThat(response.refundReason()).isEqualTo("Hoàn phần tiền thanh toán dư");
    }

    private Booking booking(BookingStatus status, Instant expiredAt, long totalAmount) {
        return Booking.builder()
                .id(UUID.randomUUID())
                .bookingCode("BKG-000001")
                .roomType(RoomType.builder()
                        .id(UUID.randomUUID())
                        .name("Junior Suite")
                        .build())
                .checkInDate(LocalDate.of(2026, 9, 1))
                .checkOutDate(LocalDate.of(2026, 9, 5))
                .adultCount(2)
                .childCount(1)
                .status(status)
                .expiredAt(expiredAt)
                .totalAmount(totalAmount)
                .accessTokenHash("access-token-hash")
                .build();
    }

    private Payment payment(Booking booking, PaymentStatus status, Instant expiresAt) {
        return Payment.builder()
                .id(UUID.randomUUID())
                .booking(booking)
                .paymentCode("PAY-EXISTING")
                .amount(booking.getTotalAmount())
                .currency("VND")
                .status(status)
                .expiresAt(expiresAt)
                .build();
    }

    private SePayQrInfo qrInfo(String paymentCode) {
        return new SePayQrInfo(
                "TESTBANK", "0000000000", "HANBAY HOTEL DEV", paymentCode, "https://vietqr.app/img?des=" + paymentCode);
    }
}
