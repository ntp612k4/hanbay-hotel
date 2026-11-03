package com.tpanh.hotel.payment.service.impl;

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
import com.tpanh.hotel.payment.dto.response.CreatePaymentRequestResponse;
import com.tpanh.hotel.payment.dto.response.PaymentStatusResponse;
import com.tpanh.hotel.payment.dto.result.SePayQrInfo;
import com.tpanh.hotel.payment.exception.PaymentAlreadyCompletedException;
import com.tpanh.hotel.payment.exception.PaymentBookingNotFoundException;
import com.tpanh.hotel.payment.exception.PaymentBookingNotPayableException;
import com.tpanh.hotel.payment.repository.PaymentRepository;
import com.tpanh.hotel.payment.repository.PaymentTransactionRepository;
import com.tpanh.hotel.payment.service.PaymentService;
import com.tpanh.hotel.payment.service.SePayService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final DateTimeFormatter PAYMENT_CODE_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC);

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentProperties paymentProperties;
    private final SePayService sePayService;
    private final Clock clock;
    private final BookingAccessTokenService bookingAccessTokenService;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @Transactional
    public CreatePaymentRequestResponse createPaymentRequest(CreatePaymentRequest request, String bookingAccessToken) {
        Instant now = clock.instant();
        Booking booking = bookingRepository
                .findByIdForUpdate(request.bookingId())
                .orElseThrow(() -> new PaymentBookingNotFoundException("Không tìm thấy đặt phòng cần thanh toán."));

        ensureCheckoutAccess(booking, bookingAccessToken);

        validatePayableBooking(booking, now);
        long payableAmount = payableAmount(booking);

        Payment activePendingPayment = findActivePendingPayment(booking, now, payableAmount);
        if (activePendingPayment != null) {
            return toResponse(activePendingPayment);
        }

        closeNonReusablePendingPayments(booking, now, payableAmount);

        Payment payment = Payment.builder()
                .booking(booking)
                .paymentCode(generatePaymentCode(now))
                .amount(payableAmount)
                .currency("VND")
                .status(PaymentStatus.PENDING)
                .idempotencyKey(additionalBalanceKey(booking))
                .expiresAt(calculatePaymentExpiration(booking, now))
                .build();

        return toResponse(paymentRepository.save(payment));
    }

    private String additionalBalanceKey(Booking booking) {
        if (booking.getStatus() == BookingStatus.PENDING) {
            return null;
        }
        return "additional-balance:" + booking.getId() + ":" + booking.getTotalAmount();
    }

    private void validatePayableBooking(Booking booking, Instant now) {
        if (booking.getStatus() != BookingStatus.PENDING
                && booking.getStatus() != BookingStatus.CONFIRMED
                && booking.getStatus() != BookingStatus.CHECKED_IN) {
            throw new PaymentBookingNotPayableException("Trạng thái booking không cho phép thanh toán.");
        }
        if (booking.getStatus() == BookingStatus.PENDING
                && (booking.getExpiredAt() == null || !booking.getExpiredAt().isAfter(now))) {
            throw new PaymentBookingNotPayableException("Đặt phòng đã hết thời gian giữ phòng.");
        }
        if (payableAmount(booking) <= 0) {
            throw new PaymentAlreadyCompletedException("Đặt phòng không còn số dư cần thanh toán.");
        }
    }

    private long payableAmount(Booking booking) {
        return Math.max(booking.getTotalAmount() - booking.getPaidAmount(), 0L);
    }

    private Payment findActivePendingPayment(Booking booking, Instant now, long payableAmount) {
        return paymentRepository
                .findAllByBookingIdAndStatusOrderByCreatedAtDesc(booking.getId(), PaymentStatus.PENDING)
                .stream()
                .filter(payment -> payment.isPendingAndActive(now) && payment.getAmount() == payableAmount)
                .findFirst()
                .orElse(null);
    }

    private void closeNonReusablePendingPayments(Booking booking, Instant now, long payableAmount) {
        paymentRepository
                .findAllByBookingIdAndStatusOrderByCreatedAtDesc(booking.getId(), PaymentStatus.PENDING)
                .stream()
                .forEach(payment -> {
                    if (payment.isPendingAndExpired(now)) {
                        payment.markExpired(now);
                    } else if (payment.getAmount() != payableAmount) {
                        payment.cancelPending(now);
                    }
                });
    }

    private Instant calculatePaymentExpiration(Booking booking, Instant now) {
        Instant configuredExpiration = now.plusSeconds(paymentProperties.expirationMinutes() * 60L);
        return booking.getExpiredAt() != null && booking.getExpiredAt().isBefore(configuredExpiration)
                ? booking.getExpiredAt()
                : configuredExpiration;
    }

    private String generatePaymentCode(Instant now) {
        return "PAY-" + PAYMENT_CODE_TIMESTAMP.format(now) + "-"
                + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    private CreatePaymentRequestResponse toResponse(Payment payment) {
        return CreatePaymentRequestResponse.from(
                payment, sePayService.generateQr(payment.getAmount(), payment.getPaymentCode()));
    }

    @Override
    @Transactional
    public PaymentStatusResponse getPaymentStatus(UUID paymentId, String bookingAccessToken) {
        Payment payment = findPayment(paymentId);
        ensureCheckoutAccess(payment.getBooking(), bookingAccessToken);
        payment = expirePendingPaymentIfNeeded(payment, clock.instant());
        Booking booking = payment.getBooking();
        PaymentRefund refund = findRelevantRefund(payment);
        SePayQrInfo qrInfo = sePayService.generateQr(payment.getAmount(), payment.getPaymentCode());
        return new PaymentStatusResponse(
                payment.getId(),
                booking.getId(),
                booking.getBookingCode(),
                payment.getPaymentCode(),
                payment.getStatus(),
                booking.getStatus(),
                booking.getRoomType().getName(),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                booking.getAdultCount(),
                booking.getChildCount(),
                java.time.temporal.ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate()),
                payment.getAmount(),
                paymentTransactionRepository.sumSuccessfulPaymentAmount(payment.getId()),
                payment.getCurrency(),
                refund == null ? null : refund.getStatus(),
                refund == null ? null : refund.getAmount(),
                refund == null ? null : refund.getReason(),
                qrInfo.qrUrl(),
                qrInfo.bankCode(),
                qrInfo.accountNumber(),
                qrInfo.accountName(),
                qrInfo.transferContent(),
                payment.getExpiresAt(),
                payment.getPaidAt());
    }

    private Payment findPayment(UUID paymentId) {
        return paymentRepository
                .findById(paymentId)
                .orElseThrow(() -> new PaymentBookingNotFoundException("Không tìm thấy khoản thanh toán."));
    }

    private Payment expirePendingPaymentIfNeeded(Payment payment, Instant now) {
        if (payment.isPendingAndExpired(now)) {
            paymentRepository.markPendingExpired(payment.getId(), now);
            return findPayment(payment.getId());
        }
        return payment;
    }

    private PaymentRefund findRelevantRefund(Payment payment) {
        return payment.getRefunds().stream()
                .max(Comparator.comparing((PaymentRefund refund) -> isActiveRefund(refund.getStatus()))
                        .thenComparing(PaymentRefund::getRequestedAt))
                .orElse(null);
    }

    private boolean isActiveRefund(PaymentRefundStatus status) {
        return status == PaymentRefundStatus.PENDING || status == PaymentRefundStatus.PROCESSING;
    }

    private void ensureCheckoutAccess(Booking booking, String bookingAccessToken) {
        UUID currentUserId = currentUserProvider.currentUserId().orElse(null);
        boolean owner =
                booking.getBookedBy() != null && booking.getBookedBy().getId().equals(currentUserId);
        if (!owner && !bookingAccessTokenService.matches(bookingAccessToken, booking.getAccessTokenHash())) {
            throw new PaymentBookingNotFoundException("Không tìm thấy đặt phòng cần thanh toán.");
        }
    }
}
