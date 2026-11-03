package com.tpanh.hotel.frontdesk.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tpanh.hotel.booking.domain.BookingStatus;
import com.tpanh.hotel.common.exception.GlobalExceptionHandler;
import com.tpanh.hotel.common.exception.RequestValidationException;
import com.tpanh.hotel.common.response.PageResponse;
import com.tpanh.hotel.frontdesk.dto.response.ArrivalRoomTypeResponse;
import com.tpanh.hotel.frontdesk.dto.response.PrimaryGuestSummaryResponse;
import com.tpanh.hotel.frontdesk.dto.response.TodayArrivalResponse;
import com.tpanh.hotel.frontdesk.service.FrontDeskService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class FrontDeskControllerTest {

    private static final UUID BOOKING_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID ROOM_TYPE_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    @Mock
    private FrontDeskService frontDeskService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FrontDeskController(frontDeskService))
                .addPlaceholderValue("api.prefix", "/api/v1")
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsTodayArrivalsUsingDefaultPagination() throws Exception {
        TodayArrivalResponse item = arrival(primaryGuest());
        when(frontDeskService.getTodayArrivals(0, 20))
                .thenReturn(new PageResponse<>(List.of(item), 0, 20, 1, 1, true, true));

        mockMvc.perform(get("/api/v1/front-desk/arrivals/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Lấy danh sách khách đến hôm nay thành công"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].bookingId").value(BOOKING_ID.toString()))
                .andExpect(jsonPath("$.data.content[0].bookingStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.content[0].primaryGuest.fullName").value("Nguyễn Văn A"))
                .andExpect(jsonPath("$.data.content[0].roomType.code").value("DLX"));

        verify(frontDeskService).getTodayArrivals(0, 20);
    }

    @Test
    void bindsCustomPaginationAndReturnsEmptyPage() throws Exception {
        when(frontDeskService.getTodayArrivals(2, 5))
                .thenReturn(new PageResponse<>(List.of(), 2, 5, 0, 0, false, true));

        mockMvc.perform(get("/api/v1/front-desk/arrivals/today")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(0));

        verify(frontDeskService).getTodayArrivals(2, 5);
    }

    @Test
    void serializesMissingPrimaryGuestAsNull() throws Exception {
        when(frontDeskService.getTodayArrivals(0, 20))
                .thenReturn(new PageResponse<>(List.of(arrival(null)), 0, 20, 1, 1, true, true));

        mockMvc.perform(get("/api/v1/front-desk/arrivals/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].primaryGuest").isEmpty());
    }

    @Test
    void mapsServicePaginationValidationToBadRequest() throws Exception {
        when(frontDeskService.getTodayArrivals(-1, 20))
                .thenThrow(new RequestValidationException("page", "Chỉ số trang phải lớn hơn hoặc bằng 0"));

        mockMvc.perform(get("/api/v1/front-desk/arrivals/today").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.page").value("Chỉ số trang phải lớn hơn hoặc bằng 0"));
    }

    @Test
    void rejectsNonNumericPaginationBeforeCallingService() throws Exception {
        mockMvc.perform(get("/api/v1/front-desk/arrivals/today").param("page", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.page").value("page phải có kiểu dữ liệu hợp lệ: int"));

        mockMvc.perform(get("/api/v1/front-desk/arrivals/today").param("size", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.size").value("size phải có kiểu dữ liệu hợp lệ: int"));

        verifyNoInteractions(frontDeskService);
    }

    private TodayArrivalResponse arrival(PrimaryGuestSummaryResponse guest) {
        return new TodayArrivalResponse(
                BOOKING_ID,
                "HAN-20260819-001",
                guest,
                new ArrivalRoomTypeResponse(ROOM_TYPE_ID, "DLX", "Deluxe"),
                LocalDate.of(2026, 8, 19),
                LocalDate.of(2026, 8, 21),
                2,
                1,
                BookingStatus.CONFIRMED,
                Instant.parse("2026-08-10T08:30:00Z"));
    }

    private PrimaryGuestSummaryResponse primaryGuest() {
        return new PrimaryGuestSummaryResponse("Nguyễn Văn A", "0901234567", "guest@example.com");
    }
}
