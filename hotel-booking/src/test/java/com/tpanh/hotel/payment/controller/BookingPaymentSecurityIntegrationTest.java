package com.tpanh.hotel.payment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tpanh.hotel.authentication.service.AuthenticationRateLimitService;
import com.tpanh.hotel.authentication.service.impl.JwtService;
import com.tpanh.hotel.booking.domain.BookingStatus;
import com.tpanh.hotel.common.config.SecurityConfig;
import com.tpanh.hotel.common.security.JwtAccessDeniedHandler;
import com.tpanh.hotel.common.security.JwtAuthenticationEntryPoint;
import com.tpanh.hotel.payment.domain.PaymentStatus;
import com.tpanh.hotel.payment.dto.request.CreatePaymentRequest;
import com.tpanh.hotel.payment.dto.response.CreatePaymentRequestResponse;
import com.tpanh.hotel.payment.dto.response.PaymentStatusResponse;
import com.tpanh.hotel.payment.service.PaymentService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = BookingPaymentController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173")
class BookingPaymentSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private AuthenticationRateLimitService rateLimitService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void anonymousGuestCanCreatePaymentRequest() throws Exception {
        UUID bookingId = UUID.fromString("8a1f6e4c-5b3a-4d9d-9e3d-4c8a3a9a1001");
        when(paymentService.createPaymentRequest(any(CreatePaymentRequest.class), nullable(String.class)))
                .thenReturn(new CreatePaymentRequestResponse(
                        UUID.fromString("0c34c243-94b5-4ff5-a0d4-5cd6fa4e0a91"),
                        bookingId,
                        "BKG-000001",
                        "PAY-20260813080000-ABC12345",
                        2_450_000L,
                        "VND",
                        PaymentStatus.PENDING,
                        "https://vietqr.app/img?bank=TESTBANK&acc=0000000000&amount=2450000",
                        Instant.parse("2026-08-13T08:15:00Z"),
                        "TESTBANK",
                        "0000000000",
                        "HANBAY HOTEL DEV",
                        "PAY-20260813080000-ABC12345"));

        mockMvc.perform(
                        post("/api/v1/payments/requests")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "bookingId": "8a1f6e4c-5b3a-4d9d-9e3d-4c8a3a9a1001"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentUrl").isNotEmpty());
    }

    @Test
    void anonymousGuestCanReadPaymentResultWithBookingAccessToken() throws Exception {
        UUID paymentId = UUID.fromString("0c34c243-94b5-4ff5-a0d4-5cd6fa4e0a91");
        UUID bookingId = UUID.fromString("8a1f6e4c-5b3a-4d9d-9e3d-4c8a3a9a1001");
        when(paymentService.getPaymentStatus(paymentId, "booking-access-token"))
                .thenReturn(new PaymentStatusResponse(
                        paymentId,
                        bookingId,
                        "BK20260814000201",
                        "PAY-20260814070244-E02D78616064F1D9FA5F7DD96F876D",
                        PaymentStatus.SUCCESS,
                        BookingStatus.CONFIRMED,
                        "Junior Suite",
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 9, 5),
                        2,
                        1,
                        4,
                        2_450_000L,
                        2_450_000L,
                        "VND",
                        null,
                        null,
                        null,
                        "https://qr.example/payment",
                        "Vietcombank",
                        "0000000002",
                        "HANBAY HOTEL",
                        "PAY-20260814070244-E02D78616064F1D9FA5F7DD96F876D",
                        Instant.parse("2026-08-14T08:15:00Z"),
                        Instant.parse("2026-08-14T08:05:00Z")));

        mockMvc.perform(get("/api/v1/payments/{paymentId}/status", paymentId)
                        .header("X-Booking-Access-Token", "booking-access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.data.bookingStatus").value("CONFIRMED"));
    }
}
