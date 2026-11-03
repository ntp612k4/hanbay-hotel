package com.tpanh.hotel.payment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tpanh.hotel.authentication.service.AuthenticationRateLimitService;
import com.tpanh.hotel.booking.domain.BookingStatus;
import com.tpanh.hotel.common.exception.GlobalExceptionHandler;
import com.tpanh.hotel.payment.domain.PaymentStatus;
import com.tpanh.hotel.payment.dto.request.CreatePaymentRequest;
import com.tpanh.hotel.payment.dto.response.CreatePaymentRequestResponse;
import com.tpanh.hotel.payment.dto.response.PaymentStatusResponse;
import com.tpanh.hotel.payment.exception.PaymentBookingNotFoundException;
import com.tpanh.hotel.payment.service.PaymentService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class BookingPaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private AuthenticationRateLimitService rateLimitService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new BookingPaymentController(paymentService, rateLimitService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addPlaceholderValue("api.prefix", "/api/v1")
                .build();
    }

    @Test
    void createsPaymentRequest() throws Exception {
        UUID bookingId = UUID.fromString("8a1f6e4c-5b3a-4d9d-9e3d-4c8a3a9a1001");
        UUID paymentId = UUID.fromString("0c34c243-94b5-4ff5-a0d4-5cd6fa4e0a91");
        when(paymentService.createPaymentRequest(any(CreatePaymentRequest.class), nullable(String.class)))
                .thenReturn(new CreatePaymentRequestResponse(
                        paymentId,
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
                .andExpect(jsonPath("$.data.paymentId").value(paymentId.toString()))
                .andExpect(jsonPath("$.data.bookingId").value(bookingId.toString()))
                .andExpect(jsonPath("$.data.transactionRef").value("PAY-20260813080000-ABC12345"))
                .andExpect(jsonPath("$.data.paymentUrl").isNotEmpty())
                .andExpect(jsonPath("$.data.bankCode").value("TESTBANK"))
                .andExpect(jsonPath("$.data.accountNumber").value("0000000000"))
                .andExpect(jsonPath("$.data.accountName").value("HANBAY HOTEL DEV"))
                .andExpect(jsonPath("$.data.transferContent").value("PAY-20260813080000-ABC12345"));

        verify(paymentService).createPaymentRequest(any(CreatePaymentRequest.class), nullable(String.class));
    }

    @Test
    void missingBookingIdReturnsValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/payments/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.bookingId").exists());
    }

    @Test
    void getsPaymentStatusWithBookingAccessToken() throws Exception {
        UUID paymentId = UUID.fromString("0c34c243-94b5-4ff5-a0d4-5cd6fa4e0a91");
        UUID bookingId = UUID.fromString("8a1f6e4c-5b3a-4d9d-9e3d-4c8a3a9a1001");
        Instant expiredAt = Instant.parse("2026-08-14T08:15:00Z");
        when(paymentService.getPaymentStatus(paymentId, "booking-access-token"))
                .thenReturn(new PaymentStatusResponse(
                        paymentId,
                        bookingId,
                        "BK20260814000201",
                        "PAY-20260814070244-E02D78616064F1D9FA5F7DD96F876D",
                        PaymentStatus.PENDING,
                        BookingStatus.PENDING,
                        "Junior Suite",
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 9, 5),
                        2,
                        1,
                        4,
                        2_450_000L,
                        0L,
                        "VND",
                        null,
                        null,
                        null,
                        "https://qr.example/payment",
                        "Vietcombank",
                        "0000000002",
                        "HANBAY HOTEL",
                        "PAY-20260814070244-E02D78616064F1D9FA5F7DD96F876D",
                        expiredAt,
                        null));

        mockMvc.perform(get("/api/v1/payments/{paymentId}/status", paymentId)
                        .header("X-Booking-Access-Token", "booking-access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentId").value(paymentId.toString()))
                .andExpect(jsonPath("$.data.bookingId").value(bookingId.toString()))
                .andExpect(jsonPath("$.data.bookingCode").value("BK20260814000201"))
                .andExpect(jsonPath("$.data.transactionRef").value("PAY-20260814070244-E02D78616064F1D9FA5F7DD96F876D"))
                .andExpect(jsonPath("$.data.paymentStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.bookingStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.roomTypeName").value("Junior Suite"))
                .andExpect(jsonPath("$.data.checkInDate").value("2026-09-01"))
                .andExpect(jsonPath("$.data.checkOutDate").value("2026-09-05"))
                .andExpect(jsonPath("$.data.adultCount").value(2))
                .andExpect(jsonPath("$.data.childCount").value(1))
                .andExpect(jsonPath("$.data.totalNights").value(4))
                .andExpect(jsonPath("$.data.amount").value(2_450_000L))
                .andExpect(jsonPath("$.data.paidAmount").value(0L))
                .andExpect(jsonPath("$.data.currency").value("VND"))
                .andExpect(jsonPath("$.data.paymentUrl").value("https://qr.example/payment"))
                .andExpect(
                        jsonPath("$.data.transferContent").value("PAY-20260814070244-E02D78616064F1D9FA5F7DD96F876D"))
                .andExpect(jsonPath("$.data.expiredAt").value("2026-08-14T08:15:00Z"))
                .andExpect(jsonPath("$.data.paidAt").isEmpty());

        verify(paymentService).getPaymentStatus(paymentId, "booking-access-token");
    }

    @Test
    void invalidPaymentIdReturnsValidationError() throws Exception {
        mockMvc.perform(get("/api/v1/payments/not-a-uuid/status"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.paymentId").exists());
    }

    @Test
    void hiddenPaymentReturnsNotFound() throws Exception {
        UUID paymentId = UUID.fromString("0c34c243-94b5-4ff5-a0d4-5cd6fa4e0a91");
        when(paymentService.getPaymentStatus(paymentId, "invalid-token"))
                .thenThrow(new PaymentBookingNotFoundException("Không tìm thấy khoản thanh toán."));

        mockMvc.perform(get("/api/v1/payments/{paymentId}/status", paymentId)
                        .header("X-Booking-Access-Token", "invalid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PAYMENT_BOOKING_NOT_FOUND"));
    }
}
