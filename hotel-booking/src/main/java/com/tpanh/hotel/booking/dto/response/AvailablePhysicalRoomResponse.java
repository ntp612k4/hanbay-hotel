package com.tpanh.hotel.booking.dto.response;

import java.util.UUID;

public record AvailablePhysicalRoomResponse(UUID roomId, String roomNumber, int floor) {}
