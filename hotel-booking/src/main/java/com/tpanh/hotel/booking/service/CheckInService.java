package com.tpanh.hotel.booking.service;

import com.tpanh.hotel.booking.dto.request.CheckInGuestRequest;
import com.tpanh.hotel.booking.dto.response.AvailablePhysicalRoomResponse;
import com.tpanh.hotel.booking.dto.response.CheckInGuestResponse;
import java.util.List;

public interface CheckInService {

    CheckInGuestResponse checkIn(String bookingCode, CheckInGuestRequest request);

    List<AvailablePhysicalRoomResponse> getAvailableRooms(String bookingCode);
}
