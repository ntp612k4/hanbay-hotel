package com.tpanh.hotel.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.tpanh.hotel.ai.entity.AiRequest;
import com.tpanh.hotel.ai.repository.AiRequestRepository;
import com.tpanh.hotel.common.security.CurrentUserProvider;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiRequestLoggerTest {

    @Mock
    private AiRequestRepository repository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Test
    void logsSuccessfulResponseWithAuthenticatedUserAndDuration() {
        UUID userId = UUID.randomUUID();
        Instant finishedAt = Instant.parse("2026-08-21T08:00:02Z");
        AiRequestLogger logger =
                new AiRequestLogger(repository, currentUserProvider, Clock.fixed(finishedAt, ZoneOffset.UTC));
        logger.success(
                "Tìm phòng",
                "Có 2 hạng phòng phù hợp",
                new AiRequestLogger.RequestContext(finishedAt.minusSeconds(2), userId));

        ArgumentCaptor<AiRequest> captor = ArgumentCaptor.forClass(AiRequest.class);
        verify(repository).save(captor.capture());
        AiRequest request = captor.getValue();
        assertThat(request.getUserId()).isEqualTo(userId);
        assertThat(request.getFeature()).isEqualTo("ROOM_RECOMMENDATION");
        assertThat(request.getPrompt()).isEqualTo("[REDACTED]");
        assertThat(request.getResponse()).isNull();
        assertThat(request.isSuccessful()).isTrue();
        assertThat(request.getDurationMs()).isEqualTo(2000);
    }

    @Test
    void logsFailureWithoutLosingTheOriginalError() {
        Instant now = Instant.parse("2026-08-21T08:00:00Z");
        AiRequestLogger logger = new AiRequestLogger(repository, currentUserProvider, Clock.fixed(now, ZoneOffset.UTC));
        logger.failure(
                "Xem booking",
                new IllegalStateException("Provider unavailable"),
                new AiRequestLogger.RequestContext(now, null));

        ArgumentCaptor<AiRequest> captor = ArgumentCaptor.forClass(AiRequest.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().isSuccessful()).isFalse();
        assertThat(captor.getValue().getErrorMessage()).isEqualTo("IllegalStateException");
        assertThat(captor.getValue().getResponse()).isNull();
    }
}
