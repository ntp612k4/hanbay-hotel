package com.tpanh.hotel.booking.service;

import com.tpanh.hotel.booking.dto.request.EarlyCheckoutRequest;
import com.tpanh.hotel.booking.dto.request.ExtendStayRequest;
import com.tpanh.hotel.booking.dto.response.EarlyCheckoutResponse;
import com.tpanh.hotel.booking.dto.response.ExtendStayResponse;
import java.time.LocalDate;
import java.util.UUID;

public interface StayModificationService {
    EarlyCheckoutResponse quoteEarlyCheckout(UUID bookingId, LocalDate actualCheckOutDate);

    EarlyCheckoutResponse earlyCheckout(UUID bookingId, EarlyCheckoutRequest request);

    ExtendStayResponse extendStay(UUID bookingId, ExtendStayRequest request);
}
