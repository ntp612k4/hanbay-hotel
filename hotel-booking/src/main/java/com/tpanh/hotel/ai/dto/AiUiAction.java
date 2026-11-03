package com.tpanh.hotel.ai.dto;

import java.time.LocalDate;
import java.util.UUID;

public record AiUiAction(Type type, String label, Params params) {

    public enum Type {
        CONTINUE_BOOKING,
        REVIEW_CANCELLATION,
        SELECT_ADDONS,
        VIEW_PAYMENT
    }

    public record Params(
            UUID roomTypeId,
            LocalDate checkIn,
            LocalDate checkOut,
            Integer adults,
            Integer children,
            String bookingCode,
            UUID paymentId) {}
}
