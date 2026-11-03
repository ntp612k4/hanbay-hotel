package com.tpanh.hotel.booking.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CheckInGuestRequest(@NotNull(message = "Room ID không được để trống") UUID roomId) {}
