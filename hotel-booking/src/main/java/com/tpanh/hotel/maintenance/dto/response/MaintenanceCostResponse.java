package com.tpanh.hotel.maintenance.dto.response;

import java.time.*;
import java.util.UUID;

public record MaintenanceCostResponse(
        long amount,
        LocalDate expenseDate,
        String note,
        int revision,
        UUID recordedBy,
        String recordedByName,
        Instant recordedAt) {}
