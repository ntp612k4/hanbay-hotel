package com.tpanh.hotel.payment;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.tpanh.hotel.booking.domain.Booking;
import com.tpanh.hotel.payment.domain.*;
import com.tpanh.hotel.payment.repository.PaymentRefundRepository;
import com.tpanh.hotel.payment.service.impl.RefundManagementServiceImpl;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;

class RefundBankInformationTest {
    @Test
    void returnsStoredRecipientBankDetailsToRefundManager() {
        var repository = mock(PaymentRefundRepository.class);
        var service = new RefundManagementServiceImpl(repository, Clock.systemUTC());
        var payment = Payment.builder()
                .booking(Booking.builder().bookingCode("BOOK-REFUND").build())
                .paymentCode("PAY-REFUND")
                .build();
        var refund = PaymentRefund.builder()
                .id(UUID.randomUUID())
                .payment(payment)
                .amount(BigDecimal.valueOf(200000))
                .bankName("Vietcombank")
                .accountHolderName("NGUYEN VAN A")
                .accountNumber("001234567890")
                .build();
        when(repository.findAllByStatus(eq(PaymentRefundStatus.PENDING), any()))
                .thenReturn(new PageImpl<>(List.of(refund)));
        var result = service.getPendingRefunds(0, 10).content().get(0);
        assertThat(result.bankName()).isEqualTo("Vietcombank");
        assertThat(result.accountHolderName()).isEqualTo("NGUYEN VAN A");
        assertThat(result.accountNumber()).isEqualTo("001234567890");
    }
}
