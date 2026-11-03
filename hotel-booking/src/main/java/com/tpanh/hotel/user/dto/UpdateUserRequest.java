package com.tpanh.hotel.user.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.tpanh.hotel.user.exception.ProfileValidationException;
import com.tpanh.hotel.user.service.model.PatchField;
import com.tpanh.hotel.user.service.model.UpdateUserCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.LinkedHashMap;
import java.util.Map;

@Schema(description = "Thông tin cá nhân cần cập nhật")
public class UpdateUserRequest {

    private String fullName;
    private String phone;
    private boolean fullNamePresent;
    private boolean phonePresent;
    private final Map<String, String> unsupportedFields = new LinkedHashMap<>();

    @JsonSetter("fullName")
    public void setFullName(String fullName) {
        this.fullName = fullName;
        this.fullNamePresent = true;
    }

    @JsonSetter("phone")
    public void setPhone(String phone) {
        this.phone = phone;
        this.phonePresent = true;
    }

    @JsonAnySetter
    public void rejectUnsupportedField(String fieldName, Object ignoredValue) {
        unsupportedFields.put(fieldName, "Trường dữ liệu không được hỗ trợ");
    }

    public UpdateUserCommand toCommand() {
        if (!unsupportedFields.isEmpty()) {
            throw new ProfileValidationException(unsupportedFields);
        }

        return new UpdateUserCommand(
                fullNamePresent ? PatchField.of(fullName) : PatchField.absent(),
                phonePresent ? PatchField.of(phone) : PatchField.absent());
    }
}
