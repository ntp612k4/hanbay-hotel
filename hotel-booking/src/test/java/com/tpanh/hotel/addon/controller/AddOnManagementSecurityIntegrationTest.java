package com.tpanh.hotel.addon.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tpanh.hotel.addon.mapper.AddOnMapper;
import com.tpanh.hotel.addon.service.AddOnManagementService;
import com.tpanh.hotel.authentication.service.impl.JwtService;
import com.tpanh.hotel.authentication.service.impl.JwtService.ValidatedJwt;
import com.tpanh.hotel.common.config.SecurityConfig;
import com.tpanh.hotel.common.response.PageResponse;
import com.tpanh.hotel.common.security.JwtAccessDeniedHandler;
import com.tpanh.hotel.common.security.JwtAuthenticationEntryPoint;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(controllers = AddOnManagementController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173")
class AddOnManagementSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AddOnManagementService addOnManagementService;

    @MockitoBean
    private AddOnMapper addOnMapper;

    @Test
    void anonymousShouldReceiveUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/management/add-ons"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"RECEPTIONIST", "HOUSEKEEPER"})
    void nonManagementStaffShouldReceiveForbidden(String role) throws Exception {
        authenticate("denied-token", role);

        mockMvc.perform(get("/api/v1/management/add-ons").header("Authorization", "Bearer denied-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"MANAGER", "ADMIN"})
    void managerAndAdminShouldAccessManagementApi(String role) throws Exception {
        authenticate("allowed-token", role);
        when(addOnManagementService.findAll(
                        new com.tpanh.hotel.addon.service.model.AddOnSearchCriteria(0, 20, null, null, "name", "asc")))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true, true));

        mockMvc.perform(get("/api/v1/management/add-ons").header("Authorization", "Bearer allowed-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void authenticatedGuestWithoutStaffRoleShouldReceiveForbidden() throws Exception {
        authenticate("guest-token");

        mockMvc.perform(get("/api/v1/management/add-ons").header("Authorization", "Bearer guest-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    @Test
    void receptionistShouldBeForbiddenFromEveryWriteEndpoint() throws Exception {
        authenticate("receptionist-token", "RECEPTIONIST");
        String id = "4c2ff15b-c288-4722-b4b8-8e73d3f55337";

        assertForbidden(post("/api/v1/management/add-ons")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"CITY_TOUR\",\"name\":\"City tour\",\"price\":1}"));
        assertForbidden(put("/api/v1/management/add-ons/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"City tour\",\"price\":1}"));
        assertForbidden(patch("/api/v1/management/add-ons/{id}/status", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":false}"));
    }

    private void authenticate(String token, String... roles) {
        when(jwtService.validate(token)).thenReturn(Optional.of(new ValidatedJwt("staff-id", List.of(roles))));
    }

    private void assertForbidden(MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request.header("Authorization", "Bearer receptionist-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }
}
