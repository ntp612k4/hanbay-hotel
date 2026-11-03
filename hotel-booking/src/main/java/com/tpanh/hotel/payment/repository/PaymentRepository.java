package com.tpanh.hotel.payment.repository;

import com.tpanh.hotel.payment.domain.Payment;
import com.tpanh.hotel.payment.domain.PaymentStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByPaymentCode(String paymentCode);

    @Query("select payment.booking.id from Payment payment where payment.paymentCode = :paymentCode")
    Optional<UUID> findBookingIdByPaymentCode(@Param("paymentCode") String paymentCode);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            """
            update Payment payment
               set payment.status = com.tpanh.hotel.payment.domain.PaymentStatus.EXPIRED
             where payment.id = :paymentId
               and payment.status = com.tpanh.hotel.payment.domain.PaymentStatus.PENDING
               and payment.expiresAt <= :now
            """)
    int markPendingExpired(@Param("paymentId") UUID paymentId, @Param("now") java.time.Instant now);

    boolean existsByPaymentCode(String paymentCode);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    Optional<Payment> findFirstByBookingIdAndStatusOrderByCreatedAtDesc(UUID bookingId, PaymentStatus status);

    Optional<Payment> findFirstByBookingIdAndStatusOrderByPaidAtDesc(UUID bookingId, PaymentStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Payment> findFirstByBookingIdOrderByCreatedAtDesc(UUID bookingId);

    List<Payment> findAllByBookingIdInOrderByCreatedAtDesc(Collection<UUID> bookingIds);

    boolean existsByBookingIdAndStatus(UUID bookingId, PaymentStatus status);

    List<Payment> findAllByBookingIdAndStatusOrderByCreatedAtDesc(UUID bookingId, PaymentStatus status);

    long countByStatus(PaymentStatus status);

    @Query(
            value =
                    """
                select payment
                  from Payment payment
                  join fetch payment.booking booking
                 where (:keyword = ''
                        or lower(payment.paymentCode) like lower(concat('%', :keyword, '%'))
                        or lower(booking.bookingCode) like lower(concat('%', :keyword, '%')))
                """,
            countQuery =
                    """
                select count(payment)
                  from Payment payment
                  join payment.booking booking
                 where (:keyword = ''
                        or lower(payment.paymentCode) like lower(concat('%', :keyword, '%'))
                        or lower(booking.bookingCode) like lower(concat('%', :keyword, '%')))
                """)
    Page<Payment> findForMonitoring(@Param("keyword") String keyword, Pageable pageable);

    @Query(
            value =
                    """
                select payment
                  from Payment payment
                  join fetch payment.booking booking
                 where payment.status = :status
                   and (:keyword = ''
                        or lower(payment.paymentCode) like lower(concat('%', :keyword, '%'))
                        or lower(booking.bookingCode) like lower(concat('%', :keyword, '%')))
                """,
            countQuery =
                    """
                select count(payment)
                  from Payment payment
                  join payment.booking booking
                 where payment.status = :status
                   and (:keyword = ''
                        or lower(payment.paymentCode) like lower(concat('%', :keyword, '%'))
                        or lower(booking.bookingCode) like lower(concat('%', :keyword, '%')))
                """)
    Page<Payment> findForMonitoringByStatus(
            @Param("keyword") String keyword, @Param("status") PaymentStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select payment from Payment payment where payment.paymentCode = :paymentCode")
    Optional<Payment> findByPaymentCodeForUpdate(@Param("paymentCode") String paymentCode);
}
