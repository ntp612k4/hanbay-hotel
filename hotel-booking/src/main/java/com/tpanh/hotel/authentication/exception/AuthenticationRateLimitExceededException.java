package com.tpanh.hotel.authentication.exception;

import com.tpanh.hotel.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class AuthenticationRateLimitExceededException extends DomainException {

    private final long retryAfterSeconds;

    public AuthenticationRateLimitExceededException() {
        this(60);
    }

    public AuthenticationRateLimitExceededException(long retryAfterSeconds) {
        super("AUTH_RATE_LIMITED", "Bạn đã gửi quá nhiều yêu cầu. Vui lòng thử lại sau.", HttpStatus.TOO_MANY_REQUESTS);
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
