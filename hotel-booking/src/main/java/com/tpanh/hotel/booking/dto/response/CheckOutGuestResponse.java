package com.tpanh.hotel.booking.dto.response;

import com.tpanh.hotel.booking.domain.BookingStatus;
import java.time.Instant;
import java.util.UUID;

public record CheckOutGuestResponse(
        UUID bookingId,
        String bookingCode,
        BookingStatus bookingStatus,
        AssignedRoomResponse room,
        Instant checkedOutAt,
        long totalAmount,
        long paidAmount,
        long outstandingAmount,
        long additionalChargeAmount,
        String additionalChargeReason,
        long collectedAmount) {}
