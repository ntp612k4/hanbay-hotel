package com.tpanh.hotel.maintenance.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tpanh.hotel.authentication.service.impl.JwtService;
import com.tpanh.hotel.authentication.service.impl.JwtService.ValidatedJwt;
import com.tpanh.hotel.common.config.SecurityConfig;
import com.tpanh.hotel.common.security.JwtAccessDeniedHandler;
import com.tpanh.hotel.common.security.JwtAuthenticationEntryPoint;
import com.tpanh.hotel.maintenance.service.MaintenanceCostService;
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

@WebMvcTest(controllers = MaintenanceCostController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173")
class MaintenanceCostSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private MaintenanceCostService costService;

    @MockitoBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMappingContext;

    @Test
    void anonymousReceivesUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/maintenance-requests/00000000-0000-0000-0000-000000000001/cost"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));
    }

    @Test
    void nonManagerReceivesForbidden() throws Exception {
        authenticate("denied-token", "RECEPTIONIST");

        mockMvc.perform(get("/api/v1/maintenance-requests/00000000-0000-0000-0000-000000000001/cost")
                        .header("Authorization", "Bearer denied-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    @Test
    void managerCanReadCost() throws Exception {
        authenticate("manager-token", "MANAGER");

        mockMvc.perform(get("/api/v1/maintenance-requests/00000000-0000-0000-0000-000000000001/cost")
                        .header("Authorization", "Bearer manager-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void receptionistCannotRecordCost() throws Exception {
        authenticate("denied-token", "RECEPTIONIST");
        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                                        "/api/v1/maintenance-requests/00000000-0000-0000-0000-000000000001/cost")
                                .header("Authorization", "Bearer denied-token")
                                .contentType("application/json")
                                .content(
                                        "{\"amount\":100000,\"expenseDate\":\"2026-09-08\",\"note\":\"Repair\",\"expectedRevision\":0}"))
                .andExpect(status().isForbidden());
        org.mockito.Mockito.verifyNoInteractions(costService);
    }

    private void authenticate(String token, String role) {
        when(jwtService.validate(token))
                .thenReturn(Optional.of(new ValidatedJwt(UUID.randomUUID().toString(), List.of(role))));
    }
}
