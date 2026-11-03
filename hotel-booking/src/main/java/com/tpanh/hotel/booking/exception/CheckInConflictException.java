package com.tpanh.hotel.booking.exception;

import com.tpanh.hotel.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class CheckInConflictException extends DomainException {

    private CheckInConflictException(String errorCode, String message) {
        super(errorCode, message, HttpStatus.CONFLICT);
    }

    public static CheckInConflictException bookingNotConfirmed() {
        return new CheckInConflictException("BOOKING_NOT_CONFIRMED", "Chỉ booking CONFIRMED mới có thể check-in");
    }

    public static CheckInConflictException bookingAlreadyCheckedIn() {
        return new CheckInConflictException("BOOKING_ALREADY_CHECKED_IN", "Booking đã được check-in trước đó");
    }

    public static CheckInConflictException checkInDateNotAllowed() {
        return new CheckInConflictException(
                "CHECK_IN_DATE_NOT_ALLOWED", "Chỉ có thể check-in trong ngày nhận phòng của booking");
    }

    public static CheckInConflictException roomTypeMismatch() {
        return new CheckInConflictException("ROOM_TYPE_MISMATCH", "Phòng đã chọn không thuộc hạng phòng của booking");
    }

    public static CheckInConflictException roomNotAvailable() {
        return new CheckInConflictException("ROOM_NOT_AVAILABLE", "Phòng đã chọn không còn trống");
    }
}
