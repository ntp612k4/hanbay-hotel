package com.tpanh.hotel.booking.controller;

import com.tpanh.hotel.booking.domain.BookingStatus;
import com.tpanh.hotel.booking.dto.request.CancelBookingRequest;
import com.tpanh.hotel.booking.dto.request.CreatePendingBookingRequest;
import com.tpanh.hotel.booking.dto.response.BookingHistoryItemResponse;
import com.tpanh.hotel.booking.dto.response.CancelBookingResponse;
import com.tpanh.hotel.booking.dto.response.CancellationEligibilityResponse;
import com.tpanh.hotel.booking.dto.response.CreatePendingBookingResponse;
import com.tpanh.hotel.booking.service.BookingService;
import com.tpanh.hotel.common.response.ApiErrorResponse;
import com.tpanh.hotel.common.response.ApiResponse;
import com.tpanh.hotel.common.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.prefix}/bookings")
@RequiredArgsConstructor
@Tag(name = "Đặt phòng", description = "API tạo và quản lý đặt phòng")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @Operation(
            summary = "Tạo đặt phòng chờ thanh toán",
            description = "Kiểm tra ngày lưu trú, sức chứa và phòng trống trước khi giữ phòng trong 15 phút",
            security = {})
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "201",
                description = "Tạo đặt phòng chờ thanh toán thành công"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Dữ liệu đặt phòng, ngày lưu trú hoặc sức chứa không hợp lệ",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Không tìm thấy loại phòng hoặc dịch vụ bổ sung",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "Hết phòng hoặc phát sinh xung đột khi tạo đặt phòng",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<CreatePendingBookingResponse>> createPendingBooking(
            @RequestBody @Valid CreatePendingBookingRequest request) {
        CreatePendingBookingResponse response = bookingService.createPendingBooking(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Đặt phòng thành công", response));
    }

    @GetMapping("/my-bookings")
    @Operation(
            summary = "Xem lịch sử đặt phòng",
            description = "Lấy lịch sử đặt phòng của tài khoản đang đăng nhập, có hỗ trợ phân trang và lọc trạng thái")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Lấy lịch sử đặt phòng thành công"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Tham số phân trang hoặc trạng thái không hợp lệ",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Người dùng chưa đăng nhập hoặc mã truy cập không hợp lệ",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ApiResponse<PageResponse<BookingHistoryItemResponse>> getCurrentUserBookingHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) BookingStatus status) {
        return ApiResponse.success(
                "Lấy lịch sử đặt phòng thành công", bookingService.getCurrentUserBookingHistory(page, size, status));
    }

    @GetMapping("/{bookingCode}")
    @Operation(summary = "Xem chi tiết đặt phòng", description = "Chỉ trả booking của tài khoản đang đăng nhập")
    public ApiResponse<BookingHistoryItemResponse> getCurrentUserBooking(@PathVariable String bookingCode) {
        return ApiResponse.success(
                "Lấy chi tiết đặt phòng thành công", bookingService.getCurrentUserBooking(bookingCode));
    }

    @PatchMapping("/{bookingId}")
    @Operation(
            summary = "Cập nhật đặt phòng đang chờ thanh toán",
            description = "Tính lại sức chứa, phòng trống và toàn bộ giá đã lưu tại thời điểm đặt")
    public ApiResponse<CreatePendingBookingResponse> updatePendingBooking(
            @PathVariable UUID bookingId,
            @RequestHeader(value = "X-Booking-Access-Token", required = false) String bookingAccessToken,
            @RequestBody @Valid CreatePendingBookingRequest request) {
        return ApiResponse.success(
                "Cập nhật đặt phòng thành công",
                bookingService.updatePendingBooking(bookingId, request, bookingAccessToken));
    }

    @GetMapping("/{bookingCode}/cancellation")
    @Operation(
            summary = "Kiểm tra chính sách hủy đặt phòng",
            description =
                    "Kiểm tra khả năng hủy và số tiền dự kiến được hoàn của đặt phòng thuộc tài khoản đang đăng nhập")
    public ApiResponse<CancellationEligibilityResponse> getCancellationEligibility(@PathVariable String bookingCode) {
        return ApiResponse.success(
                "Kiểm tra chính sách hủy thành công", bookingService.getCancellationEligibility(bookingCode));
    }

    @PostMapping("/{bookingId}/cancel")
    @Operation(
            summary = "Hủy đặt phòng",
            description = "Hủy đặt phòng thuộc tài khoản đang đăng nhập khi trạng thái và chính sách hủy cho phép")
    public ApiResponse<CancelBookingResponse> cancelBooking(
            @PathVariable UUID bookingId, @RequestBody @Valid CancelBookingRequest request) {
        return ApiResponse.success("Hủy đặt phòng thành công", bookingService.cancelBooking(bookingId, request));
    }
}
