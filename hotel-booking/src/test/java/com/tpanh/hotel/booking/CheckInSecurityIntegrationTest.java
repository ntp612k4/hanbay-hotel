package com.tpanh.hotel.booking;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tpanh.hotel.authentication.service.impl.JwtService;
import com.tpanh.hotel.authentication.service.impl.JwtService.ValidatedJwt;
import com.tpanh.hotel.booking.controller.CheckInController;
import com.tpanh.hotel.booking.domain.BookingStatus;
import com.tpanh.hotel.booking.dto.response.AssignedRoomResponse;
import com.tpanh.hotel.booking.dto.response.CheckInGuestResponse;
import com.tpanh.hotel.booking.service.CheckInService;
import com.tpanh.hotel.common.config.SecurityConfig;
import com.tpanh.hotel.common.security.JwtAccessDeniedHandler;
import com.tpanh.hotel.common.security.JwtAuthenticationEntryPoint;
import com.tpanh.hotel.room.domain.RoomStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CheckInController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173")
class CheckInSecurityIntegrationTest {

    private static final UUID BOOKING_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID ROOM_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final String BOOKING_CODE = "BK20260819000001";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CheckInService checkInService;

    @Test
    void anonymousRequestReturnsUnauthorized() throws Exception {
        performCheckIn(null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));
    }

    @Test
    void guestRoleReturnsForbidden() throws Exception {
        authenticate("guest-token", "MANAGER");

        performCheckIn("guest-token")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    @Test
    void receptionistCanCheckInGuest() throws Exception {
        authenticate("receptionist-token", "RECEPTIONIST");
        when(checkInService.checkIn(eq(BOOKING_CODE), any()))
                .thenReturn(new CheckInGuestResponse(
                        BOOKING_ID,
                        "BK20260819000001",
                        BookingStatus.CHECKED_IN,
                        new AssignedRoomResponse(ROOM_ID, "0501", 5, RoomStatus.OCCUPIED),
                        Instant.parse("2026-08-19T07:00:00Z")));

        performCheckIn("receptionist-token")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookingStatus").value("CHECKED_IN"));
    }

    @Test
    void receptionistCanListAvailableRooms() throws Exception {
        authenticate("receptionist-token", "RECEPTIONIST");

        mockMvc.perform(get("/api/v1/bookings/{bookingCode}/available-rooms", BOOKING_CODE)
                        .header("Authorization", "Bearer receptionist-token"))
                .andExpect(status().isOk());
    }

    @Test
    void managerCannotListAvailableRooms() throws Exception {
        authenticate("manager-token", "MANAGER");

        mockMvc.perform(get("/api/v1/bookings/{bookingCode}/available-rooms", BOOKING_CODE)
                        .header("Authorization", "Bearer manager-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    private org.springframework.test.web.servlet.ResultActions performCheckIn(String token) throws Exception {
        var request = post("/api/v1/bookings/{bookingCode}/check-in", BOOKING_CODE)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"roomId\":\"" + ROOM_ID + "\"}");
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return mockMvc.perform(request);
    }

    private void authenticate(String token, String... roles) {
        when(jwtService.validate(token))
                .thenReturn(Optional.of(new ValidatedJwt(UUID.randomUUID().toString(), List.of(roles))));
    }
}
