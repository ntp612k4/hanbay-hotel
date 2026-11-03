package com.tpanh.hotel.authentication.service;

import com.tpanh.hotel.common.config.JwtProperties;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccessTokenRevocationService {

    private static final DefaultRedisScript<Long> INCREMENT_WITH_EXPIRY = new DefaultRedisScript<>(
            """
            local version = redis.call('INCR', KEYS[1])
            redis.call('EXPIRE', KEYS[1], ARGV[1])
            return version
            """,
            Long.class);

    private final StringRedisTemplate redisTemplate;
    private final JwtProperties jwtProperties;

    public void revokeAll(UUID userId) {
        Long version = redisTemplate.execute(
                INCREMENT_WITH_EXPIRY,
                List.of(key(userId)),
                Long.toString(Duration.ofSeconds(jwtProperties.accessTokenExpirationSeconds())
                        .toSeconds()));
        if (version == null) {
            throw new IllegalStateException("Không thể cập nhật phiên bản access token trong Redis");
        }
    }

    public long currentVersion(UUID userId) {
        String value = redisTemplate.opsForValue().get(key(userId));
        return value == null ? 0L : Long.parseLong(value);
    }

    public boolean isRevoked(String subject, long tokenVersion) {
        try {
            return tokenVersion != currentVersion(UUID.fromString(subject));
        } catch (IllegalArgumentException exception) {
            return true;
        }
    }

    private String key(UUID userId) {
        return "auth:access-token:version:" + userId;
    }
}
