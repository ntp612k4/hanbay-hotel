package com.tpanh.hotel.booking.mapper;

import com.tpanh.hotel.booking.domain.Booking;
import com.tpanh.hotel.booking.dto.response.CreatePendingBookingResponse;
import com.tpanh.hotel.common.mapper.MapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapperConfig.class)
public interface BookingMapper {

    @Mapping(target = "bookingId", source = "id")
    @Mapping(target = "bookingAccessToken", ignore = true)
    CreatePendingBookingResponse toPendingResponse(Booking booking);
}
