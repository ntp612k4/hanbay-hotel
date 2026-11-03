package com.tpanh.hotel.payment.repository;

import com.tpanh.hotel.payment.domain.PaymentRefund;
import com.tpanh.hotel.payment.domain.PaymentRefundStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRefundRepository extends JpaRepository<PaymentRefund, UUID> {

    @EntityGraph(attributePaths = {"payment", "payment.booking"})
    Page<PaymentRefund> findAllByStatus(PaymentRefundStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            select refund
            from PaymentRefund refund
            join fetch refund.payment payment
            join fetch payment.booking booking
            where refund.id = :refundId
            """)
    Optional<PaymentRefund> findByIdForUpdate(@Param("refundId") UUID refundId);
}
