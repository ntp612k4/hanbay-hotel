package com.tpanh.hotel.payment.exception;

import com.tpanh.hotel.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public final class InvalidPaymentStateException extends DomainException {
    private InvalidPaymentStateException(String code, String message) {
        super(code, message, HttpStatus.CONFLICT);
    }

    public static InvalidPaymentStateException notPending(String action) {
        return new InvalidPaymentStateException(
                "PAYMENT_NOT_PENDING", "Chỉ có thể " + action + " khoản thanh toán đang chờ");
    }

    public static InvalidPaymentStateException refundNotAllowed() {
        return new InvalidPaymentStateException(
                "PAYMENT_REFUND_NOT_ALLOWED", "Trạng thái payment hiện tại không cho phép hoàn tiền");
    }

    public static InvalidPaymentStateException notExpired() {
        return new InvalidPaymentStateException("PAYMENT_NOT_EXPIRED", "Payment chưa ở trạng thái chờ và quá hạn");
    }

    public static InvalidPaymentStateException reconciliationNotAllowed() {
        return new InvalidPaymentStateException(
                "PAYMENT_RECONCILIATION_NOT_ALLOWED", "Chỉ có thể đối soát payment đã đóng hoặc thất bại");
    }
}
