package com.tpanh.hotel.payment.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tpanh.hotel.booking.domain.Booking;
import com.tpanh.hotel.booking.domain.BookingStatus;
import com.tpanh.hotel.payment.config.SePayProperties;
import com.tpanh.hotel.payment.domain.Payment;
import com.tpanh.hotel.payment.domain.PaymentRefundStatus;
import com.tpanh.hotel.payment.domain.PaymentStatus;
import com.tpanh.hotel.payment.domain.PaymentTransactionStatus;
import com.tpanh.hotel.payment.domain.PaymentTransactionType;
import com.tpanh.hotel.payment.domain.PaymentWebhookEvent;
import com.tpanh.hotel.payment.domain.WebhookProcessingStatus;
import com.tpanh.hotel.payment.dto.SePayTransferType;
import com.tpanh.hotel.payment.dto.request.SePayWebhookRequest;
import com.tpanh.hotel.payment.exception.InvalidWebhookApiKeyException;
import com.tpanh.hotel.payment.repository.PaymentRepository;
import com.tpanh.hotel.payment.repository.PaymentTransactionRepository;
import com.tpanh.hotel.payment.repository.PaymentWebhookEventRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentWebhookServiceImplTest {

    private static final Instant RECEIVED_AT = Instant.parse("2026-08-08T09:30:00Z");

    @Mock
    private PaymentWebhookEventRepository webhookEventRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentTransactionRepository transactionRepository;

    @Mock
    private com.tpanh.hotel.booking.repository.BookingRepository bookingRepository;

    private PaymentWebhookServiceImpl service;

    @BeforeEach
    void setUp() {
        SePayProperties properties =
                new SePayProperties("MB", "0123456789", "HANBAY HOTEL", "https://qr.example", "secret-key");
        service = new PaymentWebhookServiceImpl(
                webhookEventRepository,
                paymentRepository,
                transactionRepository,
                properties,
                Clock.fixed(RECEIVED_AT, ZoneOffset.UTC),
                bookingRepository);
    }

    @Test
    void storesValidWebhookBeforeAcknowledgingIt() {
        service.handleSePayWebhook("Apikey secret-key", validRequest());

        ArgumentCaptor<PaymentWebhookEvent> eventCaptor = ArgumentCaptor.forClass(PaymentWebhookEvent.class);
        verify(webhookEventRepository).save(eventCaptor.capture());
        PaymentWebhookEvent event = eventCaptor.getValue();

        assertThat(event.getEventId()).isEqualTo("12345");
        assertThat(event.getDeduplicationKey()).isEqualTo("sepay:event:12345");
        assertThat(event.getProcessingStatus()).isEqualTo(WebhookProcessingStatus.IGNORED);
        assertThat(event.getReceivedAt()).isEqualTo(RECEIVED_AT);
        assertThat(event.isSignatureValid()).isTrue();
        assertThat(event.getPayload()).containsEntry("transferAmount", 1_500_000L);
    }

    @Test
    void acknowledgesDuplicateWithoutSavingItAgain() {
        when(webhookEventRepository.existsByDeduplicationKey("sepay:event:12345"))
                .thenReturn(true);

        service.handleSePayWebhook("Apikey secret-key", validRequest());

        verify(webhookEventRepository, never()).save(any());
    }

    @Test
    void confirmsBookingWhenIncomingTransferPaysFullAmount() {
        Booking booking = Booking.builder()
                .id(UUID.randomUUID())
                .bookingCode("BKG-001")
                .status(BookingStatus.PENDING)
                .totalAmount(1_500_000L)
                .expiredAt(RECEIVED_AT.plusSeconds(600))
                .build();
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .booking(booking)
                .paymentCode("PAY-20260808163000-ABCDEF12")
                .amount(1_500_000L)
                .status(PaymentStatus.PENDING)
                .expiresAt(RECEIVED_AT.plusSeconds(600))
                .build();
        when(paymentRepository.findBookingIdByPaymentCode(payment.getPaymentCode()))
                .thenReturn(Optional.of(booking.getId()));
        when(bookingRepository.findByIdWithAssignedRoomForUpdate(booking.getId()))
                .thenReturn(Optional.of(booking));
        when(paymentRepository.findByPaymentCodeForUpdate(payment.getPaymentCode()))
                .thenReturn(Optional.of(payment));
        when(transactionRepository.sumSuccessfulPaymentAmount(payment.getId())).thenReturn(1_500_000L);

        service.handleSePayWebhook("Apikey secret-key", validRequest(payment.getPaymentCode()));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getPaidAmount()).isEqualTo(1_500_000L);
        assertThat(payment.getTransactions()).hasSize(1);
    }

    @Test
    void confirmsBookingWhenBankRemovesSeparatorsFromQrPaymentCode() {
        Booking booking = Booking.builder()
                .id(UUID.randomUUID())
                .bookingCode("BKG-COMPACT-CODE")
                .status(BookingStatus.PENDING)
                .totalAmount(4_200_000L)
                .expiredAt(RECEIVED_AT.plusSeconds(600))
                .build();
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .booking(booking)
                .paymentCode("PAY-20260816150422-10A30FFABFB64A4681F10BBA6DC63DE4")
                .amount(4_200_000L)
                .status(PaymentStatus.PENDING)
                .expiresAt(RECEIVED_AT.plusSeconds(600))
                .build();
        when(paymentRepository.findBookingIdByPaymentCode(payment.getPaymentCode()))
                .thenReturn(Optional.of(booking.getId()));
        when(bookingRepository.findByIdWithAssignedRoomForUpdate(booking.getId()))
                .thenReturn(Optional.of(booking));
        when(paymentRepository.findByPaymentCodeForUpdate(payment.getPaymentCode()))
                .thenReturn(Optional.of(payment));
        when(transactionRepository.sumSuccessfulPaymentAmount(payment.getId())).thenReturn(4_200_000L);

        service.handleSePayWebhook(
                "Apikey secret-key", validRequest("PAY2026081615042210A30FFABFB64A4681F10BBA6DC63DE4", 4_200_000L));

        var lockOrder = org.mockito.Mockito.inOrder(bookingRepository, paymentRepository);
        lockOrder.verify(bookingRepository).findByIdWithAssignedRoomForUpdate(booking.getId());
        lockOrder.verify(paymentRepository).findByPaymentCodeForUpdate(payment.getPaymentCode());
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void confirmsBookingAndCreatesRefundForOverpayment() {
        Booking booking = Booking.builder()
                .id(UUID.randomUUID())
                .bookingCode("BKG-OVERPAID")
                .status(BookingStatus.PENDING)
                .totalAmount(1_500_000L)
                .expiredAt(RECEIVED_AT.plusSeconds(600))
                .build();
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .booking(booking)
                .paymentCode("PAY-20260808163000-ABCDEF12")
                .amount(1_500_000L)
                .status(PaymentStatus.PENDING)
                .expiresAt(RECEIVED_AT.plusSeconds(600))
                .build();
        when(paymentRepository.findBookingIdByPaymentCode(payment.getPaymentCode()))
                .thenReturn(Optional.of(booking.getId()));
        when(bookingRepository.findByIdWithAssignedRoomForUpdate(booking.getId()))
                .thenReturn(Optional.of(booking));
        when(paymentRepository.findByPaymentCodeForUpdate(payment.getPaymentCode()))
                .thenReturn(Optional.of(payment));
        when(transactionRepository.sumSuccessfulPaymentAmount(payment.getId())).thenReturn(1_700_000L);

        service.handleSePayWebhook("Apikey secret-key", validRequest(payment.getPaymentCode(), 1_700_000L));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(payment.getRefunds()).singleElement().satisfies(refund -> {
            assertThat(refund.getAmount()).isEqualByComparingTo("200000");
            assertThat(refund.getStatus()).isEqualTo(PaymentRefundStatus.PENDING);
            assertThat(refund.getIdempotencyKey()).isEqualTo("overpayment:" + payment.getId() + ":12345");
        });
    }

    @Test
    void createsPendingRefundForPartialTransferToExpiredBooking() {
        Booking booking = Booking.builder()
                .id(UUID.randomUUID())
                .bookingCode("BKG-EXPIRED")
                .status(BookingStatus.PENDING)
                .totalAmount(1_500_000L)
                .expiredAt(RECEIVED_AT)
                .build();
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .booking(booking)
                .paymentCode("PAY-20260808163000-ABCDEF12")
                .amount(1_500_000L)
                .status(PaymentStatus.PENDING)
                .expiresAt(RECEIVED_AT)
                .build();
        when(paymentRepository.findBookingIdByPaymentCode(payment.getPaymentCode()))
                .thenReturn(Optional.of(booking.getId()));
        when(bookingRepository.findByIdWithAssignedRoomForUpdate(booking.getId()))
                .thenReturn(Optional.of(booking));
        when(paymentRepository.findByPaymentCodeForUpdate(payment.getPaymentCode()))
                .thenReturn(Optional.of(payment));
        when(transactionRepository.sumSuccessfulPaymentAmount(payment.getId())).thenReturn(500_000L);

        service.handleSePayWebhook("Apikey secret-key", validRequest(payment.getPaymentCode(), 500_000L));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(payment.getRefunds()).singleElement().satisfies(refund -> {
            assertThat(refund.getAmount()).isEqualByComparingTo("500000");
            assertThat(refund.getStatus()).isEqualTo(PaymentRefundStatus.PENDING);
            assertThat(refund.getIdempotencyKey()).isEqualTo("late-payment:" + payment.getId() + ":12345");
        });
        assertThat(payment.getTransactions())
                .filteredOn(transaction -> transaction.getType() == PaymentTransactionType.REFUND)
                .singleElement()
                .extracting(transaction -> transaction.getStatus())
                .isEqualTo(PaymentTransactionStatus.PENDING);
    }

    @ParameterizedTest
    @EnumSource(
            value = PaymentStatus.class,
            names = {"CANCELLED", "FAILED", "EXPIRED"})
    void createsPendingRefundForPartialLateTransferToClosedPayment(PaymentStatus closedStatus) {
        Booking booking = Booking.builder()
                .id(UUID.randomUUID())
                .bookingCode("BKG-CLOSED")
                .status(BookingStatus.CANCELLED)
                .totalAmount(1_500_000L)
                .expiredAt(null)
                .build();
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .booking(booking)
                .paymentCode("PAY-20260808163000-ABCDEF12")
                .amount(1_500_000L)
                .status(closedStatus)
                .expiresAt(RECEIVED_AT.minusSeconds(60))
                .build();
        when(paymentRepository.findBookingIdByPaymentCode(payment.getPaymentCode()))
                .thenReturn(Optional.of(booking.getId()));
        when(bookingRepository.findByIdWithAssignedRoomForUpdate(booking.getId()))
                .thenReturn(Optional.of(booking));
        when(paymentRepository.findByPaymentCodeForUpdate(payment.getPaymentCode()))
                .thenReturn(Optional.of(payment));
        when(transactionRepository.sumSuccessfulPaymentAmount(payment.getId())).thenReturn(500_000L);

        service.handleSePayWebhook("Apikey secret-key", validRequest(payment.getPaymentCode(), 500_000L));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(payment.getRefunds()).singleElement().satisfies(refund -> {
            assertThat(refund.getAmount()).isEqualByComparingTo("500000");
            assertThat(refund.getStatus()).isEqualTo(PaymentRefundStatus.PENDING);
            assertThat(refund.getIdempotencyKey()).isEqualTo("late-payment:" + payment.getId() + ":12345");
        });
    }

    @Test
    void createsRefundWhenAdditionalTransferArrivesAfterPaymentSucceeded() {
        Booking booking = Booking.builder()
                .id(UUID.randomUUID())
                .bookingCode("BKG-CONFIRMED")
                .status(BookingStatus.CONFIRMED)
                .totalAmount(1_500_000L)
                .build();
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .booking(booking)
                .paymentCode("PAY-20260808163000-ABCDEF12")
                .amount(1_500_000L)
                .status(PaymentStatus.SUCCESS)
                .expiresAt(RECEIVED_AT.minusSeconds(60))
                .build();
        when(paymentRepository.findBookingIdByPaymentCode(payment.getPaymentCode()))
                .thenReturn(Optional.of(booking.getId()));
        when(bookingRepository.findByIdWithAssignedRoomForUpdate(booking.getId()))
                .thenReturn(Optional.of(booking));
        when(paymentRepository.findByPaymentCodeForUpdate(payment.getPaymentCode()))
                .thenReturn(Optional.of(payment));
        when(transactionRepository.sumSuccessfulPaymentAmount(payment.getId())).thenReturn(1_700_000L);

        service.handleSePayWebhook("Apikey secret-key", validRequest(payment.getPaymentCode(), 200_000L));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(payment.getRefunds()).singleElement().satisfies(refund -> {
            assertThat(refund.getAmount()).isEqualByComparingTo("200000");
            assertThat(refund.getIdempotencyKey()).isEqualTo("overpayment:" + payment.getId() + ":12345");
        });
    }

    @Test
    void refundsLateQrTransferAfterCheckoutWithoutIncreasingBookingPaidAmount() {
        Booking booking = Booking.builder()
                .id(UUID.randomUUID())
                .bookingCode("BKG-CHECKED-OUT")
                .status(BookingStatus.CHECKED_OUT)
                .totalAmount(1_500_000L)
                .paidAmount(1_500_000L)
                .build();
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .booking(booking)
                .paymentCode("PAY-20260808163000-ABCDEF12")
                .amount(1_500_000L)
                .status(PaymentStatus.CANCELLED)
                .expiresAt(RECEIVED_AT.minusSeconds(60))
                .build();
        when(paymentRepository.findBookingIdByPaymentCode(payment.getPaymentCode()))
                .thenReturn(Optional.of(booking.getId()));
        when(bookingRepository.findByIdWithAssignedRoomForUpdate(booking.getId()))
                .thenReturn(Optional.of(booking));
        when(paymentRepository.findByPaymentCodeForUpdate(payment.getPaymentCode()))
                .thenReturn(Optional.of(payment));
        when(transactionRepository.sumSuccessfulPaymentAmount(payment.getId())).thenReturn(100_000L);

        service.handleSePayWebhook("Apikey secret-key", validRequest(payment.getPaymentCode(), 100_000L));

        assertThat(booking.getPaidAmount()).isEqualTo(1_500_000L);
        assertThat(payment.getRefunds()).singleElement().satisfies(refund -> assertThat(refund.getAmount())
                .isEqualByComparingTo("100000"));
    }

    @Test
    void rejectsInvalidApiKeyBeforeAccessingRepository() {
        assertThatThrownBy(() -> service.handleSePayWebhook("Apikey wrong-key", validRequest()))
                .isInstanceOf(InvalidWebhookApiKeyException.class);

        verify(webhookEventRepository, never()).existsByDeduplicationKey(any());
        verify(webhookEventRepository, never()).acquireDeduplicationLock(any());
        verify(webhookEventRepository, never()).save(any());
    }

    private SePayWebhookRequest validRequest() {
        return validRequest("HBABC123456");
    }

    private SePayWebhookRequest validRequest(String content) {
        return validRequest(content, 1_500_000L);
    }

    private SePayWebhookRequest validRequest(String content, long amount) {
        return new SePayWebhookRequest(
                12345L,
                "MBBank",
                LocalDateTime.of(2026, 8, 8, 16, 30),
                "0123456789",
                null,
                null,
                content,
                SePayTransferType.IN,
                amount,
                2_000_000L,
                "Payment for booking",
                "FT2622012345");
    }
}
