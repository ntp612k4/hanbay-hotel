package com.tpanh.hotel.ai.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tpanh.hotel.ai.config.PolicyRagProperties;
import com.tpanh.hotel.ai.dto.AiChatRequest;
import com.tpanh.hotel.ai.dto.AiChatStreamEvent;
import com.tpanh.hotel.ai.service.HotelAiAssistantService;
import com.tpanh.hotel.authentication.service.AuthenticationRateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

@ExtendWith(MockitoExtension.class)
class AiAssistantControllerTest {

    private static final UUID CONVERSATION_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");

    @Mock
    private HotelAiAssistantService assistantService;

    @Mock
    private AuthenticationRateLimitService rateLimitService;

    @Mock
    private HttpServletRequest servletRequest;

    private AiAssistantController controller;

    @BeforeEach
    void setUp() {
        controller = new AiAssistantController(
                assistantService, rateLimitService, new PolicyRagProperties(false, 3, 0.18, 10, 1536));
    }

    @Test
    void streamsDeltasAndTerminatesWithDoneEvent() {
        when(assistantService.stream(CONVERSATION_ID, "Tìm phòng"))
                .thenReturn(Flux.just(
                        AiChatStreamEvent.delta("Có "), AiChatStreamEvent.delta("2 phòng"), AiChatStreamEvent.done()));

        List<ServerSentEvent<AiChatStreamEvent>> events = controller.stream(
                        new AiChatRequest(CONVERSATION_ID, "Tìm phòng"), servletRequest)
                .collectList()
                .block();

        assertThat(events).isNotNull().hasSize(3);
        assertThat(events).extracting(ServerSentEvent::event).containsExactly("delta", "delta", "done");
        assertThat(events.get(0).data().content()).isEqualTo("Có ");
        assertThat(events.get(1).data().content()).isEqualTo("2 phòng");
        verify(rateLimitService).check("ai-chat-stream", null, 10, Duration.ofMinutes(1));
    }

    @Test
    void convertsLateStreamFailureToSafeErrorEvent() {
        when(assistantService.stream(CONVERSATION_ID, "Test"))
                .thenReturn(Flux.error(new IllegalStateException("secret provider error")));

        List<ServerSentEvent<AiChatStreamEvent>> events = controller.stream(
                        new AiChatRequest(CONVERSATION_ID, "Test"), servletRequest)
                .collectList()
                .block();

        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.event()).isEqualTo("error");
            assertThat(event.data().content()).doesNotContain("secret provider error");
        });
    }

    @Test
    void convertsSynchronousProviderFailureToSafeErrorEvent() {
        when(assistantService.stream(CONVERSATION_ID, "Test"))
                .thenThrow(new IllegalStateException("secret provider error"));

        List<ServerSentEvent<AiChatStreamEvent>> events = controller.stream(
                        new AiChatRequest(CONVERSATION_ID, "Test"), servletRequest)
                .collectList()
                .block();

        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.event()).isEqualTo("error");
            assertThat(event.data().content()).doesNotContain("secret provider error");
        });
    }
}
