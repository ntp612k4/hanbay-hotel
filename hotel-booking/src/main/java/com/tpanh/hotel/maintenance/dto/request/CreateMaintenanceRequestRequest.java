package com.tpanh.hotel.maintenance.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

@Schema(description = "Thông tin tạo yêu cầu bảo trì phòng vật lý")
public record CreateMaintenanceRequestRequest(
        @NotNull(message = "Phòng không được để trống") @Schema(format = "uuid") UUID roomId,
        @NotBlank(message = "Tiêu đề bảo trì không được để trống") @Size(max = 150, message = "Tiêu đề bảo trì không được vượt quá 150 ký tự") String title,
        @Size(max = 2000, message = "Mô tả bảo trì không được vượt quá 2000 ký tự") String description) {}
