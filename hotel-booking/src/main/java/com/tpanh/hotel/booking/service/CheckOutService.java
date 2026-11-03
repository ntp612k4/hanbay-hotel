package com.tpanh.hotel.booking.service;

import com.tpanh.hotel.booking.dto.request.CheckOutGuestRequest;
import com.tpanh.hotel.booking.dto.response.CheckOutGuestResponse;
import java.util.UUID;

public interface CheckOutService {

    CheckOutGuestResponse checkOut(UUID bookingId, CheckOutGuestRequest request);
}
