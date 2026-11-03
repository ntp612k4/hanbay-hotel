package com.tpanh.hotel.room.dto.request;

import jakarta.validation.constraints.*;
import java.util.UUID;

public record RoomWriteRequest(
        @NotNull UUID roomTypeId,
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,30}") String roomNumber,
        @Min(-10) @Max(200) int floor,
        @Size(max = 2000) String note) {}
