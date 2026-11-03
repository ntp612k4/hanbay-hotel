package com.tpanh.hotel.payment.service;

import com.tpanh.hotel.payment.dto.request.CreatePaymentRequest;
import com.tpanh.hotel.payment.dto.response.CreatePaymentRequestResponse;
import com.tpanh.hotel.payment.dto.response.PaymentStatusResponse;
import java.util.UUID;

public interface PaymentService {
    CreatePaymentRequestResponse createPaymentRequest(CreatePaymentRequest request, String bookingAccessToken);

    PaymentStatusResponse getPaymentStatus(UUID paymentId, String bookingAccessToken);
}
