package com.tpanh.hotel.room.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tpanh.hotel.common.exception.GlobalExceptionHandler;
import com.tpanh.hotel.common.response.PageResponse;
import com.tpanh.hotel.room.domain.RoomStatus;
import com.tpanh.hotel.room.dto.response.PhysicalRoomListItemResponse;
import com.tpanh.hotel.room.service.RoomService;
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
class RoomControllerTest {

    @Mock
    private RoomService roomService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new RoomController(roomService))
                .addPlaceholderValue("api.prefix", "/api/v1")
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsPhysicalRoomFieldsAndDefaultPagination() throws Exception {
        UUID roomId = UUID.fromString("10000000-0000-0000-0000-000000000001");
        when(roomService.findAll(0, 20))
                .thenReturn(new PageResponse<>(
                        List.of(new PhysicalRoomListItemResponse(roomId, "DLX-101", 1, RoomStatus.AVAILABLE)),
                        0,
                        20,
                        1,
                        1,
                        true,
                        true));

        mockMvc.perform(get("/api/v1/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].roomId").value(roomId.toString()))
                .andExpect(jsonPath("$.data.content[0].roomNumber").value("DLX-101"))
                .andExpect(jsonPath("$.data.content[0].floor").value(1))
                .andExpect(jsonPath("$.data.content[0].status").value("AVAILABLE"));

        verify(roomService).findAll(0, 20);
    }

    @Test
    void returnsEmptyPage() throws Exception {
        when(roomService.findAll(0, 20)).thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true, true));

        mockMvc.perform(get("/api/v1/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty());
    }
}
