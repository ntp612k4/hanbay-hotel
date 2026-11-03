package com.tpanh.hotel.pricing.dto.result;

import com.tpanh.hotel.booking.domain.BookingNight;
import java.util.List;

public record RoomPricingResult(List<BookingNight> nights, long roomAmount, long roomDiscount, long finalRoomAmount) {}
