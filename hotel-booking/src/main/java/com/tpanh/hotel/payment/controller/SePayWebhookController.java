package com.tpanh.hotel.payment.controller;

import com.tpanh.hotel.payment.dto.request.SePayWebhookRequest;
import com.tpanh.hotel.payment.dto.response.WebhookResponse;
import com.tpanh.hotel.payment.service.PaymentWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.prefix}/payments/sepay")
@RequiredArgsConstructor
@Tag(name = "SePay", description = "Webhook giao dịch ngân hàng từ SePay")
public class SePayWebhookController {

    private final PaymentWebhookService paymentWebhookService;

    @PostMapping("/webhook")
    @Operation(
            summary = "Nhận webhook giao dịch ngân hàng từ SePay",
            description =
                    "Xác thực API key, chống nhận trùng, đối soát giao dịch và tự động xác nhận đặt phòng khi đã nhận đủ tiền",
            security = {})
    public ResponseEntity<WebhookResponse> receiveWebhook(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody @Valid SePayWebhookRequest request) {
        paymentWebhookService.handleSePayWebhook(authorization, request);
        return ResponseEntity.ok(new WebhookResponse(true, "Webhook đã được tiếp nhận"));
    }
}
