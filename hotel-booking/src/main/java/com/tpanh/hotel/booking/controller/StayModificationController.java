package com.tpanh.hotel.booking.controller;

import com.tpanh.hotel.booking.dto.request.EarlyCheckoutRequest;
import com.tpanh.hotel.booking.dto.request.ExtendStayRequest;
import com.tpanh.hotel.booking.dto.response.EarlyCheckoutResponse;
import com.tpanh.hotel.booking.dto.response.ExtendStayResponse;
import com.tpanh.hotel.booking.service.StayModificationService;
import com.tpanh.hotel.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.prefix}/bookings")
@RequiredArgsConstructor
public class StayModificationController {
    private final StayModificationService service;

    @GetMapping("/{bookingId}/early-checkout")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'MANAGER', 'ADMIN')")
    public ApiResponse<EarlyCheckoutResponse> quoteEarlyCheckout(
            @PathVariable UUID bookingId, @RequestParam LocalDate actualCheckOutDate) {
        return ApiResponse.success(
                "Tính trả phòng sớm thành công", service.quoteEarlyCheckout(bookingId, actualCheckOutDate));
    }

    @PostMapping("/{bookingId}/early-checkout")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'MANAGER', 'ADMIN')")
    public ApiResponse<EarlyCheckoutResponse> earlyCheckout(
            @PathVariable UUID bookingId, @RequestBody @Valid EarlyCheckoutRequest request) {
        return ApiResponse.success("Trả phòng sớm thành công", service.earlyCheckout(bookingId, request));
    }

    @PostMapping("/{bookingId}/extend")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ExtendStayResponse> extendStay(
            @PathVariable UUID bookingId, @RequestBody @Valid ExtendStayRequest request) {
        return ApiResponse.success("Gia hạn lưu trú thành công", service.extendStay(bookingId, request));
    }
}
