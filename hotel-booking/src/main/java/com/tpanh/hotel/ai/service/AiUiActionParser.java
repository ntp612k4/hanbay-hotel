package com.tpanh.hotel.ai.service;

import com.tpanh.hotel.ai.dto.AiUiAction;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class AiUiActionParser {

    static final String MARKER = "@@UI_ACTION@@";
    private final ObjectMapper objectMapper;

    public ParsedResponse parse(String rawResponse) {
        int markerIndex = rawResponse.lastIndexOf(MARKER);
        if (markerIndex < 0) return new ParsedResponse(rawResponse.trim(), Optional.empty());

        String answer = rawResponse.substring(0, markerIndex).trim();
        String json = rawResponse.substring(markerIndex + MARKER.length()).trim();
        try {
            RawAction rawAction = objectMapper.readValue(json, RawAction.class);
            AiUiAction action = rawAction.toAction();
            return isValid(action)
                    ? new ParsedResponse(answer, Optional.of(withServerLabel(action)))
                    : new ParsedResponse(answer, Optional.empty());
        } catch (JacksonException exception) {
            return new ParsedResponse(answer, Optional.empty());
        }
    }

    private boolean isValid(AiUiAction action) {
        if (action == null || action.type() == null || action.params() == null) return false;
        AiUiAction.Params params = action.params();
        return switch (action.type()) {
            case CONTINUE_BOOKING -> params.roomTypeId() != null
                    && params.checkIn() != null
                    && params.checkOut() != null
                    && params.checkOut().isAfter(params.checkIn())
                    && params.adults() != null
                    && params.adults() > 0
                    && params.children() != null
                    && params.children() >= 0;
            case REVIEW_CANCELLATION, SELECT_ADDONS -> params.bookingCode() != null
                    && !params.bookingCode().isBlank();
            case VIEW_PAYMENT -> params.paymentId() != null;
        };
    }

    private AiUiAction withServerLabel(AiUiAction action) {
        String label =
                switch (action.type()) {
                    case CONTINUE_BOOKING -> "Tiếp tục đặt phòng";
                    case REVIEW_CANCELLATION -> "Xem và xác nhận hủy";
                    case SELECT_ADDONS -> "Chọn dịch vụ";
                    case VIEW_PAYMENT -> "Xem thanh toán";
                };
        return new AiUiAction(action.type(), label, action.params());
    }

    public record ParsedResponse(String answer, Optional<AiUiAction> action) {}

    private record RawAction(
            AiUiAction.Type type,
            String label,
            AiUiAction.Params params,
            UUID roomTypeId,
            LocalDate checkIn,
            LocalDate checkOut,
            Integer adults,
            Integer children,
            String bookingCode,
            UUID paymentId) {

        private AiUiAction toAction() {
            AiUiAction.Params resolvedParams = params != null
                    ? params
                    : new AiUiAction.Params(roomTypeId, checkIn, checkOut, adults, children, bookingCode, paymentId);
            return new AiUiAction(type, label, resolvedParams);
        }
    }
}
