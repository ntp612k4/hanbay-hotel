package com.tpanh.hotel.ai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ai_requests")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AiRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false, length = 80)
    private String feature;

    @Column(nullable = false, columnDefinition = "text")
    private String prompt;

    @Column(columnDefinition = "text")
    private String response;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(nullable = false)
    private boolean successful;

    @Column(name = "duration_ms", nullable = false)
    private long durationMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public void completeSuccessfully(String response, long durationMs) {
        this.response = response;
        this.errorMessage = null;
        this.successful = true;
        this.durationMs = durationMs;
    }

    public void completeWithFailure(String errorMessage, long durationMs) {
        this.response = null;
        this.errorMessage = errorMessage;
        this.successful = false;
        this.durationMs = durationMs;
    }
}
