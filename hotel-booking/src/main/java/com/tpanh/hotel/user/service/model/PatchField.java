package com.tpanh.hotel.user.service.model;

public record PatchField<T>(boolean present, T value) {

    public static <T> PatchField<T> absent() {
        return new PatchField<>(false, null);
    }

    public static <T> PatchField<T> of(T value) {
        return new PatchField<>(true, value);
    }
}
