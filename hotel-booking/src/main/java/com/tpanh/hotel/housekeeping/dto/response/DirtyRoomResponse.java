package com.tpanh.hotel.housekeeping.dto.response;

import com.tpanh.hotel.room.domain.RoomStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Phòng vật lý đang cần được dọn")
public record DirtyRoomResponse(
        @Schema(format = "uuid") UUID roomId,
        @Schema(example = "DLX-101") String roomNumber,
        @Schema(example = "1") int floor,
        @Schema(allowableValues = {"DIRTY", "CLEANING", "AVAILABLE"}) RoomStatus status,
        @Schema(nullable = true, description = "Ghi chú vận hành hiện tại của phòng") String note,
        DirtyRoomTypeSummaryResponse roomType) {}
