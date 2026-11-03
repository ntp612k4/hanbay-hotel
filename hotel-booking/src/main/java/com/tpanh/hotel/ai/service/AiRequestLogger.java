package com.tpanh.hotel.ai.service;

import com.tpanh.hotel.ai.entity.AiRequest;
import com.tpanh.hotel.ai.repository.AiRequestRepository;
import com.tpanh.hotel.common.security.CurrentUserProvider;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiRequestLogger {

    static final String ROOM_RECOMMENDATION_FEATURE = "ROOM_RECOMMENDATION";
    static final String REDACTED = "[REDACTED]";

    private final AiRequestRepository repository;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public RequestContext start() {
        return new RequestContext(clock.instant(), currentUserId());
    }

    public void success(String prompt, String response, RequestContext context) {
        save(ROOM_RECOMMENDATION_FEATURE, prompt, response, null, true, context);
    }

    public void failure(String prompt, Throwable exception, RequestContext context) {
        save(ROOM_RECOMMENDATION_FEATURE, prompt, null, exception.getClass().getSimpleName(), false, context);
    }

    private void save(
            String feature,
            String prompt,
            String response,
            String errorMessage,
            boolean successful,
            RequestContext context) {
        Instant finishedAt = clock.instant();
        save(AiRequest.builder()
                .userId(context.userId())
                .feature(feature)
                .prompt(REDACTED)
                .response(null)
                .errorMessage(errorMessage)
                .successful(successful)
                .durationMs(durationSince(context.startedAt()))
                .createdAt(finishedAt)
                .build());
    }

    private UUID currentUserId() {
        try {
            return currentUserProvider.currentUserId().orElse(null);
        } catch (RuntimeException exception) {
            log.warn("Không thể ghi audit log cho AI request", exception);
            return null;
        }
    }

    private long durationSince(Instant startedAt) {
        return Math.max(0, Duration.between(startedAt, clock.instant()).toMillis());
    }

    private boolean save(AiRequest request) {
        try {
            repository.save(request);
            return true;
        } catch (RuntimeException exception) {
            log.warn("Không thể ghi audit log cho AI request", exception);
            return false;
        }
    }

    public record RequestContext(Instant startedAt, UUID userId) {}
}
