package com.tpanh.hotel.booking;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tpanh.hotel.authentication.service.impl.JwtService;
import com.tpanh.hotel.authentication.service.impl.JwtService.ValidatedJwt;
import com.tpanh.hotel.booking.controller.BookingController;
import com.tpanh.hotel.booking.domain.BookingStatus;
import com.tpanh.hotel.booking.dto.request.CancelBookingRequest;
import com.tpanh.hotel.booking.dto.response.CancelBookingResponse;
import com.tpanh.hotel.booking.dto.response.CancellationEligibilityResponse;
import com.tpanh.hotel.booking.dto.response.RefundStatus;
import com.tpanh.hotel.booking.service.BookingService;
import com.tpanh.hotel.common.config.SecurityConfig;
import com.tpanh.hotel.common.security.JwtAccessDeniedHandler;
import com.tpanh.hotel.common.security.JwtAuthenticationEntryPoint;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = BookingController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173")
class BookingCancellationApiTest {

    private static final String TOKEN = "valid-customer-token";
    private static final String BOOKING_CODE = "BK20260812000001";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMappingContext;

    @Test
    void anonymousUserCannotCheckCancellationPolicy() throws Exception {
        mockMvc.perform(get("/api/v1/bookings/{bookingCode}/cancellation", BOOKING_CODE))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));

        verifyNoInteractions(bookingService);
    }

    @Test
    void returnsCancellationEligibilityForAuthenticatedGuest() throws Exception {
        authenticate();
        when(bookingService.getCancellationEligibility(BOOKING_CODE))
                .thenReturn(new CancellationEligibilityResponse(
                        true, null, null, 50, 1_000_000, "VND", "Đủ điều kiện hoàn 50%"));

        mockMvc.perform(get("/api/v1/bookings/{bookingCode}/cancellation", BOOKING_CODE)
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.canCancel").value(true))
                .andExpect(jsonPath("$.data.refundPercent").value(50));
    }

    @Test
    void cancelsBookingForAuthenticatedGuest() throws Exception {
        authenticate();
        UUID bookingId = UUID.randomUUID();
        when(bookingService.cancelBooking(eq(bookingId), any(CancelBookingRequest.class)))
                .thenReturn(new CancelBookingResponse(
                        bookingId,
                        BOOKING_CODE,
                        BookingStatus.CANCELLED,
                        Instant.parse("2026-08-12T07:00:00Z"),
                        RefundStatus.PENDING,
                        50,
                        1_000_000,
                        "VND"));

        mockMvc.perform(
                        post("/api/v1/bookings/{bookingId}/cancel", bookingId)
                                .header("Authorization", "Bearer " + TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"reason":"Đổi kế hoạch","refundInformation":{"method":"BANK_TRANSFER",
                                "bankName":"Vietcombank","accountHolderName":"TRAN PHUOC ANH","accountNumber":"12345678"}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.refundAmount").value(1_000_000));
    }

    private void authenticate() {
        when(jwtService.validate(TOKEN))
                .thenReturn(Optional.of(new ValidatedJwt(UUID.randomUUID().toString(), List.of())));
    }
}
