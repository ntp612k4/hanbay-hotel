package com.tpanh.hotel.ai.controller;

import com.tpanh.hotel.ai.config.PolicyRagProperties;
import com.tpanh.hotel.ai.dto.AiChatRequest;
import com.tpanh.hotel.ai.dto.AiChatStreamEvent;
import com.tpanh.hotel.ai.service.HotelAiAssistantService;
import com.tpanh.hotel.authentication.service.AuthenticationRateLimitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("${api.prefix}/ai")
@RequiredArgsConstructor
@Tag(name = "AI Assistant", description = "Trợ lý khách sạn sử dụng function calling")
public class AiAssistantController {

    private final HotelAiAssistantService assistantService;
    private final AuthenticationRateLimitService rateLimitService;
    private final PolicyRagProperties policyRagProperties;

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "Chat streaming với trợ lý AI",
            description = "Trả từng phần câu trả lời qua Server-Sent Events sau khi các function cần thiết được gọi")
    public Flux<ServerSentEvent<AiChatStreamEvent>> stream(
            @RequestBody @Valid AiChatRequest request, HttpServletRequest servletRequest) {
        String principal = servletRequest.getUserPrincipal() == null
                ? servletRequest.getRemoteAddr()
                : servletRequest.getUserPrincipal().getName();
        rateLimitService.check(
                "ai-chat-stream", principal, policyRagProperties.rateLimitPerMinute(), Duration.ofMinutes(1));
        return Flux.defer(() -> assistantService.stream(request.conversationId(), request.message()))
                .map(payload ->
                        ServerSentEvent.builder(payload).event(payload.type()).build())
                .onErrorResume(exception -> Flux.just(ServerSentEvent.builder(AiChatStreamEvent.error())
                        .event("error")
                        .build()));
    }
}
