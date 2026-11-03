package com.tpanh.hotel.pricing.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record CalculateStayPriceRequest(
        @NotNull(message = "ID hạng phòng không được để trống") UUID roomTypeId,
        @NotNull(message = "Ngày nhận phòng không được để trống") LocalDate checkInDate,
        @NotNull(message = "Ngày trả phòng không được để trống") LocalDate checkOutDate) {}
