package com.tpanh.hotel.reporting.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tpanh.hotel.authentication.service.impl.JwtService;
import com.tpanh.hotel.authentication.service.impl.JwtService.ValidatedJwt;
import com.tpanh.hotel.common.config.SecurityConfig;
import com.tpanh.hotel.common.exception.RequestValidationException;
import com.tpanh.hotel.common.security.JwtAccessDeniedHandler;
import com.tpanh.hotel.common.security.JwtAuthenticationEntryPoint;
import com.tpanh.hotel.reporting.dto.response.DashboardReportResponse;
import com.tpanh.hotel.reporting.dto.response.DashboardReportResponse.BookingSummary;
import com.tpanh.hotel.reporting.dto.response.DashboardReportResponse.OccupancySummary;
import com.tpanh.hotel.reporting.dto.response.DashboardReportResponse.Period;
import com.tpanh.hotel.reporting.dto.response.DashboardReportResponse.RevenueSummary;
import com.tpanh.hotel.reporting.service.DashboardReportService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = DashboardReportController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173")
class DashboardReportControllerTest {

    private static final String PATH = "/api/v1/reports/dashboard?from=2026-08-01&to=2026-08-04";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private DashboardReportService reportService;

    @Test
    void anonymousRequestReturnsUnauthorized() throws Exception {
        mockMvc.perform(get(PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"MANAGER", "ADMIN"})
    void managerAndAdminCanAccessDashboard(String role) throws Exception {
        authenticate(role);
        when(reportService.getDashboardReport(any())).thenReturn(response());

        mockMvc.perform(get(PATH).header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Lấy báo cáo dashboard thành công"))
                .andExpect(jsonPath("$.data.period.currency").value("VND"))
                .andExpect(jsonPath("$.data.revenue.netRevenue").value(800000))
                .andExpect(jsonPath("$.data.occupancy.occupancyRate").value(75.00))
                .andExpect(jsonPath("$.data.bookings.totalBookings").value(4));
    }

    @ParameterizedTest
    @ValueSource(strings = {"RECEPTIONIST", "HOUSEKEEPER"})
    void otherStaffRolesReceiveForbidden(String role) throws Exception {
        authenticate(role);

        mockMvc.perform(get(PATH).header("Authorization", "Bearer token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    @Test
    void missingDateReturnsValidationError() throws Exception {
        authenticate("MANAGER");

        mockMvc.perform(get("/api/v1/reports/dashboard?to=2026-08-04").header("Authorization", "Bearer token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.from").exists());
    }

    @Test
    void malformedDateReturnsValidationError() throws Exception {
        authenticate("MANAGER");

        mockMvc.perform(get("/api/v1/reports/dashboard?from=not-a-date&to=2026-08-04")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void lowercaseCurrencyReturnsValidationError() throws Exception {
        authenticate("MANAGER");

        mockMvc.perform(get(PATH + "&currency=vnd").header("Authorization", "Bearer token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.currency").exists());
    }

    @Test
    void invalidDateRangeReturnsValidationError() throws Exception {
        authenticate("MANAGER");
        when(reportService.getDashboardReport(any()))
                .thenThrow(new RequestValidationException("to", "Ngày kết thúc phải sau ngày bắt đầu"));

        mockMvc.perform(get(PATH).header("Authorization", "Bearer token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.to").value("Ngày kết thúc phải sau ngày bắt đầu"));
    }

    private void authenticate(String role) {
        when(jwtService.validate("token")).thenReturn(Optional.of(new ValidatedJwt("staff-id", List.of(role))));
    }

    private DashboardReportResponse response() {
        return new DashboardReportResponse(
                new Period(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 4), "VND"),
                new RevenueSummary(1_000_000, 200_000, 800_000),
                new OccupancySummary(9, 12, new BigDecimal("75.00")),
                new BookingSummary(4, 0, 2, 0, 1, 1, 0, 2_000_000, 500_000, new BigDecimal("25.00")),
                List.of(),
                List.of(),
                List.of());
    }
}
