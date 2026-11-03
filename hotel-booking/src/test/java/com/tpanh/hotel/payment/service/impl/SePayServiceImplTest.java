package com.tpanh.hotel.payment.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tpanh.hotel.common.exception.RequestValidationException;
import com.tpanh.hotel.payment.config.SePayProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SePayServiceImplTest {

    private SePayServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SePayServiceImpl(
                new SePayProperties("MB", "0123456789", "HANBAY HOTEL", "https://qr.example", "secret-key"));
    }

    @Test
    void allowsZeroPaymentAmountForFullyDiscountedBooking() {
        var result = service.generateQr(0, "HBABC123456");

        assertThat(result.qrUrl()).contains("amount=0");
    }

    @Test
    void rejectsNegativePaymentAmount() {
        assertThatThrownBy(() -> service.generateQr(-1, "HBABC123456"))
                .isInstanceOf(RequestValidationException.class)
                .hasMessage("Số tiền thanh toán không được âm")
                .extracting("field")
                .isEqualTo("amount");
    }
}
