package com.tpanh.hotel.payment.repository;

import com.tpanh.hotel.payment.domain.PaymentWebhookEvent;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEvent, UUID> {

    @Query(value = "select pg_advisory_xact_lock(hashtext(:deduplicationKey))", nativeQuery = true)
    void acquireDeduplicationLock(@Param("deduplicationKey") String deduplicationKey);

    Optional<PaymentWebhookEvent> findByDeduplicationKey(String deduplicationKey);

    boolean existsByDeduplicationKey(String deduplicationKey);

    long countByProcessingStatus(com.tpanh.hotel.payment.domain.WebhookProcessingStatus status);

    @Query(
            value =
                    """
                select event
                  from PaymentWebhookEvent event
                  left join fetch event.payment payment
                 where (:keyword = ''
                        or lower(event.eventId) like lower(concat('%', :keyword, '%'))
                        or lower(coalesce(payment.paymentCode, '')) like lower(concat('%', :keyword, '%')))
                """,
            countQuery =
                    """
                select count(event)
                  from PaymentWebhookEvent event
                  left join event.payment payment
                 where (:keyword = ''
                        or lower(event.eventId) like lower(concat('%', :keyword, '%'))
                        or lower(coalesce(payment.paymentCode, '')) like lower(concat('%', :keyword, '%')))
                """)
    Page<PaymentWebhookEvent> findForMonitoring(@Param("keyword") String keyword, Pageable pageable);

    @Query(
            value =
                    """
                select event
                  from PaymentWebhookEvent event
                  left join fetch event.payment payment
                 where event.processingStatus = :status
                   and (:keyword = ''
                        or lower(event.eventId) like lower(concat('%', :keyword, '%'))
                        or lower(coalesce(payment.paymentCode, '')) like lower(concat('%', :keyword, '%')))
                """,
            countQuery =
                    """
                select count(event)
                  from PaymentWebhookEvent event
                  left join event.payment payment
                 where event.processingStatus = :status
                   and (:keyword = ''
                        or lower(event.eventId) like lower(concat('%', :keyword, '%'))
                        or lower(coalesce(payment.paymentCode, '')) like lower(concat('%', :keyword, '%')))
                """)
    Page<PaymentWebhookEvent> findForMonitoringByStatus(
            @Param("keyword") String keyword,
            @Param("status") com.tpanh.hotel.payment.domain.WebhookProcessingStatus status,
            Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "payment")
    Optional<PaymentWebhookEvent> findById(UUID id);

    @Modifying
    @Query(
            """
        UPDATE PaymentWebhookEvent event
           SET event.processingStatus =
                   WebhookProcessingStatus.PROCESSING,
               event.processingAttempts =
                   event.processingAttempts + 1,
               event.processingStartedAt = :startedAt,
               event.lastError = null
         WHERE event.id = :eventId
           AND event.processingStatus in (
               WebhookProcessingStatus.RECEIVED,
               WebhookProcessingStatus.FAILED
           )
    """)
    int claimForProcessing(@Param("eventId") UUID eventId, @Param("startedAt") Instant startedAt);

    @Modifying
    @Query(
            """
        UPDATE PaymentWebhookEvent event
           SET event.processingStatus =
                   WebhookProcessingStatus.RECEIVED,
               event.processingStartedAt = null
         WHERE event.processingStatus =
                   WebhookProcessingStatus.PROCESSING
           AND event.processingStartedAt < :cutoff
    """)
    int resetStaleProcessingEvents(@Param("cutoff") Instant cutoff);
}
