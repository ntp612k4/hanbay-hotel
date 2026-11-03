package com.tpanh.hotel.booking.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreatePendingBookingRequest(
        @NotNull(message = "Loại phòng không được để trống") UUID roomTypeId,
        @NotNull(message = "Ngày vào phòng không được để trống") LocalDate checkInDate,
        @NotNull(message = "Ngày rời phòng không được để trống") LocalDate checkOutDate,
        @Min(value = 1, message = "Yêu cầu tối thiểu phải có 1 người lớn") int adults,
        @Min(value = 0, message = "Số trẻ em không được âm") int children,
        @NotNull(message = "Thông tin khách hàng không được để trống") @Valid GuestInfo guestInfo,
        @Valid List<AddOnSelectionRequest> addOns) {}
