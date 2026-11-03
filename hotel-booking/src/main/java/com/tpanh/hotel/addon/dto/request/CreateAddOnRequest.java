package com.tpanh.hotel.addon.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.tpanh.hotel.addon.service.model.CreateAddOnCommand;
import com.tpanh.hotel.common.exception.RequestValidationException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;

@Getter
public class CreateAddOnRequest {

    @NotBlank(message = "Mã dịch vụ không được để trống") @Size(min = 2, max = 50, message = "Mã dịch vụ phải có từ 2 đến 50 ký tự") @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "Mã dịch vụ chỉ gồm chữ in hoa, số hoặc dấu gạch dưới") private String code;

    @NotBlank(message = "Tên dịch vụ không được để trống") @Size(min = 2, max = 150, message = "Tên dịch vụ phải có từ 2 đến 150 ký tự") private String name;

    @Size(max = 2000, message = "Mô tả dịch vụ không được vượt quá 2000 ký tự") private String description;

    @NotNull(message = "Giá dịch vụ không được để trống") @PositiveOrZero(message = "Giá dịch vụ phải lớn hơn hoặc bằng 0") private Long price;

    private final Set<String> unsupportedFields = new LinkedHashSet<>();

    public CreateAddOnRequest() {}

    public CreateAddOnRequest(String code, String name, String description, Long price) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.price = price;
    }

    @JsonAnySetter
    public void rejectUnsupportedField(String fieldName, Object ignoredValue) {
        unsupportedFields.add(fieldName);
    }

    public CreateAddOnCommand toCommand() {
        rejectUnsupportedFields();
        return new CreateAddOnCommand(code, name, description, price);
    }

    private void rejectUnsupportedFields() {
        if (!unsupportedFields.isEmpty()) {
            String field = unsupportedFields.iterator().next();
            throw new RequestValidationException(field, "Trường dữ liệu không được hỗ trợ");
        }
    }
}
