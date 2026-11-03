package com.tpanh.hotel.payment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tpanh.hotel.common.exception.GlobalExceptionHandler;
import com.tpanh.hotel.payment.dto.request.SePayWebhookRequest;
import com.tpanh.hotel.payment.exception.InvalidWebhookApiKeyException;
import com.tpanh.hotel.payment.service.PaymentWebhookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class SePayWebhookControllerTest {

    private static final String VALID_PAYLOAD =
            """
            {
              "id": 12345,
              "gateway": "MBBank",
              "transactionDate": "2026-08-08 16:30:00",
              "accountNumber": "0123456789",
              "subAccount": null,
              "code": null,
              "content": "HBABC123456",
              "transferType": "in",
              "transferAmount": 1500000,
              "accumulated": 2000000,
              "description": "Payment for booking",
              "referenceCode": "FT2622012345"
            }
            """;

    @Mock
    private PaymentWebhookService paymentWebhookService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SePayWebhookController(paymentWebhookService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addPlaceholderValue("api.prefix", "/api/v1")
                .build();
    }

    @Test
    void validWebhookReturnsAcknowledgement() throws Exception {
        mockMvc.perform(post("/api/v1/payments/sepay/webhook")
                        .header("Authorization", "Apikey secret-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PAYLOAD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Webhook đã được tiếp nhận"));

        verify(paymentWebhookService).handleSePayWebhook(any(), any(SePayWebhookRequest.class));
    }

    @Test
    void invalidApiKeyReturnsUnauthorized() throws Exception {
        doThrow(new InvalidWebhookApiKeyException())
                .when(paymentWebhookService)
                .handleSePayWebhook(any(), any(SePayWebhookRequest.class));

        mockMvc.perform(post("/api/v1/payments/sepay/webhook")
                        .header("Authorization", "Apikey wrong-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PAYLOAD))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_WEBHOOK_API_KEY"));
    }

    @Test
    void invalidPayloadReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/payments/sepay/webhook")
                        .header("Authorization", "Apikey secret-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }
}
