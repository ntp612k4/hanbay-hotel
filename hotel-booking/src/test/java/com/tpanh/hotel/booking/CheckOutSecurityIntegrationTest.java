package com.tpanh.hotel.booking;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tpanh.hotel.authentication.service.impl.JwtService;
import com.tpanh.hotel.authentication.service.impl.JwtService.ValidatedJwt;
import com.tpanh.hotel.booking.controller.CheckOutController;
import com.tpanh.hotel.booking.domain.BookingStatus;
import com.tpanh.hotel.booking.dto.request.CheckOutGuestRequest;
import com.tpanh.hotel.booking.dto.response.AssignedRoomResponse;
import com.tpanh.hotel.booking.dto.response.CheckOutGuestResponse;
import com.tpanh.hotel.booking.service.CheckOutService;
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
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CheckOutController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173")
class CheckOutSecurityIntegrationTest {

    private static final UUID BOOKING_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID ROOM_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CheckOutService checkOutService;

    @MockitoBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMappingContext;

    @Test
    void anonymousRequestReturnsUnauthorized() throws Exception {
        performCheckOut(null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));
    }

    @Test
    void housekeeperRoleReturnsForbidden() throws Exception {
        authenticate("housekeeper-token", "HOUSEKEEPER");

        performCheckOut("housekeeper-token")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    @Test
    void receptionistCanCheckOutGuest() throws Exception {
        authenticate("receptionist-token", "RECEPTIONIST");
        when(checkOutService.checkOut(
                        eq(BOOKING_ID), eq(new CheckOutGuestRequest("Khách đã trả đủ chìa khóa", null, null, null))))
                .thenReturn(response());

        performCheckOut("receptionist-token")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookingStatus").value("CHECKED_OUT"));
    }

    @Test
    void managerCanCheckOutGuest() throws Exception {
        authenticate("manager-token", "MANAGER");
        when(checkOutService.checkOut(
                        eq(BOOKING_ID), eq(new CheckOutGuestRequest("Khách đã trả đủ chìa khóa", null, null, null))))
                .thenReturn(response());

        performCheckOut("manager-token")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.room.status").value("DIRTY"));
    }

    @Test
    void adminCanCheckOutGuest() throws Exception {
        authenticate("admin-token", "ADMIN");
        when(checkOutService.checkOut(
                        eq(BOOKING_ID), eq(new CheckOutGuestRequest("Khách đã trả đủ chìa khóa", null, null, null))))
                .thenReturn(response());

        performCheckOut("admin-token")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.outstandingAmount").value(0));
    }

    private org.springframework.test.web.servlet.ResultActions performCheckOut(String token) throws Exception {
        var request = patch("/api/v1/bookings/{bookingId}/check-out", BOOKING_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"note\":\"Khách đã trả đủ chìa khóa\"}");
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return mockMvc.perform(request);
    }

    private void authenticate(String token, String... roles) {
        when(jwtService.validate(token))
                .thenReturn(Optional.of(new ValidatedJwt(UUID.randomUUID().toString(), List.of(roles))));
    }

    private CheckOutGuestResponse response() {
        return new CheckOutGuestResponse(
                BOOKING_ID,
                "BK20260824000001",
                BookingStatus.CHECKED_OUT,
                new AssignedRoomResponse(ROOM_ID, "0501", 5, RoomStatus.DIRTY),
                Instant.parse("2026-08-24T03:30:00Z"),
                5_635_000L,
                5_635_000L,
                0L,
                0L,
                null,
                0L);
    }
}
