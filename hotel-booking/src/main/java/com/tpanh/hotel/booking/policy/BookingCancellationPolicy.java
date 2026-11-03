package com.tpanh.hotel.booking.policy;

import com.tpanh.hotel.booking.domain.Booking;
import com.tpanh.hotel.payment.domain.PaymentStatus;
import java.time.Instant;

public interface BookingCancellationPolicy {
    CancellationDecision evaluate(Booking booking, PaymentStatus paymentStatus, Instant now);
}
