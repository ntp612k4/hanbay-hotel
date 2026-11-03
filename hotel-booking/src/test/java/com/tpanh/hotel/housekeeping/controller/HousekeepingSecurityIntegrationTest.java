package com.tpanh.hotel.housekeeping.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tpanh.hotel.authentication.service.impl.JwtService;
import com.tpanh.hotel.authentication.service.impl.JwtService.ValidatedJwt;
import com.tpanh.hotel.common.config.SecurityConfig;
import com.tpanh.hotel.common.response.PageResponse;
import com.tpanh.hotel.common.security.JwtAccessDeniedHandler;
import com.tpanh.hotel.common.security.JwtAuthenticationEntryPoint;
import com.tpanh.hotel.housekeeping.dto.response.DirtyRoomResponse;
import com.tpanh.hotel.housekeeping.dto.response.DirtyRoomTypeSummaryResponse;
import com.tpanh.hotel.housekeeping.service.HousekeepingService;
import com.tpanh.hotel.room.domain.RoomStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = HousekeepingController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173")
class HousekeepingSecurityIntegrationTest {

    private static final String PATH = "/api/v1/housekeeping/dirty-rooms";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private HousekeepingService housekeepingService;

    @MockitoBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMappingContext;

    @Test
    void anonymousRequestReturnsUnauthorizedWithoutCallingService() throws Exception {
        mockMvc.perform(get(PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));

        verifyNoInteractions(housekeepingService);
    }

    @Test
    void anonymousCannotCompleteCleaning() throws Exception {
        mockMvc.perform(patch("/api/v1/housekeeping/rooms/10000000-0000-0000-0000-000000000001/complete-cleaning"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));

        verifyNoInteractions(housekeepingService);
    }

    @Test
    void invalidOrExpiredTokenReturnsUnauthorized() throws Exception {
        when(jwtService.validate("rejected-token")).thenReturn(Optional.empty());

        mockMvc.perform(get(PATH).header("Authorization", "Bearer rejected-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));

        verifyNoInteractions(housekeepingService);
    }

    @Test
    void housekeeperCanViewDirtyRooms() throws Exception {
        authenticate("housekeeper-token", "HOUSEKEEPER");
        when(housekeepingService.getDirtyRooms(0, 20))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true, true));

        mockMvc.perform(get(PATH).header("Authorization", "Bearer housekeeper-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"RECEPTIONIST", "MANAGER", "ADMIN", "GUEST"})
    void deniedRolesReceiveForbiddenWithoutCallingService(String role) throws Exception {
        authenticate("denied-token", role);

        mockMvc.perform(get(PATH).header("Authorization", "Bearer denied-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));

        verifyNoInteractions(housekeepingService);
    }

    @Test
    void multiRoleUserCanAccessWhenHousekeeperRoleIsPresent() throws Exception {
        authenticate("multi-role-token", "GUEST", "HOUSEKEEPER");
        when(housekeepingService.getDirtyRooms(0, 20))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true, true));

        mockMvc.perform(get(PATH).header("Authorization", "Bearer multi-role-token"))
                .andExpect(status().isOk());
    }

    @Test
    void housekeeperCanCompleteCleaning() throws Exception {
        UUID roomId = UUID.fromString("10000000-0000-0000-0000-000000000001");
        authenticate("housekeeper-complete-token", "HOUSEKEEPER");
        when(housekeepingService.completeCleaning(roomId))
                .thenReturn(new DirtyRoomResponse(
                        roomId,
                        "DLX-101",
                        1,
                        RoomStatus.AVAILABLE,
                        null,
                        new DirtyRoomTypeSummaryResponse(
                                UUID.fromString("20000000-0000-0000-0000-000000000001"), "DLX", "Deluxe hướng biển")));

        mockMvc.perform(patch("/api/v1/housekeeping/rooms/{roomId}/complete-cleaning", roomId)
                        .header("Authorization", "Bearer housekeeper-complete-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("AVAILABLE"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"RECEPTIONIST", "MANAGER", "ADMIN", "GUEST"})
    void deniedRolesCannotStartCleaning(String role) throws Exception {
        authenticate("denied-mutation-token", role);

        mockMvc.perform(patch("/api/v1/housekeeping/rooms/10000000-0000-0000-0000-000000000001/start-cleaning")
                        .header("Authorization", "Bearer denied-mutation-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));

        verifyNoInteractions(housekeepingService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"RECEPTIONIST", "MANAGER", "ADMIN", "GUEST"})
    void deniedRolesCannotCompleteCleaning(String role) throws Exception {
        authenticate("denied-complete-token", role);

        mockMvc.perform(patch("/api/v1/housekeeping/rooms/10000000-0000-0000-0000-000000000001/complete-cleaning")
                        .header("Authorization", "Bearer denied-complete-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));

        verifyNoInteractions(housekeepingService);
    }

    private void authenticate(String token, String... roles) {
        when(jwtService.validate(token)).thenReturn(Optional.of(new ValidatedJwt("staff-id", List.of(roles))));
    }
}
