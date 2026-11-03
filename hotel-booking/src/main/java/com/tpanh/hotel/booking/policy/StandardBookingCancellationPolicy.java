package com.tpanh.hotel.booking.policy;

import com.tpanh.hotel.booking.config.BookingCancellationProperties;
import com.tpanh.hotel.booking.domain.Booking;
import com.tpanh.hotel.booking.domain.BookingStatus;
import com.tpanh.hotel.booking.dto.response.RefundStatus;
import com.tpanh.hotel.payment.domain.PaymentStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StandardBookingCancellationPolicy implements BookingCancellationPolicy {

    private final BookingCancellationProperties properties;

    @Override
    public CancellationDecision evaluate(Booking booking, PaymentStatus paymentStatus, Instant now) {
        if (booking.getStatus() == BookingStatus.PENDING) {
            return CancellationDecision.allowed(0, 0, RefundStatus.NOT_REQUIRED);
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            return CancellationDecision.denied(
                    "BOOKING_CANCELLATION_NOT_ALLOWED", "Trạng thái hiện tại không cho phép hủy đặt phòng");
        }

        Instant checkIn = booking.getCheckInDate()
                .atTime(properties.checkInTime())
                .atZone(properties.zoneId())
                .toInstant();
        long remainingMinutes = Duration.between(now, checkIn).toMinutes();
        long fullRefundMinutes = properties.fullRefundHours() * 60L;
        long partialRefundMinutes = properties.partialRefundHours() * 60L;

        if (remainingMinutes >= fullRefundMinutes) {
            return refundableDecision(booking, paymentStatus, 100);
        }
        if (remainingMinutes >= partialRefundMinutes) {
            return refundableDecision(booking, paymentStatus, properties.partialRefundPercent());
        }
        return CancellationDecision.denied(
                "BOOKING_CANCELLATION_WINDOW_EXPIRED",
                "Đặt phòng chỉ được hủy trước giờ nhận phòng ít nhất " + properties.partialRefundHours() + " giờ");
    }

    private CancellationDecision refundableDecision(Booking booking, PaymentStatus paymentStatus, int refundPercent) {
        if (paymentStatus != PaymentStatus.SUCCESS) {
            return CancellationDecision.allowed(0, 0, RefundStatus.NOT_REQUIRED);
        }
        long refundAmount = BigDecimal.valueOf(booking.getTotalAmount())
                .multiply(BigDecimal.valueOf(refundPercent))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN)
                .longValueExact();
        return CancellationDecision.allowed(refundPercent, refundAmount, RefundStatus.PENDING);
    }
}
