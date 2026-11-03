package com.tpanh.hotel.booking.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.annotation.JsonDeserialize;

public record CheckOutGuestRequest(
        @Size(max = 500, message = "Ghi chú không được vượt quá 500 ký tự") String note,
        @JsonDeserialize(using = CheckOutAmountDeserializer.class)
                @Min(value = 0, message = "Phí phát sinh không được âm") @Max(value = 9_999_999_999L, message = "Phí phát sinh không được vượt quá 9.999.999.999 đồng") Long additionalChargeAmount,
        @Size(max = 255, message = "Lý do phát sinh không được vượt quá 255 ký tự") String additionalChargeReason,
        @JsonDeserialize(using = CheckOutAmountDeserializer.class)
                @Min(value = 0, message = "Số tiền thu thêm không được âm") @Max(value = 9_999_999_999L, message = "Số tiền thu thêm không được vượt quá 9.999.999.999 đồng") Long collectedAmount) {

    @AssertTrue(message = "Lý do phát sinh là bắt buộc khi có phí phát sinh") public boolean isAdditionalChargeReasonValid() {
        return normalizedAdditionalChargeAmount() == 0
                || (additionalChargeReason != null
                        && !additionalChargeReason.trim().isEmpty());
    }

    public long normalizedAdditionalChargeAmount() {
        return additionalChargeAmount == null ? 0L : additionalChargeAmount;
    }

    public long normalizedCollectedAmount() {
        return collectedAmount == null ? 0L : collectedAmount;
    }

    public String normalizedNote() {
        return normalize(note);
    }

    public String normalizedAdditionalChargeReason() {
        return normalize(additionalChargeReason);
    }

    private String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
