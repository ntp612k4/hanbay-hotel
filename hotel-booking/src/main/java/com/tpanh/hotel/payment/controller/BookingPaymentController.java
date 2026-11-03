package com.tpanh.hotel.payment.controller;

import com.tpanh.hotel.authentication.service.AuthenticationRateLimitService;
import com.tpanh.hotel.common.response.ApiResponse;
import com.tpanh.hotel.payment.dto.request.CreatePaymentRequest;
import com.tpanh.hotel.payment.dto.response.CreatePaymentRequestResponse;
import com.tpanh.hotel.payment.dto.response.PaymentStatusResponse;
import com.tpanh.hotel.payment.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.prefix}/payments")
@RequiredArgsConstructor
public class BookingPaymentController {

    private final PaymentService paymentService;
    private final AuthenticationRateLimitService rateLimitService;

    @PostMapping("/requests")
    public ResponseEntity<ApiResponse<CreatePaymentRequestResponse>> createPaymentRequest(
            @RequestHeader(value = "X-Booking-Access-Token", required = false) String bookingAccessToken,
            @RequestBody @Valid CreatePaymentRequest request,
            HttpServletRequest servletRequest) {
        rateLimitService.check("payment-request", clientIdentity(servletRequest), 20, Duration.ofMinutes(1));
        CreatePaymentRequestResponse response = paymentService.createPaymentRequest(request, bookingAccessToken);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo yêu cầu thanh toán thành công", response));
    }

    @GetMapping("/{paymentId}/status")
    public ApiResponse<PaymentStatusResponse> getPaymentStatus(
            @PathVariable UUID paymentId,
            @RequestHeader(value = "X-Booking-Access-Token", required = false) String bookingAccessToken,
            HttpServletRequest servletRequest) {
        rateLimitService.check("payment-status", clientIdentity(servletRequest), 120, Duration.ofMinutes(1));
        return ApiResponse.success(
                "Lấy trạng thái thanh toán thành công", paymentService.getPaymentStatus(paymentId, bookingAccessToken));
    }

    private String clientIdentity(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
