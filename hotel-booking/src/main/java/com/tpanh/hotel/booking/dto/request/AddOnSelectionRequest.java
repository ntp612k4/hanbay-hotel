package com.tpanh.hotel.booking.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AddOnSelectionRequest(
        @NotNull(message = "Dịch vụ bổ sung không được để trống") UUID addOnServiceId,
        @Min(value = 1, message = "Số lượng dịch vụ phải lớn hơn hoặc bằng 1") @Max(value = 20, message = "Số lượng mỗi dịch vụ không được vượt quá 20") int quantity) {}
