package com.tpanh.hotel.room;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tpanh.hotel.authentication.service.impl.JwtService;
import com.tpanh.hotel.authentication.service.impl.JwtService.ValidatedJwt;
import com.tpanh.hotel.common.config.SecurityConfig;
import com.tpanh.hotel.common.security.JwtAccessDeniedHandler;
import com.tpanh.hotel.common.security.JwtAuthenticationEntryPoint;
import com.tpanh.hotel.room.controller.RoomMaintenanceController;
import com.tpanh.hotel.room.domain.RoomStatus;
import com.tpanh.hotel.room.dto.response.RoomMaintenanceResponse;
import com.tpanh.hotel.room.service.RoomMaintenanceService;
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

@WebMvcTest(controllers = RoomMaintenanceController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173")
class RoomMaintenanceSecurityIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private RoomMaintenanceService roomMaintenanceService;

    @MockitoBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMappingContext;

    @Test
    void anonymousIsUnauthorized() throws Exception {
        perform(null).andExpect(status().isUnauthorized());
    }

    @Test
    void onlyManagerIsAllowed() throws Exception {
        for (String role : List.of("RECEPTIONIST", "HOUSEKEEPER", "ADMIN")) {
            authenticate(role, role);
            perform(role).andExpect(status().isForbidden());
        }
        authenticate("MANAGER", "MANAGER");
        when(roomMaintenanceService.changeMaintenanceStatus(
                        "0501", com.tpanh.hotel.room.domain.RoomMaintenanceAction.START_MAINTENANCE))
                .thenReturn(new RoomMaintenanceResponse(UUID.randomUUID(), "0501", RoomStatus.MAINTENANCE));
        perform("MANAGER").andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.ResultActions perform(String token) throws Exception {
        var request = patch("/api/v1/rooms/0501/maintenance")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"START_MAINTENANCE\"}");
        if (token != null) request.header("Authorization", "Bearer " + token);
        return mockMvc.perform(request);
    }

    private void authenticate(String token, String role) {
        when(jwtService.validate(token))
                .thenReturn(Optional.of(new ValidatedJwt(UUID.randomUUID().toString(), List.of(role))));
    }
}
