package com.tpanh.hotel.pricing.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.tpanh.hotel.common.exception.RequestValidationException;
import com.tpanh.hotel.pricing.service.model.CreateHolidayCalendarCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;

@Getter
public class CreateHolidayCalendarRequest {

    @NotBlank(message = "Tên ngày lễ không được để trống") @Size(min = 2, max = 150, message = "Tên ngày lễ phải có từ 2 đến 150 ký tự") private String name;

    @NotNull(message = "Ngày bắt đầu không được để trống") private LocalDate startDate;

    @NotNull(message = "Ngày kết thúc không được để trống") private LocalDate endDate;

    private final Set<String> unsupportedFields = new LinkedHashSet<>();

    public CreateHolidayCalendarRequest() {}

    public CreateHolidayCalendarRequest(String name, LocalDate startDate, LocalDate endDate) {
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    @JsonAnySetter
    public void rejectUnsupportedField(String fieldName, Object ignoredValue) {
        unsupportedFields.add(fieldName);
    }

    public CreateHolidayCalendarCommand toCommand() {
        rejectUnsupportedFields();
        return new CreateHolidayCalendarCommand(name, startDate, endDate);
    }

    private void rejectUnsupportedFields() {
        if (!unsupportedFields.isEmpty()) {
            String field = unsupportedFields.iterator().next();
            throw new RequestValidationException(field, "Trường dữ liệu không được hỗ trợ");
        }
    }
}
