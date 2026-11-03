package com.tpanh.hotel.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreatePaymentRefundRequest(
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) @Schema(description = "Số tiền hoàn, đơn vị VND")
                BigDecimal amount,
        @Size(max = 255) String reason) {}
