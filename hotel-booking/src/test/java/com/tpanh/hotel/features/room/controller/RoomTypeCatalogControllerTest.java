package com.tpanh.hotel.features.room.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tpanh.hotel.common.exception.GlobalExceptionHandler;
import com.tpanh.hotel.common.exception.RequestValidationException;
import com.tpanh.hotel.common.response.PageResponse;
import com.tpanh.hotel.room.controller.RoomTypeController;
import com.tpanh.hotel.room.dto.RoomTypeCatalogItemResponse;
import com.tpanh.hotel.room.dto.response.AvailableRoomTypeResponse;
import com.tpanh.hotel.room.dto.response.NightlyPriceResponse;
import com.tpanh.hotel.room.service.AvailableRoomTypeSearchService;
import com.tpanh.hotel.room.service.RoomTypeService;
import java.math.BigDecimal;
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
class RoomTypeCatalogControllerTest {

    @Mock
    private RoomTypeService roomTypeService;

    @Mock
    private AvailableRoomTypeSearchService availableRoomTypeSearchService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        RoomTypeController controller = new RoomTypeController(roomTypeService, availableRoomTypeSearchService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getCatalogReturnsSuccessResponse() throws Exception {
        UUID id = UUID.randomUUID();
        RoomTypeCatalogItemResponse item = new RoomTypeCatalogItemResponse(
                id, "Deluxe Room", "Spacious room", 2, 1, new BigDecimal("1500000"), null);
        PageResponse<RoomTypeCatalogItemResponse> catalog = new PageResponse<>(List.of(item), 0, 6, 1, 1, true, true);
        when(roomTypeService.getCatalog(0, 6, "deluxe", "name", "asc")).thenReturn(catalog);

        mockMvc.perform(get("/api/v1/room-types").param("keyword", "deluxe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Danh mục loại phòng đã được lấy thành công."))
                .andExpect(jsonPath("$.data.content[0].id").value(id.toString()))
                .andExpect(jsonPath("$.data.content[0].name").value("Deluxe Room"))
                .andExpect(jsonPath("$.data.content[0].capacityAdults").value(2))
                .andExpect(jsonPath("$.data.content[0].capacityChildren").value(1))
                .andExpect(jsonPath("$.data.content[0].thumbnailUrl").isEmpty())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        verify(roomTypeService).getCatalog(0, 6, "deluxe", "name", "asc");
    }

    @Test
    void getCatalogReturnsValidationErrorFromService() throws Exception {
        when(roomTypeService.getCatalog(-1, 6, null, "name", "asc"))
                .thenThrow(new RequestValidationException("page", "Chỉ số trang phải lớn hơn hoặc bằng 0"));

        mockMvc.perform(get("/api/v1/room-types").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Dữ liệu gửi lên không hợp lệ"))
                .andExpect(jsonPath("$.path").value("/api/v1/room-types"))
                .andExpect(jsonPath("$.errors.page").value("Chỉ số trang phải lớn hơn hoặc bằng 0"));
    }

    @Test
    void getCatalogReturnsValidationErrorForNonNumericPage() throws Exception {
        mockMvc.perform(get("/api/v1/room-types").param("page", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.page").value("page phải có kiểu dữ liệu hợp lệ: int"));
    }

    @Test
    void searchAvailableRoomTypesBindsPagination() throws Exception {
        UUID roomTypeId = UUID.randomUUID();
        LocalDate checkIn = LocalDate.of(2026, 8, 12);
        LocalDate checkOut = LocalDate.of(2026, 8, 15);
        AvailableRoomTypeResponse item = new AvailableRoomTypeResponse(
                roomTypeId,
                "DELUXE",
                "Deluxe Room",
                "Ocean view",
                2_450_000,
                4,
                2,
                2,
                "1 King bed",
                new BigDecimal("42.50"),
                "https://example.com/deluxe.jpg",
                3,
                3,
                7_350_000,
                0,
                7_350_000,
                List.of(
                        new NightlyPriceResponse(LocalDate.of(2026, 8, 12), 2_450_000, List.of()),
                        new NightlyPriceResponse(LocalDate.of(2026, 8, 13), 2_450_000, List.of()),
                        new NightlyPriceResponse(LocalDate.of(2026, 8, 14), 2_450_000, List.of())));
        PageResponse<AvailableRoomTypeResponse> page = new PageResponse<>(List.of(item), 0, 1, 1, 1, true, true);
        when(availableRoomTypeSearchService.search(checkIn, checkOut, 2, 1, roomTypeId, 0, 1))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/room-types/available")
                        .param("checkIn", "2026-08-12")
                        .param("checkOut", "2026-08-15")
                        .param("adults", "2")
                        .param("children", "1")
                        .param("roomTypeId", roomTypeId.toString())
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(roomTypeId.toString()))
                .andExpect(jsonPath("$.data.content[0].code").value("DELUXE"))
                .andExpect(jsonPath("$.data.content[0].basePrice").value(2450000))
                .andExpect(jsonPath("$.data.content[0].availableRooms").value(3))
                .andExpect(jsonPath("$.data.content[0].totalNights").value(3))
                .andExpect(jsonPath("$.data.content[0].roomSubtotal").value(7350000))
                .andExpect(jsonPath("$.data.content[0].roomDiscount").value(0))
                .andExpect(jsonPath("$.data.content[0].roomTotal").value(7350000))
                .andExpect(
                        jsonPath("$.data.content[0].nightlyPrices[0].stayDate").value("2026-08-12"))
                .andExpect(jsonPath("$.data.content[0].nightlyPrices[0].price").value(2450000))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1));

        verify(availableRoomTypeSearchService).search(checkIn, checkOut, 2, 1, roomTypeId, 0, 1);
    }
}
