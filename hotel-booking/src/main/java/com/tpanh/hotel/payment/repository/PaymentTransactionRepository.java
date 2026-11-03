package com.tpanh.hotel.payment.repository;

import com.tpanh.hotel.payment.domain.PaymentTransaction;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

    Optional<PaymentTransaction> findByProviderTransactionId(String providerTransactionId);

    boolean existsByProviderTransactionId(String providerTransactionId);

    @Query(
            value =
                    """
            select coalesce(sum(pt.amount), 0)
              from payment_transactions pt
             where pt.payment_id = :paymentId
               and pt.type = 'PAYMENT'
               and pt.status = 'SUCCESS'
            """,
            nativeQuery = true)
    long sumSuccessfulPaymentAmount(@Param("paymentId") UUID paymentId);
}
