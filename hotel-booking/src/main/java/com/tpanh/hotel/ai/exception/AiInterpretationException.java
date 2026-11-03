package com.tpanh.hotel.ai.exception;

import com.tpanh.hotel.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class AiInterpretationException extends DomainException {

    private AiInterpretationException(String code, String message, HttpStatus status, Throwable cause) {
        super(code, message, status);
        initCause(cause);
    }

    public static AiInterpretationException invalidResponse() {
        return new AiInterpretationException(
                "AI_INVALID_STRUCTURED_RESPONSE",
                "AI trả về kết quả không hợp lệ. Vui lòng diễn đạt lại yêu cầu.",
                HttpStatus.BAD_GATEWAY,
                null);
    }

    public static AiInterpretationException providerFailure(Throwable cause) {
        String detail = cause.getMessage() == null ? "" : cause.getMessage().toLowerCase();
        boolean timeout = detail.contains("timeout") || detail.contains("timed out");
        return new AiInterpretationException(
                timeout ? "AI_TIMEOUT" : "AI_PROVIDER_UNAVAILABLE",
                timeout
                        ? "AI phản hồi quá thời gian. Vui lòng thử lại."
                        : "Dịch vụ AI tạm thời không khả dụng. Vui lòng thử lại.",
                timeout ? HttpStatus.GATEWAY_TIMEOUT : HttpStatus.BAD_GATEWAY,
                cause);
    }
}
