package com.tpanh.hotel.booking;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tpanh.hotel.authentication.service.impl.JwtService;
import com.tpanh.hotel.authentication.service.impl.JwtService.ValidatedJwt;
import com.tpanh.hotel.booking.controller.BookingController;
import com.tpanh.hotel.booking.domain.BookingStatus;
import com.tpanh.hotel.booking.dto.response.BookingHistoryItemResponse;
import com.tpanh.hotel.booking.dto.response.BookingHistoryRoomTypeResponse;
import com.tpanh.hotel.booking.service.BookingService;
import com.tpanh.hotel.common.config.SecurityConfig;
import com.tpanh.hotel.common.response.PageResponse;
import com.tpanh.hotel.common.security.JwtAccessDeniedHandler;
import com.tpanh.hotel.common.security.JwtAuthenticationEntryPoint;
import com.tpanh.hotel.payment.domain.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = BookingController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173")
class BookingHistoryApiTest {

    private static final String TOKEN = "valid-customer-token";
    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID BOOKING_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID ROOM_TYPE_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMappingContext;

    @Test
    void returns401WhenAccessTokenIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/bookings/my-bookings"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/v1/bookings/my-bookings"));

        verifyNoInteractions(bookingService);
    }

    @Test
    void bindsFiltersAndReturnsPaginatedBookingHistory() throws Exception {
        authenticate();
        UUID paymentId = UUID.randomUUID();
        BookingHistoryItemResponse item = new BookingHistoryItemResponse(
                BOOKING_ID,
                "BK20260810000001",
                new BookingHistoryRoomTypeResponse(ROOM_TYPE_ID, "Phòng Deluxe", "https://example.com/deluxe.jpg"),
                LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 12),
                2,
                1,
                2,
                new BigDecimal("3500000.00"),
                BigDecimal.ZERO,
                "VND",
                BookingStatus.CONFIRMED,
                paymentId,
                PaymentStatus.SUCCESS,
                null,
                null,
                Instant.parse("2026-08-09T03:30:00Z"));
        when(bookingService.getCurrentUserBookingHistory(1, 5, BookingStatus.CONFIRMED))
                .thenReturn(new PageResponse<>(List.of(item), 1, 5, 6, 2, false, true));

        mockMvc.perform(get("/api/v1/bookings/my-bookings")
                        .header("Authorization", "Bearer " + TOKEN)
                        .queryParam("page", "1")
                        .queryParam("size", "5")
                        .queryParam("status", "CONFIRMED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Lấy lịch sử đặt phòng thành công"))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(5))
                .andExpect(jsonPath("$.data.content[0].paymentId").value(paymentId.toString()))
                .andExpect(jsonPath("$.data.totalElements").value(6))
                .andExpect(jsonPath("$.data.first").value(false))
                .andExpect(jsonPath("$.data.last").value(true))
                .andExpect(jsonPath("$.data.content[0].bookingId").value(BOOKING_ID.toString()))
                .andExpect(jsonPath("$.data.content[0].roomType.name").value("Phòng Deluxe"))
                .andExpect(jsonPath("$.data.content[0].bookingStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.content[0].paymentStatus").value("SUCCESS"));

        verify(bookingService).getCurrentUserBookingHistory(1, 5, BookingStatus.CONFIRMED);
    }

    @Test
    void returns400WhenStatusIsUnknown() throws Exception {
        authenticate();

        mockMvc.perform(get("/api/v1/bookings/my-bookings")
                        .header("Authorization", "Bearer " + TOKEN)
                        .queryParam("status", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.status").exists());

        verifyNoInteractions(bookingService);
    }

    @Test
    void returnsCurrentUsersBookingDetail() throws Exception {
        authenticate();
        BookingHistoryItemResponse item = new BookingHistoryItemResponse(
                BOOKING_ID,
                "BK20260810000001",
                new BookingHistoryRoomTypeResponse(ROOM_TYPE_ID, "Phòng Deluxe", null),
                LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 12),
                2,
                0,
                2,
                new BigDecimal("3500000"),
                BigDecimal.ZERO,
                "VND",
                BookingStatus.CONFIRMED,
                null,
                null,
                null,
                null,
                Instant.parse("2026-08-09T03:30:00Z"));
        when(bookingService.getCurrentUserBooking("BK20260810000001")).thenReturn(item);

        mockMvc.perform(get("/api/v1/bookings/BK20260810000001").header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookingId").value(BOOKING_ID.toString()));
    }

    private void authenticate() {
        when(jwtService.validate(TOKEN)).thenReturn(Optional.of(new ValidatedJwt(USER_ID.toString(), List.of())));
    }
}
