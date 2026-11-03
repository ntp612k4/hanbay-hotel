package com.tpanh.hotel.payment.dto.request;

import com.tpanh.hotel.payment.domain.RefundMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RefundInformationRequest(
        @NotNull RefundMethod method,
        @NotBlank @Size(max = 100) String bankName,
        @NotBlank @Size(max = 150) String accountHolderName,
        @NotBlank @Size(min = 4, max = 34) @Pattern(regexp = "^[0-9A-Za-z]+$") String accountNumber) {
    public String normalizedBankName() {
        return bankName.trim();
    }

    public String normalizedAccountHolderName() {
        return accountHolderName.trim();
    }

    public String normalizedAccountNumber() {
        return accountNumber.trim();
    }
}
