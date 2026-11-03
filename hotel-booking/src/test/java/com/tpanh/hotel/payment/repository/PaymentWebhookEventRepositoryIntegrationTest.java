package com.tpanh.hotel.payment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.tpanh.hotel.payment.domain.PaymentWebhookEvent;
import com.tpanh.hotel.payment.domain.WebhookDeduplicationStrategy;
import com.tpanh.hotel.payment.domain.WebhookProcessingStatus;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Transactional
@Testcontainers(disabledWithoutDocker = true)
class PaymentWebhookEventRepositoryIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("pgvector/pgvector:pg17");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.docker.compose.enabled", () -> false);
    }

    @Autowired
    private PaymentWebhookEventRepository repository;

    @Test
    void monitoringQueriesWorkWithAndWithoutStatusFilter() {
        String eventId = Long.toString(Math.abs(UUID.randomUUID().getMostSignificantBits()));
        PaymentWebhookEvent event = PaymentWebhookEvent.builder()
                .eventId(eventId)
                .eventType("bank.transfer")
                .deduplicationKey("sepay:event:" + eventId)
                .deduplicationStrategy(WebhookDeduplicationStrategy.EVENT_ID)
                .signatureValid(true)
                .processingStatus(WebhookProcessingStatus.RECEIVED)
                .receivedAt(Instant.now())
                .payload(Map.of("gateway", "MBBank", "transferAmount", 1_500_000L))
                .build();
        repository.saveAndFlush(event);

        var withoutStatus = repository.findForMonitoring(eventId, PageRequest.of(0, 10));
        var withStatus =
                repository.findForMonitoringByStatus(eventId, WebhookProcessingStatus.RECEIVED, PageRequest.of(0, 10));

        assertThat(withoutStatus.getContent())
                .extracting(PaymentWebhookEvent::getEventId)
                .contains(eventId);
        assertThat(withStatus.getContent())
                .extracting(PaymentWebhookEvent::getEventId)
                .contains(eventId);
    }
}
