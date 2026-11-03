package com.tpanh.hotel.booking.service;

import com.tpanh.hotel.booking.domain.BookingStatus;
import com.tpanh.hotel.booking.dto.request.CancelBookingRequest;
import com.tpanh.hotel.booking.dto.request.CreatePendingBookingRequest;
import com.tpanh.hotel.booking.dto.response.BookingHistoryItemResponse;
import com.tpanh.hotel.booking.dto.response.CancelBookingResponse;
import com.tpanh.hotel.booking.dto.response.CancellationEligibilityResponse;
import com.tpanh.hotel.booking.dto.response.CreatePendingBookingResponse;
import com.tpanh.hotel.common.response.PageResponse;

public interface BookingService {
    CreatePendingBookingResponse createPendingBooking(CreatePendingBookingRequest request);

    CreatePendingBookingResponse updatePendingBooking(
            java.util.UUID bookingId, CreatePendingBookingRequest request, String bookingAccessToken);

    PageResponse<BookingHistoryItemResponse> getCurrentUserBookingHistory(int page, int size, BookingStatus status);

    BookingHistoryItemResponse getCurrentUserBooking(String bookingCode);

    CancellationEligibilityResponse getCancellationEligibility(String bookingCode);

    CancelBookingResponse cancelBooking(java.util.UUID bookingId, CancelBookingRequest request);

    /** Compatibility for internal callers; HTTP command endpoints use bookingId. */
    CancelBookingResponse cancelBooking(String bookingCode, CancelBookingRequest request);
}
