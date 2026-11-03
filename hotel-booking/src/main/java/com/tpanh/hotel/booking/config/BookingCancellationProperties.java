package com.tpanh.hotel.booking.config;

import jakarta.validation.constraints.*;
import java.time.LocalTime;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "booking.cancellation")
public record BookingCancellationProperties(
        @NotNull LocalTime checkInTime,
        @NotBlank String hotelZone,
        @Positive int fullRefundHours,
        @Positive int partialRefundHours,
        @Min(1) @Max(99) int partialRefundPercent) {

    public BookingCancellationProperties {
        if (fullRefundHours <= partialRefundHours) {
            throw new IllegalArgumentException("Thời hạn hoàn toàn bộ phải lớn hơn thời hạn hoàn một phần");
        }

        ZoneId.of(hotelZone);
    }

    public ZoneId zoneId() {
        return ZoneId.of(hotelZone);
    }
}
