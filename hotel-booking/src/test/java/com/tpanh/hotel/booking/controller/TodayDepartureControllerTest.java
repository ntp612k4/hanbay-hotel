package com.tpanh.hotel.booking.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tpanh.hotel.authentication.service.impl.JwtService;
import com.tpanh.hotel.authentication.service.impl.JwtService.ValidatedJwt;
import com.tpanh.hotel.booking.domain.BookingStatus;
import com.tpanh.hotel.booking.dto.response.TodayDepartureResponse;
import com.tpanh.hotel.booking.service.TodayDepartureService;
import com.tpanh.hotel.common.config.SecurityConfig;
import com.tpanh.hotel.common.response.PageResponse;
import com.tpanh.hotel.common.security.JwtAccessDeniedHandler;
import com.tpanh.hotel.common.security.JwtAuthenticationEntryPoint;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = TodayDepartureController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173")
class TodayDepartureControllerTest {

    private static final String ENDPOINT = "/api/v1/front-desk/departures/today";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private TodayDepartureService todayDepartureService;

    @Test
    void anonymousRequestShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get(ENDPOINT))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"RECEPTIONIST", "MANAGER", "ADMIN"})
    void authorizedStaffShouldAccessTodayDepartures(String role) throws Exception {
        authenticate("allowed-token", role);
        when(todayDepartureService.getTodayDepartures(0, 20)).thenReturn(emptyPage());

        mockMvc.perform(get(ENDPOINT).header("Authorization", "Bearer allowed-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Lấy danh sách khách trả phòng hôm nay thành công"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20));
    }

    @Test
    void housekeeperShouldReceiveForbidden() throws Exception {
        authenticate("housekeeper-token", "HOUSEKEEPER");

        mockMvc.perform(get(ENDPOINT).header("Authorization", "Bearer housekeeper-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    @Test
    void requestShouldForwardPaginationAndReturnDepartureData() throws Exception {
        authenticate("receptionist-token", "RECEPTIONIST");
        UUID bookingId = UUID.fromString("f7906823-32ab-4faa-99bf-a9f4535eb17b");
        TodayDepartureResponse departure = new TodayDepartureResponse(
                bookingId,
                "BK202608190001",
                "Nguyen Van An",
                "0901234567",
                "Deluxe",
                LocalDate.of(2026, 8, 17),
                LocalDate.of(2026, 8, 19),
                2,
                1,
                BookingStatus.CHECKED_IN,
                5_000_000L,
                3_500_000L,
                1_500_000L);
        PageResponse<TodayDepartureResponse> response = new PageResponse<>(List.of(departure), 1, 5, 6, 2, false, true);
        when(todayDepartureService.getTodayDepartures(1, 5)).thenReturn(response);

        mockMvc.perform(get(ENDPOINT)
                        .param("page", "1")
                        .param("size", "5")
                        .header("Authorization", "Bearer receptionist-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].bookingId").value(bookingId.toString()))
                .andExpect(jsonPath("$.data.content[0].bookingCode").value("BK202608190001"))
                .andExpect(jsonPath("$.data.content[0].outstandingAmount").value(1_500_000))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(5));

        verify(todayDepartureService).getTodayDepartures(1, 5);
    }

    private void authenticate(String token, String... roles) {
        when(jwtService.validate(token)).thenReturn(Optional.of(new ValidatedJwt("staff-id", List.of(roles))));
    }

    private PageResponse<TodayDepartureResponse> emptyPage() {
        return new PageResponse<>(List.of(), 0, 20, 0, 0, true, true);
    }
}
