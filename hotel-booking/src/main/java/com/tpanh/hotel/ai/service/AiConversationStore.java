package com.tpanh.hotel.ai.service;

import com.tpanh.hotel.common.security.CurrentUserProvider;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiConversationStore {

    private static final Duration TTL = Duration.ofHours(1);
    private static final int MAX_MESSAGES = 12;
    private static final TypeReference<List<Message>> MESSAGE_LIST = new TypeReference<>() {};

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final CurrentUserProvider currentUserProvider;
    private final ConcurrentHashMap<String, List<Message>> fallback = new ConcurrentHashMap<>();

    public UUID requireCurrentUserId() {
        return currentUserProvider
                .currentUserId()
                .orElseThrow(() -> new IllegalStateException("AI conversation yêu cầu đăng nhập"));
    }

    public List<Message> get(UUID userId, UUID conversationId) {
        String key = key(userId, conversationId);
        try {
            String value = redisTemplate.opsForValue().get(key);
            return value == null ? List.of() : objectMapper.readValue(value, MESSAGE_LIST);
        } catch (RuntimeException exception) {
            log.warn("Không thể đọc AI conversation từ Redis, dùng bộ nhớ cục bộ");
            return List.copyOf(fallback.getOrDefault(key, List.of()));
        }
    }

    public void append(UUID userId, UUID conversationId, String userMessage, String assistantMessage) {
        String key = key(userId, conversationId);
        List<Message> messages = new ArrayList<>(get(userId, conversationId));
        messages.add(new Message("user", userMessage));
        messages.add(new Message("assistant", assistantMessage));
        if (messages.size() > MAX_MESSAGES) {
            messages = new ArrayList<>(messages.subList(messages.size() - MAX_MESSAGES, messages.size()));
        }
        fallback.put(key, List.copyOf(messages));
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(messages), TTL);
        } catch (RuntimeException exception) {
            log.warn("Không thể lưu AI conversation vào Redis, dùng bộ nhớ cục bộ");
        }
    }

    private String key(UUID userId, UUID conversationId) {
        return "ai:conversation:" + userId + ":" + conversationId;
    }

    public record Message(String role, String content) {}
}
