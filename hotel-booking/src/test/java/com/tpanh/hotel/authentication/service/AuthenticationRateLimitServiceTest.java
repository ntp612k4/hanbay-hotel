package com.tpanh.hotel.authentication.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import com.tpanh.hotel.authentication.exception.AuthenticationRateLimitExceededException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

class AuthenticationRateLimitServiceTest {

    @Test
    void shouldRejectRequestWhenRedisCounterExceedsLimit() {
        StringRedisTemplate redisTemplate = org.mockito.Mockito.mock(StringRedisTemplate.class);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any())).thenReturn(6L);
        AuthenticationRateLimitService service = new AuthenticationRateLimitService(
                redisTemplate, Clock.fixed(Instant.parse("2026-08-10T10:00:00Z"), ZoneOffset.UTC));

        assertThatThrownBy(() -> service.check("forgot", "127.0.0.1", 5, Duration.ofMinutes(1)))
                .isInstanceOf(AuthenticationRateLimitExceededException.class);
    }

    @Test
    void shouldKeepLimitingLocallyWhenRedisIsUnavailable() {
        StringRedisTemplate redisTemplate = org.mockito.Mockito.mock(StringRedisTemplate.class);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any()))
                .thenThrow(new IllegalStateException("Redis unavailable"));
        AuthenticationRateLimitService service = new AuthenticationRateLimitService(
                redisTemplate, Clock.fixed(Instant.parse("2026-08-10T10:00:00Z"), ZoneOffset.UTC));

        service.check("forgot", "127.0.0.1", 1, Duration.ofMinutes(1));

        assertThatThrownBy(() -> service.check("forgot", "127.0.0.1", 1, Duration.ofMinutes(1)))
                .isInstanceOf(AuthenticationRateLimitExceededException.class);
    }

    @Test
    void shouldBlockAnIdentityAfterTheConfiguredNumberOfFailuresAndAllowItAfterClear() {
        StringRedisTemplate redisTemplate = org.mockito.Mockito.mock(StringRedisTemplate.class);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any()))
                .thenThrow(new IllegalStateException("Redis unavailable"));
        AuthenticationRateLimitService service = new AuthenticationRateLimitService(
                redisTemplate, Clock.fixed(Instant.parse("2026-08-10T10:00:00Z"), ZoneOffset.UTC));

        service.check("login-account", "identity-hash", 2, Duration.ofMinutes(15));
        service.check("login-account", "identity-hash", 2, Duration.ofMinutes(15));

        assertThatThrownBy(() -> service.ensureAllowed("login-account", "identity-hash", 2, Duration.ofMinutes(15)))
                .isInstanceOf(AuthenticationRateLimitExceededException.class);

        service.clear("login-account", "identity-hash");

        assertThatCode(() -> service.ensureAllowed("login-account", "identity-hash", 2, Duration.ofMinutes(15)))
                .doesNotThrowAnyException();
    }

    @Test
    void keepsFailuresWhenRedisRecoversAndExpiresThemWithoutExtendingTheWindow() {
        StringRedisTemplate redis = org.mockito.Mockito.mock(StringRedisTemplate.class);
        var values = org.mockito.Mockito.mock(org.springframework.data.redis.core.ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(redis.execute(any(RedisScript.class), anyList(), any())).thenThrow(new IllegalStateException("offline"));
        Clock clock = org.mockito.Mockito.mock(Clock.class);
        when(clock.millis()).thenReturn(0L);
        var service = new AuthenticationRateLimitService(redis, clock);
        service.check("login-account-ip", "key", 2, Duration.ofSeconds(60));
        service.check("login-account-ip", "key", 2, Duration.ofSeconds(60));
        when(clock.millis()).thenReturn(50000L);
        // Redis has recovered with no key. The local failures must still apply.
        assertThatThrownBy(() -> service.ensureAllowed("login-account-ip", "key", 2, Duration.ofSeconds(60)))
                .isInstanceOf(AuthenticationRateLimitExceededException.class)
                .extracting("retryAfterSeconds")
                .isEqualTo(10L);
        when(clock.millis()).thenReturn(60000L);
        assertThatCode(() -> service.ensureAllowed("login-account-ip", "key", 2, Duration.ofSeconds(60)))
                .doesNotThrowAnyException();
    }

    @Test
    void returnsRemainingRedisWindowInsteadOfRestartingCooldown() {
        StringRedisTemplate redis = org.mockito.Mockito.mock(StringRedisTemplate.class);
        when(redis.execute(any(RedisScript.class), anyList(), any())).thenReturn(31L);
        when(redis.getExpire(
                        any(String.class), org.mockito.ArgumentMatchers.eq(java.util.concurrent.TimeUnit.MILLISECONDS)))
                .thenReturn(4200L);
        var service = new AuthenticationRateLimitService(redis, Clock.systemUTC());
        assertThatThrownBy(() -> service.check("login-ip", "ip", 30, Duration.ofSeconds(60)))
                .isInstanceOf(AuthenticationRateLimitExceededException.class)
                .extracting("retryAfterSeconds")
                .isEqualTo(5L);
    }

    @Test
    void boundsLocalKeysAndReclaimsExpiredEntries() {
        StringRedisTemplate redis = org.mockito.Mockito.mock(StringRedisTemplate.class);
        when(redis.execute(any(RedisScript.class), anyList(), any())).thenReturn(1L);
        Clock clock = org.mockito.Mockito.mock(Clock.class);
        when(clock.millis()).thenReturn(0L);
        var service = new AuthenticationRateLimitService(redis, clock);
        for (int i = 0; i < 10000; i++) service.check("login-ip", "ip-" + i, 30, Duration.ofSeconds(60));
        assertThatThrownBy(() -> service.check("login-ip", "overflow", 30, Duration.ofSeconds(60)))
                .isInstanceOf(AuthenticationRateLimitExceededException.class);
        when(clock.millis()).thenReturn(60000L);
        assertThatCode(() -> service.check("login-ip", "new-ip", 30, Duration.ofSeconds(60)))
                .doesNotThrowAnyException();
    }
}
