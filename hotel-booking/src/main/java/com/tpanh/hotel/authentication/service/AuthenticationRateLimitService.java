package com.tpanh.hotel.authentication.service;

import com.tpanh.hotel.authentication.exception.AuthenticationRateLimitExceededException;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationRateLimitService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationRateLimitService.class);
    private static final DefaultRedisScript<Long> INCREMENT_WITH_EXPIRY = new DefaultRedisScript<>(
            """
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]) end
            return count
            """,
            Long.class);

    private final StringRedisTemplate redisTemplate;
    private final Clock clock;
    private static final int MAX_LOCAL_KEYS = 10000;
    private long nextCleanupMillis;
    private final Map<String, LocalWindow> localFallback = new ConcurrentHashMap<>();

    public void check(String action, String identity, int limit, Duration window) {
        String key = key(action, identity);
        LocalWindow local = incrementLocal(key, window);
        long count;
        boolean redisAvailable = true;
        try {
            Long redisCount = redisTemplate.execute(
                    INCREMENT_WITH_EXPIRY, java.util.List.of(key), Long.toString(window.toMillis()));
            count = redisCount == null ? limit + 1L : redisCount;
        } catch (RuntimeException exception) {
            LOGGER.warn("Redis không khả dụng, dùng rate limit cục bộ cho {}", action);
            redisAvailable = false;
            count = local.count.get();
        }
        if (local.count.get() > limit) {
            throw new AuthenticationRateLimitExceededException(remainingSeconds(local));
        }
        if (count > limit) {
            throw new AuthenticationRateLimitExceededException(
                    redisAvailable ? redisRetryAfter(key, window) : remainingSeconds(local));
        }
    }

    public void ensureAllowed(String action, String identity, int limit, Duration window) {
        String key = key(action, identity);
        LocalWindow local = localFallback.get(key);
        if (local != null && local.expiresAtMillis > clock.millis() && local.count.get() >= limit) {
            throw new AuthenticationRateLimitExceededException(remainingSeconds(local));
        }
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value != null && Long.parseLong(value) >= limit) {
                throw new AuthenticationRateLimitExceededException(redisRetryAfter(key, window));
            }
        } catch (AuthenticationRateLimitExceededException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            LOGGER.warn("Redis không khả dụng, dùng trạng thái giới hạn cục bộ cho {}", action);
        }
    }

    private long redisRetryAfter(String key, Duration window) {
        try {
            Long ttl = redisTemplate.getExpire(key, java.util.concurrent.TimeUnit.MILLISECONDS);
            return ttl == null || ttl < 0 ? window.toSeconds() : Math.max(1, (ttl + 999) / 1000);
        } catch (RuntimeException exception) {
            return window.toSeconds();
        }
    }

    private long remainingSeconds(LocalWindow local) {
        return Math.max(1, (local.expiresAtMillis - clock.millis() + 999) / 1000);
    }

    public void clear(String action, String identity) {
        String key = key(action, identity);
        localFallback.remove(key);
        try {
            redisTemplate.delete(key);
        } catch (RuntimeException exception) {
            LOGGER.warn("Redis không khả dụng khi xóa rate limit cho {}", action);
        }
    }

    private String key(String action, String identity) {
        return "auth:rate-limit:" + action + ":" + identity;
    }

    private synchronized LocalWindow incrementLocal(String key, Duration window) {
        long now = clock.millis();
        if (now >= nextCleanupMillis || localFallback.size() >= MAX_LOCAL_KEYS) {
            localFallback.entrySet().removeIf(entry -> entry.getValue().expiresAtMillis <= now);
            nextCleanupMillis = now + 60000;
        }
        if (!localFallback.containsKey(key) && localFallback.size() >= MAX_LOCAL_KEYS) {
            throw new AuthenticationRateLimitExceededException(60);
        }
        LocalWindow current = localFallback.compute(key, (ignored, existing) -> {
            if (existing == null || existing.expiresAtMillis <= now) {
                return new LocalWindow(new AtomicInteger(1), now + window.toMillis());
            }
            existing.count.incrementAndGet();
            return existing;
        });
        return current;
    }

    private record LocalWindow(AtomicInteger count, long expiresAtMillis) {}
}
