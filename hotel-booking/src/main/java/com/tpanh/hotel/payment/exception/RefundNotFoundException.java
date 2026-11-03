package com.tpanh.hotel.payment.exception;

import com.tpanh.hotel.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class RefundNotFoundException extends DomainException {
    public RefundNotFoundException() {
        super("REFUND_NOT_FOUND", "Không tìm thấy yêu cầu hoàn tiền", HttpStatus.NOT_FOUND);
    }
}
