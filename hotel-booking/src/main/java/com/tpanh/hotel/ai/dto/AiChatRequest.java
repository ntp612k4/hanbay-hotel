package com.tpanh.hotel.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AiChatRequest(
        @NotNull(message = "conversationId không được để trống") UUID conversationId,
        @NotBlank(message = "Nội dung câu hỏi không được để trống") String message) {}
