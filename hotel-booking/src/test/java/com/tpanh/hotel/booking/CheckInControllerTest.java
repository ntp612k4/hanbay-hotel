package com.tpanh.hotel.booking;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tpanh.hotel.booking.controller.CheckInController;
import com.tpanh.hotel.booking.domain.BookingStatus;
import com.tpanh.hotel.booking.dto.response.AssignedRoomResponse;
import com.tpanh.hotel.booking.dto.response.AvailablePhysicalRoomResponse;
import com.tpanh.hotel.booking.dto.response.CheckInGuestResponse;
import com.tpanh.hotel.booking.exception.CheckInConflictException;
import com.tpanh.hotel.booking.service.CheckInService;
import com.tpanh.hotel.room.domain.RoomStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CheckInController.class)
@AutoConfigureMockMvc(addFilters = false)
class CheckInControllerTest {

    private static final UUID BOOKING_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID ROOM_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final String BOOKING_CODE = "BK20260819000001";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CheckInService checkInService;

    @MockitoBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMappingContext;

    @Test
    void returnsCheckInResult() throws Exception {
        when(checkInService.checkIn(eq(BOOKING_CODE), any()))
                .thenReturn(new CheckInGuestResponse(
                        BOOKING_ID,
                        "BK20260819000001",
                        BookingStatus.CHECKED_IN,
                        new AssignedRoomResponse(ROOM_ID, "0501", 5, RoomStatus.OCCUPIED),
                        Instant.parse("2026-08-19T07:00:00Z")));

        mockMvc.perform(post("/api/v1/bookings/{bookingCode}/check-in", BOOKING_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomId\":\"" + ROOM_ID + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Check-in thành công"))
                .andExpect(jsonPath("$.data.bookingStatus").value("CHECKED_IN"))
                .andExpect(jsonPath("$.data.room.roomNumber").value("0501"))
                .andExpect(jsonPath("$.data.room.status").value("OCCUPIED"));
    }

    @Test
    void returns400WhenRoomIdIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/bookings/{bookingCode}/check-in", BOOKING_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.roomId").value("Room ID không được để trống"));
    }

    @Test
    void mapsBusinessConflictTo409() throws Exception {
        when(checkInService.checkIn(eq(BOOKING_CODE), any())).thenThrow(CheckInConflictException.roomNotAvailable());

        mockMvc.perform(post("/api/v1/bookings/{bookingCode}/check-in", BOOKING_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomId\":\"" + ROOM_ID + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ROOM_NOT_AVAILABLE"));
    }

    @Test
    void returnsAvailablePhysicalRooms() throws Exception {
        when(checkInService.getAvailableRooms(BOOKING_CODE))
                .thenReturn(List.of(new AvailablePhysicalRoomResponse(ROOM_ID, "0501", 5)));

        mockMvc.perform(get("/api/v1/bookings/{bookingCode}/available-rooms", BOOKING_CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].roomId").value(ROOM_ID.toString()))
                .andExpect(jsonPath("$.data[0].roomNumber").value("0501"))
                .andExpect(jsonPath("$.data[0].floor").value(5));
    }
}
