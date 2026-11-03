package com.tpanh.hotel.housekeeping.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Thông tin tóm tắt của hạng phòng cần dọn")
public record DirtyRoomTypeSummaryResponse(
        @Schema(format = "uuid") UUID id,
        @Schema(example = "DLX") String code,
        @Schema(example = "Deluxe hướng biển") String name) {}
