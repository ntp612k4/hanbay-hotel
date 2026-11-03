package com.tpanh.hotel.payment.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SePayTransferType {
    IN("in"),
    OUT("out");

    private final String value;

    SePayTransferType(String value) {
        this.value = value;
    }

    @JsonCreator
    public static SePayTransferType fromValue(String value) {
        for (SePayTransferType transferType : values()) {
            if (transferType.value.equals(value)) {
                return transferType;
            }
        }
        throw new IllegalArgumentException("Loại giao dịch SePay không hợp lệ: " + value);
    }

    @JsonValue
    public String value() {
        return value;
    }
}
