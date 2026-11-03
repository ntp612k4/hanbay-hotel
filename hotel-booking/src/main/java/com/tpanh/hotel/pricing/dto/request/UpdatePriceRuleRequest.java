package com.tpanh.hotel.pricing.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.tpanh.hotel.common.exception.RequestValidationException;
import com.tpanh.hotel.pricing.domain.AdjustmentDirection;
import com.tpanh.hotel.pricing.domain.AdjustmentType;
import com.tpanh.hotel.pricing.domain.RuleType;
import com.tpanh.hotel.pricing.service.model.UpdatePriceRuleCommand;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;

@Getter
public class UpdatePriceRuleRequest {

    @NotNull(message = "ID hang phong khong duoc de trong") private UUID roomTypeId;

    @NotBlank(message = "Ten quy tac gia khong duoc de trong") @Size(min = 2, max = 150, message = "Ten quy tac gia phai co tu 2 den 150 ky tu") private String name;

    @NotNull(message = "Loai quy tac gia khong duoc de trong") private RuleType ruleType;

    @NotNull(message = "Loai dieu chinh gia khong duoc de trong") private AdjustmentType adjustmentType;

    @NotNull(message = "Gia tri dieu chinh khong duoc de trong") @PositiveOrZero(message = "Gia tri dieu chinh phai lon hon hoac bang 0") private Long adjustmentValue;

    private AdjustmentDirection adjustmentDirection;

    @NotNull(message = "Ngay bat dau khong duoc de trong") private LocalDate startDate;

    @NotNull(message = "Ngay ket thuc khong duoc de trong") private LocalDate endDate;

    @NotNull(message = "Do uu tien khong duoc de trong") @Min(value = 0, message = "Do uu tien phai tu 0 den 1000") @Max(value = 1000, message = "Do uu tien phai tu 0 den 1000") private Integer priority;

    private final Set<String> unsupportedFields = new LinkedHashSet<>();

    public UpdatePriceRuleRequest() {}

    @JsonAnySetter
    public void rejectUnsupportedField(String fieldName, Object ignoredValue) {
        unsupportedFields.add(fieldName);
    }

    public UpdatePriceRuleCommand toCommand() {
        rejectUnsupportedFields();
        return new UpdatePriceRuleCommand(
                roomTypeId,
                name,
                ruleType,
                adjustmentType,
                adjustmentValue,
                adjustmentDirection,
                startDate,
                endDate,
                priority);
    }

    private void rejectUnsupportedFields() {
        if (!unsupportedFields.isEmpty()) {
            String field = unsupportedFields.iterator().next();
            throw new RequestValidationException(field, "Truong du lieu khong duoc ho tro");
        }
    }
}
