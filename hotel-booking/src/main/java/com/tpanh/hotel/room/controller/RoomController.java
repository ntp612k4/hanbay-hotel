package com.tpanh.hotel.room.controller;

import com.tpanh.hotel.common.response.ApiResponse;
import com.tpanh.hotel.common.response.PageResponse;
import com.tpanh.hotel.room.dto.response.PhysicalRoomListItemResponse;
import com.tpanh.hotel.room.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.prefix}/rooms")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')")
@Tag(name = "Physical Rooms", description = "Danh sách phòng vật lý dành cho quản lý vận hành")
public class RoomController {

    private final RoomService roomService;

    @GetMapping
    @Operation(
            summary = "Xem danh sách phòng vật lý",
            description =
                    "Trả về các phòng vật lý và trạng thái hiện tại; backend maintenance quyết định phòng nào được tạo yêu cầu bảo trì.")
    public ApiResponse<PageResponse<PhysicalRoomListItemResponse>> findAll(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success("Lấy danh sách phòng vật lý thành công", roomService.findAll(page, size));
    }
}
