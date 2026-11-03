package com.tpanh.hotel.booking.domain;

public enum BookingStatus {
    PENDING,
    CONFIRMED,
    CANCELLED,
    CHECKED_IN,
    CHECKED_OUT,
    EXPIRED;

    public boolean canCancel() {
        return switch (this) {
            case PENDING, CONFIRMED -> true;
            default -> false;
        };
    }

    public boolean canConfirm() {
        return this == PENDING;
    }

    public boolean canExpire() {
        return this == PENDING;
    }
}
