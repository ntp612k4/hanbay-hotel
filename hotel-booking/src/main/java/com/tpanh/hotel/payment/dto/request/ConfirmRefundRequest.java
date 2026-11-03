package com.tpanh.hotel.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConfirmRefundRequest(
        @NotBlank(message = "Mã tham chiếu ngân hàng không được để trống") @Size(max = 120, message = "Mã tham chiếu ngân hàng không được vượt quá 120 ký tự") String bankReference) {
    public String normalizedBankReference() {
        return bankReference.trim();
    }
}
