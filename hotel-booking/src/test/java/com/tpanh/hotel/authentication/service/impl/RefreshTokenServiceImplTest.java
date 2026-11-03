package com.tpanh.hotel.authentication.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tpanh.hotel.authentication.domain.entity.RefreshToken;
import com.tpanh.hotel.authentication.domain.enums.UserStatus;
import com.tpanh.hotel.authentication.dto.request.RefreshTokenRequest;
import com.tpanh.hotel.authentication.dto.response.RefreshTokenResponse;
import com.tpanh.hotel.authentication.exception.InvalidRefreshTokenException;
import com.tpanh.hotel.authentication.exception.RefreshTokenExpiredException;
import com.tpanh.hotel.authentication.repository.RefreshTokenRepository;
import com.tpanh.hotel.authentication.security.RefreshTokenGenerator;
import com.tpanh.hotel.authentication.security.TokenHashService;
import com.tpanh.hotel.common.config.JwtProperties;
import com.tpanh.hotel.user.domain.User;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-08-04T08:00:00Z");

    @Mock
    private RefreshTokenRepository repository;

    @Mock
    private TokenHashService hashService;

    @Mock
    private RefreshTokenGenerator refreshTokenGenerator;

    @Mock
    private JwtService jwtService;

    private RefreshTokenServiceImpl service;

    @BeforeEach
    void setUp() {
        JwtProperties properties =
                new JwtProperties("hotel-booking-system", "test-secret-at-least-32-characters-long", 900, 2_592_000);
        service = new RefreshTokenServiceImpl(
                repository,
                hashService,
                refreshTokenGenerator,
                jwtService,
                properties,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void refreshAccessTokenShouldRotateRefreshToken() {
        User user = activeUser();
        RefreshToken currentToken = validToken(user);
        when(hashService.hash("current-token")).thenReturn("current-hash");
        when(repository.findByTokenHashForUpdate("current-hash")).thenReturn(Optional.of(currentToken));
        when(refreshTokenGenerator.generate()).thenReturn("new-token");
        when(hashService.hash("new-token")).thenReturn("new-hash");
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");

        RefreshTokenResponse response = service.refreshAccessToken(new RefreshTokenRequest("current-token"));

        assertEquals("access-token", response.accessToken());
        assertEquals("new-token", response.refreshToken());
        assertEquals(NOW, currentToken.getRevokedAt());
        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository, org.mockito.Mockito.times(2)).save(tokenCaptor.capture());
        RefreshToken newToken = tokenCaptor.getAllValues().get(1);
        assertEquals("new-hash", newToken.getTokenHash());
        assertEquals(NOW.plusSeconds(2_592_000), newToken.getExpiredAt());
    }

    @Test
    void refreshAccessTokenShouldRejectUnknownToken() {
        when(hashService.hash("unknown")).thenReturn("unknown-hash");
        when(repository.findByTokenHashForUpdate("unknown-hash")).thenReturn(Optional.empty());

        assertThrows(
                InvalidRefreshTokenException.class,
                () -> service.refreshAccessToken(new RefreshTokenRequest("unknown")));
        verify(repository, never()).save(any());
    }

    @Test
    void refreshAccessTokenShouldRejectRevokedToken() {
        RefreshToken token = validToken(activeUser());
        token.setRevokedAt(NOW.minusSeconds(1));
        prepareLookup(token);

        assertThrows(
                InvalidRefreshTokenException.class,
                () -> service.refreshAccessToken(new RefreshTokenRequest("current-token")));
    }

    @Test
    void refreshAccessTokenShouldRejectExpiredToken() {
        RefreshToken token = validToken(activeUser());
        token.setExpiredAt(NOW);
        prepareLookup(token);

        assertThrows(
                RefreshTokenExpiredException.class,
                () -> service.refreshAccessToken(new RefreshTokenRequest("current-token")));
    }

    @Test
    void refreshAccessTokenShouldRejectInactiveUser() {
        User user = activeUser();
        user.setStatus(UserStatus.LOCKED);
        prepareLookup(validToken(user));

        assertThrows(
                InvalidRefreshTokenException.class,
                () -> service.refreshAccessToken(new RefreshTokenRequest("current-token")));
    }

    private void prepareLookup(RefreshToken token) {
        when(hashService.hash("current-token")).thenReturn("current-hash");
        when(repository.findByTokenHashForUpdate("current-hash")).thenReturn(Optional.of(token));
    }

    private User activeUser() {
        User user = new User();
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    private RefreshToken validToken(User user) {
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setExpiredAt(NOW.plusSeconds(60));
        return token;
    }
}
