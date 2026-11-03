package com.tpanh.hotel.payment.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tpanh.hotel.authentication.service.impl.JwtService;
import com.tpanh.hotel.authentication.service.impl.JwtService.ValidatedJwt;
import com.tpanh.hotel.common.config.SecurityConfig;
import com.tpanh.hotel.common.security.JwtAccessDeniedHandler;
import com.tpanh.hotel.common.security.JwtAuthenticationEntryPoint;
import com.tpanh.hotel.payment.dto.response.PaymentMonitoringSummaryResponse;
import com.tpanh.hotel.payment.service.PaymentMonitoringService;
import com.tpanh.hotel.payment.service.RefundManagementService;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PaymentMonitoringController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173")
class PaymentMonitoringSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private PaymentMonitoringService paymentMonitoringService;

    @MockitoBean
    private RefundManagementService refundManagementService;

    @Test
    void anonymousRequestShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/payment-monitoring/summary"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));
    }

    @Test
    void invalidOrExpiredTokenShouldReturnUnauthorized() throws Exception {
        when(jwtService.validate("rejected-token")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/payment-monitoring/summary").header("Authorization", "Bearer rejected-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));
    }

    @Test
    void managerShouldAccessSummary() throws Exception {
        authenticate("manager-token", "MANAGER");
        when(paymentMonitoringService.getSummary()).thenReturn(summary());

        mockMvc.perform(get("/api/v1/payment-monitoring/summary").header("Authorization", "Bearer manager-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void receptionistShouldReceiveForbidden() throws Exception {
        authenticate("receptionist-token", "RECEPTIONIST");

        mockMvc.perform(get("/api/v1/payment-monitoring/summary").header("Authorization", "Bearer receptionist-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    @Test
    void multiRoleUserShouldAccessWhenOneRoleIsAllowed() throws Exception {
        authenticate("multi-role-token", "RECEPTIONIST", "MANAGER");
        when(paymentMonitoringService.getSummary()).thenReturn(summary());

        mockMvc.perform(get("/api/v1/payment-monitoring/summary").header("Authorization", "Bearer multi-role-token"))
                .andExpect(status().isOk());
    }

    @Test
    void managerShouldNotAccessAdminOnlyWebhookDetail() throws Exception {
        authenticate("manager-token", "MANAGER");

        mockMvc.perform(get("/api/v1/payment-monitoring/webhook-events/96a8f221-c24d-4a52-a91d-b110fbde8c0e")
                        .header("Authorization", "Bearer manager-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    @Test
    void adminShouldAccessAdminOnlyWebhookDetail() throws Exception {
        authenticate("admin-token", "ADMIN");

        mockMvc.perform(get("/api/v1/payment-monitoring/webhook-events/96a8f221-c24d-4a52-a91d-b110fbde8c0e")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @MethodSource("sharedMonitoringAccessCases")
    void adminAndManagerShouldAccessSharedMonitoringEndpoints(String role, String path) throws Exception {
        authenticate("allowed-token", role);

        mockMvc.perform(get(path).header("Authorization", "Bearer allowed-token"))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @MethodSource("deniedMonitoringAccessCases")
    void receptionistAndHousekeeperShouldNotAccessSharedMonitoringEndpoints(String role, String path) throws Exception {
        authenticate("denied-token", role);

        mockMvc.perform(get(path).header("Authorization", "Bearer denied-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    private void authenticate(String token, String... roles) {
        when(jwtService.validate(token)).thenReturn(Optional.of(new ValidatedJwt("staff-id", List.of(roles))));
    }

    private PaymentMonitoringSummaryResponse summary() {
        return new PaymentMonitoringSummaryResponse(10, 8, 12, 1);
    }

    private static Stream<Arguments> sharedMonitoringAccessCases() {
        return Stream.of("ADMIN", "MANAGER").flatMap(role -> Stream.of(
                        "/api/v1/payment-monitoring/summary",
                        "/api/v1/payment-monitoring/payments",
                        "/api/v1/payment-monitoring/webhook-events")
                .map(path -> Arguments.of(role, path)));
    }

    private static Stream<Arguments> deniedMonitoringAccessCases() {
        return Stream.of("RECEPTIONIST", "HOUSEKEEPER").flatMap(role -> Stream.of(
                        "/api/v1/payment-monitoring/summary",
                        "/api/v1/payment-monitoring/payments",
                        "/api/v1/payment-monitoring/webhook-events")
                .map(path -> Arguments.of(role, path)));
    }
}
