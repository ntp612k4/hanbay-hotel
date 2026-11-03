package com.tpanh.hotel.room.service;

import java.time.LocalDate;
import java.util.UUID;

public interface AvailabilityService {
    void ensureAvailable(UUID roomTypeId, LocalDate checkIn, LocalDate checkOut);
}
