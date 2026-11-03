package com.tpanh.hotel.payment.controller;

import com.tpanh.hotel.common.response.ApiResponse;
import com.tpanh.hotel.common.response.PageResponse;
import com.tpanh.hotel.payment.domain.PaymentStatus;
import com.tpanh.hotel.payment.domain.WebhookProcessingStatus;
import com.tpanh.hotel.payment.dto.request.ConfirmRefundRequest;
import com.tpanh.hotel.payment.dto.response.MonitoredPaymentResponse;
import com.tpanh.hotel.payment.dto.response.MonitoredWebhookEventResponse;
import com.tpanh.hotel.payment.dto.response.PaymentMonitoringSummaryResponse;
import com.tpanh.hotel.payment.dto.response.RefundRequestResponse;
import com.tpanh.hotel.payment.dto.response.WebhookEventDetailResponse;
import com.tpanh.hotel.payment.service.PaymentMonitoringService;
import com.tpanh.hotel.payment.service.RefundManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.prefix}/payment-monitoring")
@RequiredArgsConstructor
@Tag(name = "Giám sát thanh toán", description = "API giám sát thanh toán và webhook dành cho quản trị")
public class PaymentMonitoringController {

    private final PaymentMonitoringService paymentMonitoringService;
    private final RefundManagementService refundManagementService;

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Lấy số liệu tổng quan thanh toán và webhook")
    public ApiResponse<PaymentMonitoringSummaryResponse> getSummary() {
        return ApiResponse.success(paymentMonitoringService.getSummary());
    }

    @GetMapping("/payments")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Tra cứu danh sách payment")
    public ApiResponse<PageResponse<MonitoredPaymentResponse>> getPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) PaymentStatus status) {
        return ApiResponse.success(paymentMonitoringService.getPayments(page, size, keyword, status));
    }

    @GetMapping("/webhook-events")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Tra cứu danh sách webhook event không bao gồm raw payload")
    public ApiResponse<PageResponse<MonitoredWebhookEventResponse>> getWebhookEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) WebhookProcessingStatus status) {
        return ApiResponse.success(paymentMonitoringService.getWebhookEvents(page, size, keyword, status));
    }

    @GetMapping("/webhook-events/{eventId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xem chi tiết và raw payload của webhook event")
    public ApiResponse<WebhookEventDetailResponse> getWebhookEvent(@PathVariable UUID eventId) {
        return ApiResponse.success(paymentMonitoringService.getWebhookEvent(eventId));
    }

    @GetMapping("/refunds")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Lấy danh sách yêu cầu hoàn tiền đang chờ")
    public ApiResponse<PageResponse<RefundRequestResponse>> getPendingRefunds(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(refundManagementService.getPendingRefunds(page, size));
    }

    @org.springframework.web.bind.annotation.PostMapping("/refunds/{refundId}/confirm")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Xác nhận đã hoàn tiền cho khách")
    public ApiResponse<RefundRequestResponse> confirmRefund(
            @PathVariable UUID refundId, @RequestBody @Valid ConfirmRefundRequest request) {
        return ApiResponse.success(
                "Xác nhận hoàn tiền thành công", refundManagementService.confirmRefund(refundId, request));
    }
}
