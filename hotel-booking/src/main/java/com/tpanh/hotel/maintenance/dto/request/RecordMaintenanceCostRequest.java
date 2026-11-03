package com.tpanh.hotel.maintenance.dto.request;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record RecordMaintenanceCostRequest(
        @NotNull @Min(0) @Max(999999999999L) Long amount,
        @NotNull LocalDate expenseDate,
        @NotBlank @Size(max = 2000) String note,
        @NotNull @Min(0) Integer expectedRevision) {}
