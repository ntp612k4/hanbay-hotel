package com.tpanh.hotel.frontdesk.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tpanh.hotel.authentication.service.impl.JwtService;
import com.tpanh.hotel.authentication.service.impl.JwtService.ValidatedJwt;
import com.tpanh.hotel.common.config.SecurityConfig;
import com.tpanh.hotel.common.response.PageResponse;
import com.tpanh.hotel.common.security.JwtAccessDeniedHandler;
import com.tpanh.hotel.common.security.JwtAuthenticationEntryPoint;
import com.tpanh.hotel.frontdesk.service.FrontDeskService;
import java.util.List;
import java.util.Optional;
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

@WebMvcTest(controllers = FrontDeskController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173")
class FrontDeskSecurityIntegrationTest {

    private static final String PATH = "/api/v1/front-desk/arrivals/today";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private FrontDeskService frontDeskService;

    @MockitoBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMappingContext;

    @Test
    void anonymousRequestReturnsUnauthorizedWithoutCallingService() throws Exception {
        mockMvc.perform(get(PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));

        verifyNoInteractions(frontDeskService);
    }

    @Test
    void invalidOrExpiredTokenReturnsUnauthorized() throws Exception {
        when(jwtService.validate("rejected-token")).thenReturn(Optional.empty());

        mockMvc.perform(get(PATH).header("Authorization", "Bearer rejected-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));

        verifyNoInteractions(frontDeskService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"RECEPTIONIST", "MANAGER", "ADMIN"})
    void allowedStaffRolesCanViewTodayArrivals(String role) throws Exception {
        authenticate("allowed-token", role);
        when(frontDeskService.getTodayArrivals(0, 20))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true, true));

        mockMvc.perform(get(PATH).header("Authorization", "Bearer allowed-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"HOUSEKEEPER", "GUEST"})
    void deniedRolesReceiveForbiddenWithoutCallingService(String role) throws Exception {
        authenticate("denied-token", role);

        mockMvc.perform(get(PATH).header("Authorization", "Bearer denied-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));

        verifyNoInteractions(frontDeskService);
    }

    @Test
    void multiRoleUserCanAccessWhenOneRoleIsAllowed() throws Exception {
        authenticate("multi-role-token", "HOUSEKEEPER", "RECEPTIONIST");
        when(frontDeskService.getTodayArrivals(0, 20))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true, true));

        mockMvc.perform(get(PATH).header("Authorization", "Bearer multi-role-token"))
                .andExpect(status().isOk());
    }

    private void authenticate(String token, String... roles) {
        when(jwtService.validate(token)).thenReturn(Optional.of(new ValidatedJwt("staff-id", List.of(roles))));
    }
}
