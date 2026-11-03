package com.tpanh.hotel.booking.dto.request;

import com.tpanh.hotel.payment.dto.request.RefundInformationRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

public record CancelBookingRequest(
        @Size(max = 500, message = "Lý do hủy không được vượt quá 500 ký tự") String reason,
        @Valid RefundInformationRequest refundInformation) {
    public CancelBookingRequest(String reason) {
        this(reason, null);
    }

    public String normalizedReason() {
        if (reason == null || reason.isBlank()) {
            return "Khách hàng thay đổi kế hoạch";
        }
        return reason.trim();
    }
}
