package com.tpanh.hotel.housekeeping.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tpanh.hotel.common.exception.GlobalExceptionHandler;
import com.tpanh.hotel.common.exception.RequestValidationException;
import com.tpanh.hotel.common.response.PageResponse;
import com.tpanh.hotel.housekeeping.dto.response.DirtyRoomResponse;
import com.tpanh.hotel.housekeeping.dto.response.DirtyRoomTypeSummaryResponse;
import com.tpanh.hotel.housekeeping.service.HousekeepingService;
import com.tpanh.hotel.room.domain.RoomStatus;
import com.tpanh.hotel.room.exception.RoomNotFoundException;
import com.tpanh.hotel.room.exception.RoomStateConflictException;
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
class HousekeepingControllerTest {

    private static final UUID ROOM_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID ROOM_TYPE_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    @Mock
    private HousekeepingService housekeepingService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new HousekeepingController(housekeepingService))
                .addPlaceholderValue("api.prefix", "/api/v1")
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsDirtyRoomsUsingDefaultPagination() throws Exception {
        DirtyRoomResponse item = dirtyRoom("Kiểm tra minibar");
        when(housekeepingService.getDirtyRooms(0, 20))
                .thenReturn(new PageResponse<>(List.of(item), 0, 20, 1, 1, true, true));

        mockMvc.perform(get("/api/v1/housekeeping/dirty-rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Lấy danh sách phòng cần dọn thành công"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].roomId").value(ROOM_ID.toString()))
                .andExpect(jsonPath("$.data.content[0].roomNumber").value("DLX-101"))
                .andExpect(jsonPath("$.data.content[0].floor").value(1))
                .andExpect(jsonPath("$.data.content[0].status").value("DIRTY"))
                .andExpect(jsonPath("$.data.content[0].note").value("Kiểm tra minibar"))
                .andExpect(jsonPath("$.data.content[0].roomType.id").value(ROOM_TYPE_ID.toString()))
                .andExpect(jsonPath("$.data.content[0].roomType.code").value("DLX"));

        verify(housekeepingService).getDirtyRooms(0, 20);
    }

    @Test
    void bindsCustomPaginationAndReturnsEmptyPage() throws Exception {
        when(housekeepingService.getDirtyRooms(2, 5))
                .thenReturn(new PageResponse<>(List.of(), 2, 5, 0, 0, false, true));

        mockMvc.perform(get("/api/v1/housekeeping/dirty-rooms")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.size").value(5))
                .andExpect(jsonPath("$.data.totalElements").value(0));

        verify(housekeepingService).getDirtyRooms(2, 5);
    }

    @Test
    void serializesNullableNote() throws Exception {
        when(housekeepingService.getDirtyRooms(0, 20))
                .thenReturn(new PageResponse<>(List.of(dirtyRoom(null)), 0, 20, 1, 1, true, true));

        mockMvc.perform(get("/api/v1/housekeeping/dirty-rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].note").isEmpty());
    }

    @Test
    void mapsServicePaginationValidationToBadRequest() throws Exception {
        when(housekeepingService.getDirtyRooms(-1, 20))
                .thenThrow(new RequestValidationException("page", "Chỉ số trang phải lớn hơn hoặc bằng 0"));

        mockMvc.perform(get("/api/v1/housekeeping/dirty-rooms").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.page").value("Chỉ số trang phải lớn hơn hoặc bằng 0"));
    }

    @Test
    void rejectsNonNumericPaginationBeforeCallingService() throws Exception {
        mockMvc.perform(get("/api/v1/housekeeping/dirty-rooms").param("page", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.page").value("page phải có kiểu dữ liệu hợp lệ: int"));

        mockMvc.perform(get("/api/v1/housekeeping/dirty-rooms").param("size", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.size").value("size phải có kiểu dữ liệu hợp lệ: int"));

        verifyNoInteractions(housekeepingService);
    }

    @Test
    void startsAndCompletesCleaning() throws Exception {
        DirtyRoomResponse cleaning = new DirtyRoomResponse(
                ROOM_ID,
                "DLX-101",
                1,
                RoomStatus.CLEANING,
                null,
                new DirtyRoomTypeSummaryResponse(ROOM_TYPE_ID, "DLX", "Deluxe hướng biển"));
        DirtyRoomResponse available =
                new DirtyRoomResponse(ROOM_ID, "DLX-101", 1, RoomStatus.AVAILABLE, null, cleaning.roomType());
        when(housekeepingService.startCleaning(ROOM_ID)).thenReturn(cleaning);
        when(housekeepingService.completeCleaning(ROOM_ID)).thenReturn(available);

        mockMvc.perform(patch("/api/v1/housekeeping/rooms/{roomId}/start-cleaning", ROOM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLEANING"));
        mockMvc.perform(patch("/api/v1/housekeeping/rooms/{roomId}/complete-cleaning", ROOM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Đã hoàn tất dọn phòng"))
                .andExpect(jsonPath("$.data.status").value("AVAILABLE"));

        verify(housekeepingService).completeCleaning(ROOM_ID);
    }

    @Test
    void mapsCompleteCleaningServiceErrorsToContractResponses() throws Exception {
        when(housekeepingService.completeCleaning(ROOM_ID))
                .thenThrow(RoomStateConflictException.cannotCompleteCleaning(RoomStatus.AVAILABLE));

        mockMvc.perform(patch("/api/v1/housekeeping/rooms/{roomId}/complete-cleaning", ROOM_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ROOM_CLEANING_COMPLETE_CONFLICT"));

        UUID missingRoomId = UUID.fromString("10000000-0000-0000-0000-000000000099");
        when(housekeepingService.completeCleaning(missingRoomId))
                .thenThrow(new RoomNotFoundException("Không tìm thấy phòng đang dọn"));

        mockMvc.perform(patch("/api/v1/housekeeping/rooms/{roomId}/complete-cleaning", missingRoomId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"));
    }

    @Test
    void rejectsInvalidRoomIdBeforeCallingCompleteCleaningService() throws Exception {
        mockMvc.perform(patch("/api/v1/housekeeping/rooms/not-a-uuid/complete-cleaning"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(housekeepingService);
    }

    private DirtyRoomResponse dirtyRoom(String note) {
        return new DirtyRoomResponse(
                ROOM_ID,
                "DLX-101",
                1,
                RoomStatus.DIRTY,
                note,
                new DirtyRoomTypeSummaryResponse(ROOM_TYPE_ID, "DLX", "Deluxe hướng biển"));
    }
}
