package com.tpanh.hotel.common.response;

import java.time.Instant;
import java.util.Map;

public record ApiErrorResponse(
        String code, String message, String path, Instant timestamp, Map<String, String> errors) {}
