package com.tpanh.hotel.frontdesk.dto.response;

import com.tpanh.hotel.booking.domain.BookingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Booking dự kiến nhận phòng hôm nay")
public record TodayArrivalResponse(
        @Schema(format = "uuid") UUID bookingId,
        @Schema(example = "HAN-20260819-001") String bookingCode,
        @Schema(nullable = true, description = "Null khi booking cũ thiếu khách chính")
                PrimaryGuestSummaryResponse primaryGuest,
        ArrivalRoomTypeResponse roomType,
        @Schema(format = "date", example = "2026-08-19") LocalDate checkInDate,
        @Schema(format = "date", example = "2026-08-21") LocalDate checkOutDate,
        @Schema(example = "2") int adultCount,
        @Schema(example = "1") int childCount,
        @Schema(allowableValues = "CONFIRMED") BookingStatus bookingStatus,
        @Schema(format = "date-time") Instant createdAt) {}
