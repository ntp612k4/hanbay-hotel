package com.tpanh.hotel.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreatePaymentRequest(@NotNull @Schema(description = "Booking cần thanh toán") UUID bookingId) {}
