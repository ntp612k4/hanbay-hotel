package com.tpanh.hotel.addon.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.tpanh.hotel.addon.service.model.UpdateAddOnStatusCommand;
import com.tpanh.hotel.common.exception.RequestValidationException;
import jakarta.validation.constraints.NotNull;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;

@Getter
public class UpdateAddOnStatusRequest {

    @NotNull(message = "Trạng thái dịch vụ không được để trống") private Boolean active;

    private final Set<String> unsupportedFields = new LinkedHashSet<>();

    public UpdateAddOnStatusRequest() {}

    public UpdateAddOnStatusRequest(Boolean active) {
        this.active = active;
    }

    @JsonAnySetter
    public void rejectUnsupportedField(String fieldName, Object ignoredValue) {
        unsupportedFields.add(fieldName);
    }

    public UpdateAddOnStatusCommand toCommand() {
        rejectUnsupportedFields();
        return new UpdateAddOnStatusCommand(active);
    }

    private void rejectUnsupportedFields() {
        if (!unsupportedFields.isEmpty()) {
            String field = unsupportedFields.iterator().next();
            throw new RequestValidationException(field, "Trường dữ liệu không được hỗ trợ");
        }
    }
}
