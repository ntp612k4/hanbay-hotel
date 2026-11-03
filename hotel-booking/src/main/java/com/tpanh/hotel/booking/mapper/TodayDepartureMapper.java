package com.tpanh.hotel.booking.mapper;

import com.tpanh.hotel.booking.domain.Booking;
import com.tpanh.hotel.booking.domain.BookingGuest;
import com.tpanh.hotel.booking.dto.response.TodayDepartureResponse;
import com.tpanh.hotel.common.mapper.MapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(config = MapperConfig.class)
public interface TodayDepartureMapper {

    @Mapping(target = "bookingId", source = "booking.id")
    @Mapping(target = "bookingCode", source = "booking.bookingCode")
    @Mapping(target = "guestName", expression = "java(guestName(primaryGuest))")
    @Mapping(target = "guestPhone", source = "primaryGuest.phone")
    @Mapping(target = "roomTypeName", source = "booking.roomType.name")
    @Mapping(target = "checkInDate", source = "booking.checkInDate")
    @Mapping(target = "checkOutDate", source = "booking.checkOutDate")
    @Mapping(target = "adults", source = "booking.adultCount")
    @Mapping(target = "children", source = "booking.childCount")
    @Mapping(target = "status", source = "booking.status")
    @Mapping(target = "totalAmount", source = "booking.totalAmount")
    @Mapping(target = "paidAmount", source = "booking.paidAmount")
    @Mapping(target = "outstandingAmount", source = "booking", qualifiedByName = "outstandingAmount")
    TodayDepartureResponse toResponse(Booking booking, BookingGuest primaryGuest);

    @Named("guestName")
    default String guestName(BookingGuest primaryGuest) {
        return primaryGuest == null ? "Chưa cập nhật" : primaryGuest.getFullName();
    }

    @Named("outstandingAmount")
    default long outstandingAmount(Booking booking) {
        return Math.max(booking.getTotalAmount() - booking.getPaidAmount(), 0L);
    }
}
