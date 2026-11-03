package com.tpanh.hotel.pricing.service;

import com.tpanh.hotel.booking.domain.Booking;
import com.tpanh.hotel.pricing.dto.result.PricingResult;
import com.tpanh.hotel.pricing.dto.result.RoomPricingResult;
import com.tpanh.hotel.pricing.dto.result.StayPriceResult;
import com.tpanh.hotel.room.domain.RoomType;
import java.time.LocalDate;
import java.util.UUID;

public interface PricingService {
    PricingResult calculatePrice(Booking booking);

    RoomPricingResult calculateRoomPrice(RoomType roomType, LocalDate checkIn, LocalDate checkOut);

    StayPriceResult calculateStayPrice(UUID roomTypeId, LocalDate checkIn, LocalDate checkOut);
}
