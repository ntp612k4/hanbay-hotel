package com.tpanh.hotel.room.controller;

import com.tpanh.hotel.common.response.ApiErrorResponse;
import com.tpanh.hotel.common.response.ApiResponse;
import com.tpanh.hotel.common.response.PageResponse;
import com.tpanh.hotel.room.dto.RoomTypeCatalogItemResponse;
import com.tpanh.hotel.room.dto.RoomTypeDetailResponse;
import com.tpanh.hotel.room.dto.response.AvailableRoomTypeResponse;
import com.tpanh.hotel.room.service.AvailableRoomTypeSearchService;
import com.tpanh.hotel.room.service.RoomTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.prefix:/api/v1}/room-types")
@RequiredArgsConstructor
@Tag(name = "Loại phòng", description = "Thông tin công khai về các loại phòng đang kinh doanh")
@SecurityRequirements
public class RoomTypeController {

    private final RoomTypeService roomTypeService;
    private final AvailableRoomTypeSearchService availableRoomTypeSearchService;

    /*
     * HAN-22
     * Xem chi tiết loại phòng theo ID.
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Xem chi tiết loại phòng",
            description = "Trả về thông tin chi tiết, hình ảnh và tiện nghi của loại phòng đang kinh doanh",
            security = {})
    public ApiResponse<RoomTypeDetailResponse> getDetail(@PathVariable UUID id) {
        return ApiResponse.success("Lấy chi tiết loại phòng thành công", roomTypeService.getDetail(id));
    }

    /*
     * Danh mục loại phòng.
     */
    @GetMapping
    @Operation(
            summary = "Xem danh mục loại phòng",
            description = "Trả về danh sách loại phòng đang hoạt động, " + "hỗ trợ tìm kiếm, sắp xếp và phân trang",
            security = {})
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Lấy danh mục loại phòng thành công"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Tham số phân trang hoặc sắp xếp không hợp lệ",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ApiResponse<PageResponse<RoomTypeCatalogItemResponse>> getCatalog(
            @Parameter(description = "Chỉ số trang, bắt đầu từ 0", example = "0") @RequestParam(defaultValue = "0")
                    int page,
            @Parameter(description = "Số phần tử mỗi trang, từ 1 đến 100", example = "6")
                    @RequestParam(defaultValue = "6")
                    int size,
            @Parameter(description = "Từ khóa tìm kiếm theo tên loại phòng", example = "Deluxe")
                    @RequestParam(required = false)
                    String keyword,
            @Parameter(description = "Trường sắp xếp: name hoặc basePrice", example = "name")
                    @RequestParam(defaultValue = "name")
                    String sortBy,
            @Parameter(description = "Chiều sắp xếp: asc hoặc desc", example = "asc")
                    @RequestParam(defaultValue = "asc")
                    String direction) {

        return ApiResponse.success(
                "Danh mục loại phòng đã được lấy thành công.",
                roomTypeService.getCatalog(page, size, keyword, sortBy, direction));
    }

    /*
     * Tìm loại phòng còn trống.
     */
    @GetMapping("/available")
    @Operation(
            summary = "Tìm loại phòng còn trống",
            description =
                    "Trả về các loại phòng đang hoạt động, " + "đủ sức chứa và còn tồn kho trong khoảng ngày đã chọn",
            security = {})
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Tìm loại phòng còn trống thành công"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Tiêu chí tìm kiếm không hợp lệ",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<PageResponse<AvailableRoomTypeResponse>>> searchAvailableRoomTypes(
            @Parameter(description = "Ngày nhận phòng", example = "2026-08-12")
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate checkIn,
            @Parameter(description = "Ngày trả phòng", example = "2026-08-15")
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate checkOut,
            @Parameter(description = "Số người lớn", example = "2") @RequestParam int adults,
            @Parameter(description = "Số trẻ em", example = "0") @RequestParam(defaultValue = "0") int children,
            @Parameter(
                            description = "Giới hạn kết quả theo một loại phòng",
                            example = "10000000-0000-0000-0000-000000000001")
                    @RequestParam(required = false)
                    UUID roomTypeId,
            @Parameter(description = "Chỉ số trang, bắt đầu từ 0", example = "0") @RequestParam(defaultValue = "0")
                    int page,
            @Parameter(description = "Số loại phòng mỗi trang, từ 1 đến 100", example = "6")
                    @RequestParam(defaultValue = "6")
                    int size) {

        return ResponseEntity.ok(ApiResponse.success(
                availableRoomTypeSearchService.search(checkIn, checkOut, adults, children, roomTypeId, page, size)));
    }
}
