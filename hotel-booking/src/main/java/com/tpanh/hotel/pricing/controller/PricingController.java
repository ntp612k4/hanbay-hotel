package com.tpanh.hotel.pricing.controller;

import com.tpanh.hotel.common.response.ApiErrorResponse;
import com.tpanh.hotel.common.response.ApiResponse;
import com.tpanh.hotel.pricing.dto.request.CalculateStayPriceRequest;
import com.tpanh.hotel.pricing.dto.response.CalculateStayPriceResponse;
import com.tpanh.hotel.pricing.mapper.PricingMapper;
import com.tpanh.hotel.pricing.service.PricingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.prefix:/api/v1}/pricing")
@RequiredArgsConstructor
@Tag(name = "Định giá", description = "Tính giá lưu trú theo hạng phòng và khoảng ngày")
public class PricingController {

    private final PricingService pricingService;
    private final PricingMapper pricingMapper;

    @PostMapping("/stay-price-calculations")
    @Operation(
            summary = "Tính giá lưu trú theo từng đêm",
            description = "Tính giá cho từng ngày trong khoảng [checkInDate, checkOutDate) và trả về tổng chính xác",
            security = {})
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Tính giá lưu trú thành công"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Request, khoảng ngày hoặc cấu hình giá không hợp lệ",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Không tìm thấy hạng phòng đang kinh doanh",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "Lỗi hệ thống không mong muốn",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ApiResponse<CalculateStayPriceResponse> calculateStayPrice(
            @RequestBody @Valid CalculateStayPriceRequest request) {
        return ApiResponse.success(
                "Tính giá lưu trú thành công",
                pricingMapper.toResponse(pricingService.calculateStayPrice(
                        request.roomTypeId(), request.checkInDate(), request.checkOutDate())));
    }
}
