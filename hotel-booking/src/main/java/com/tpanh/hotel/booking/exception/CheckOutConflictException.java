package com.tpanh.hotel.booking.exception;

import com.tpanh.hotel.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class CheckOutConflictException extends DomainException {

    private CheckOutConflictException(String errorCode, String message) {
        super(errorCode, message, HttpStatus.CONFLICT);
    }

    public static CheckOutConflictException bookingNotCheckedIn() {
        return new CheckOutConflictException(
                "BOOKING_NOT_CHECKED_IN", "Chỉ đặt phòng đang lưu trú mới có thể trả phòng");
    }

    public static CheckOutConflictException bookingAlreadyCheckedOut() {
        return new CheckOutConflictException("BOOKING_ALREADY_CHECKED_OUT", "Đặt phòng đã được trả phòng trước đó");
    }

    public static CheckOutConflictException roomNotAssigned() {
        return new CheckOutConflictException("ROOM_NOT_ASSIGNED", "Đặt phòng chưa được gán phòng vật lý");
    }

    public static CheckOutConflictException roomNotOccupied() {
        return new CheckOutConflictException("ROOM_NOT_OCCUPIED", "Phòng đã gán không đang ở trạng thái OCCUPIED");
    }

    public static CheckOutConflictException overCollection() {
        return new CheckOutConflictException(
                "CHECKOUT_OVER_COLLECTION",
                "Số tiền thu thêm vượt số tiền còn phải thu. Hãy tải lại dữ liệu thanh toán và kiểm tra số tiền thực nhận");
    }

    public static CheckOutConflictException earlyCheckoutRequired() {
        return new CheckOutConflictException(
                "EARLY_CHECKOUT_REQUIRED", "Khách trả phòng trước ngày dự kiến. Vui lòng sử dụng luồng trả phòng sớm");
    }

    public static CheckOutConflictException paymentRequired(long outstandingAmount) {
        return new CheckOutConflictException(
                "PAYMENT_REQUIRED", "Đặt phòng còn công nợ " + outstandingAmount + " VND nên chưa thể trả phòng");
    }
}
