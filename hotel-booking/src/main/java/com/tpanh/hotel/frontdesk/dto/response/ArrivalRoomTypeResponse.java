package com.tpanh.hotel.frontdesk.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Hạng phòng đã đặt")
public record ArrivalRoomTypeResponse(
        @Schema(format = "uuid") UUID id,
        @Schema(example = "DLX") String code,
        @Schema(example = "Deluxe") String name) {}
