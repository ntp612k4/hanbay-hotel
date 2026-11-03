package com.tpanh.hotel.authentication.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import com.tpanh.hotel.common.config.JwtProperties;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

class AccessTokenRevocationServiceTest {

    @Test
    void shouldInvalidatePreviousTokenVersionAfterRevocation() {
        StringRedisTemplate redisTemplate = org.mockito.Mockito.mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = org.mockito.Mockito.mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any())).thenReturn(1L);
        when(valueOperations.get(any())).thenReturn("1");
        AccessTokenRevocationService service = new AccessTokenRevocationService(
                redisTemplate, new JwtProperties("hotel-booking", "a-secret-with-at-least-32-characters", 3600, 86400));
        UUID userId = UUID.randomUUID();

        service.revokeAll(userId);

        assertThat(service.isRevoked(userId.toString(), 0L)).isTrue();
        assertThat(service.isRevoked(userId.toString(), 1L)).isFalse();
    }
}
