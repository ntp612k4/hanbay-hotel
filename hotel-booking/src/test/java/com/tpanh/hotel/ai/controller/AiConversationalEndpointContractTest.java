package com.tpanh.hotel.ai.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tpanh.hotel.ai.config.PolicyRagProperties;
import com.tpanh.hotel.ai.dto.AiChatStreamEvent;
import com.tpanh.hotel.ai.service.HotelAiAssistantService;
import com.tpanh.hotel.authentication.service.AuthenticationRateLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import reactor.core.publisher.Flux;

class AiConversationalEndpointContractTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        HotelAiAssistantService assistantService = mock(HotelAiAssistantService.class);
        when(assistantService.stream(any(), anyString())).thenReturn(Flux.just(AiChatStreamEvent.done()));
        PolicyRagProperties properties = new PolicyRagProperties(false, 3, 0.18, 10, 1536);
        mockMvc = MockMvcBuilders.standaloneSetup(new AiAssistantController(
                        assistantService, mock(AuthenticationRateLimitService.class), properties))
                .addPlaceholderValue("api.prefix", "/api/v1")
                .build();
    }

    @Test
    void unifiedStreamingEndpointExists() throws Exception {
        mockMvc.perform(
                        post("/api/v1/ai/chat/stream")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.TEXT_EVENT_STREAM)
                                .content(
                                        """
                                {"conversationId":"11111111-1111-4111-8111-111111111111","message":"Xin chào"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void legacyConversationalEndpointsAreNotMapped() throws Exception {
        mockMvc.perform(post("/api/v1/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/ai/policy-assistant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }
}
