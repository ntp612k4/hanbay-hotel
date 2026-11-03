package com.tpanh.hotel.pricing.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.tpanh.hotel.common.exception.RequestValidationException;
import com.tpanh.hotel.pricing.service.model.UpdatePriceRuleStatusCommand;
import jakarta.validation.constraints.NotNull;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;

@Getter
public class UpdatePriceRuleStatusRequest {

    @NotNull(message = "Trang thai quy tac gia khong duoc de trong") private Boolean active;

    private final Set<String> unsupportedFields = new LinkedHashSet<>();

    public UpdatePriceRuleStatusRequest() {}

    public UpdatePriceRuleStatusRequest(Boolean active) {
        this.active = active;
    }

    @JsonAnySetter
    public void rejectUnsupportedField(String fieldName, Object ignoredValue) {
        unsupportedFields.add(fieldName);
    }

    public UpdatePriceRuleStatusCommand toCommand() {
        rejectUnsupportedFields();
        return new UpdatePriceRuleStatusCommand(active);
    }

    private void rejectUnsupportedFields() {
        if (!unsupportedFields.isEmpty()) {
            String field = unsupportedFields.iterator().next();
            throw new RequestValidationException(field, "Truong du lieu khong duoc ho tro");
        }
    }
}
