package com.tpanh.hotel.payment.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.tpanh.hotel.payment.dto.SePayTransferType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SePayWebhookRequest(
        @Positive @Schema(description = "Mã giao dịch duy nhất do SePay cấp") long id,
        @NotBlank @Size(max = 100) String gateway,
        @NotNull @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime transactionDate,
        @NotBlank @Size(max = 100) String accountNumber,
        @Size(max = 100) String subAccount,
        @Size(max = 120) String code,
        @NotBlank @Size(max = 1000) String content,
        @NotNull SePayTransferType transferType,
        @Positive long transferAmount,
        @PositiveOrZero long accumulated,
        @Size(max = 1000) String description,
        @Size(max = 150) String referenceCode) {}
