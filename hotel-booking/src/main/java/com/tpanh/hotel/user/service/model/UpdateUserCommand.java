package com.tpanh.hotel.user.service.model;

public record UpdateUserCommand(PatchField<String> fullName, PatchField<String> phone) {

    public UpdateUserCommand {
        fullName = fullName == null ? PatchField.absent() : fullName;
        phone = phone == null ? PatchField.absent() : phone;
    }
}
