package com.tpanh.hotel.ai.dto;

import java.util.List;

public record AiChatStreamEvent(String type, String content, AiUiAction action, List<AiPolicySource> sources) {

    public static AiChatStreamEvent delta(String content) {
        return new AiChatStreamEvent("delta", content, null, List.of());
    }

    public static AiChatStreamEvent action(AiUiAction action) {
        return new AiChatStreamEvent("action", "", action, List.of());
    }

    public static AiChatStreamEvent sources(List<AiPolicySource> sources) {
        return new AiChatStreamEvent("sources", "", null, List.copyOf(sources));
    }

    public static AiChatStreamEvent done() {
        return new AiChatStreamEvent("done", "", null, List.of());
    }

    public static AiChatStreamEvent error() {
        return new AiChatStreamEvent("error", "Trợ lý AI chưa thể trả lời. Vui lòng thử lại.", null, List.of());
    }
}
