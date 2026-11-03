package com.tpanh.hotel.authentication.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tpanh.hotel.authentication.config.PasswordResetProperties;
import com.tpanh.hotel.authentication.domain.entity.PasswordResetToken;
import com.tpanh.hotel.authentication.domain.enums.UserStatus;
import com.tpanh.hotel.authentication.dto.request.ResetPasswordRequest;
import com.tpanh.hotel.authentication.exception.InvalidPasswordResetTokenException;
import com.tpanh.hotel.authentication.exception.PasswordMismatchException;
import com.tpanh.hotel.authentication.exception.PasswordResetTokenExpiredException;
import com.tpanh.hotel.authentication.exception.PasswordResetTokenUsedException;
import com.tpanh.hotel.authentication.repository.PasswordResetTokenRepository;
import com.tpanh.hotel.authentication.repository.RefreshTokenRepository;
import com.tpanh.hotel.authentication.security.RefreshTokenGenerator;
import com.tpanh.hotel.authentication.security.TokenHashService;
import com.tpanh.hotel.authentication.service.AccessTokenRevocationService;
import com.tpanh.hotel.user.domain.User;
import com.tpanh.hotel.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PasswordResetTokenServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-08-10T06:00:00Z");

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenGenerator tokenGenerator;

    @Mock
    private TokenHashService tokenHashService;

    @Mock
    private AccessTokenRevocationService accessTokenRevocationService;

    private PasswordResetTokenServiceImpl service;

    @BeforeEach
    void setUp() {
        PasswordResetProperties properties = new PasswordResetProperties(
                15,
                "http://localhost:5173/reset-password",
                "no-reply@example.com",
                "support@example.com",
                60,
                5,
                20,
                10,
                7);
        service = new PasswordResetTokenServiceImpl(
                passwordResetTokenRepository,
                refreshTokenRepository,
                userRepository,
                passwordEncoder,
                tokenGenerator,
                tokenHashService,
                properties,
                accessTokenRevocationService,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void issueTokenShouldRevokeOldTokenAndStoreOnlyHash() {
        User user = new User();
        user.setId(UUID.randomUUID());
        when(tokenGenerator.generate()).thenReturn("raw-reset-token");
        when(tokenHashService.hash("raw-reset-token")).thenReturn("hashed-reset-token");

        String rawToken = service.issueToken(user);

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).saveAndFlush(tokenCaptor.capture());
        PasswordResetToken storedToken = tokenCaptor.getValue();
        assertThat(rawToken).isEqualTo("raw-reset-token");
        assertThat(storedToken.getTokenHash()).isEqualTo("hashed-reset-token");
        assertThat(storedToken.getTokenHash()).doesNotContain(rawToken);
        assertThat(storedToken.getUser()).isSameAs(user);
        assertThat(storedToken.getCreatedAt()).isEqualTo(NOW);
        assertThat(storedToken.getExpiredAt()).isEqualTo(NOW.plusSeconds(900));

        InOrder order = inOrder(passwordResetTokenRepository);
        order.verify(passwordResetTokenRepository).revokeActiveTokensByUserId(user.getId(), NOW);
        order.verify(passwordResetTokenRepository).saveAndFlush(storedToken);
    }

    @Test
    void verifyTokenShouldReturnExpirationWhenTokenIsValid() {
        PasswordResetToken token = validToken();
        when(tokenHashService.hash("raw-reset-token")).thenReturn("hashed-reset-token");
        when(passwordResetTokenRepository.findByTokenHash("hashed-reset-token"))
                .thenReturn(java.util.Optional.of(token));

        var response = service.verifyToken("raw-reset-token");

        assertThat(response.status()).isEqualTo("RESET_TOKEN_VALID");
        assertThat(response.expiresAt()).isEqualTo(token.getExpiredAt());
    }

    @Test
    void verifyTokenShouldRejectUnknownToken() {
        when(tokenHashService.hash("unknown-reset-token")).thenReturn("unknown-hash");
        when(passwordResetTokenRepository.findByTokenHash("unknown-hash")).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service.verifyToken("unknown-reset-token"))
                .isInstanceOf(InvalidPasswordResetTokenException.class);
    }

    @Test
    void verifyTokenShouldRejectExpiredToken() {
        PasswordResetToken token = validToken();
        token.setExpiredAt(NOW);
        when(tokenHashService.hash("expired-reset-token")).thenReturn("expired-hash");
        when(passwordResetTokenRepository.findByTokenHash("expired-hash")).thenReturn(java.util.Optional.of(token));

        assertThatThrownBy(() -> service.verifyToken("expired-reset-token"))
                .isInstanceOf(PasswordResetTokenExpiredException.class);
    }

    @Test
    void verifyTokenShouldRejectUsedToken() {
        PasswordResetToken token = validToken();
        token.setUsedAt(NOW.minusSeconds(30));
        when(tokenHashService.hash("used-reset-token")).thenReturn("used-hash");
        when(passwordResetTokenRepository.findByTokenHash("used-hash")).thenReturn(java.util.Optional.of(token));

        assertThatThrownBy(() -> service.verifyToken("used-reset-token"))
                .isInstanceOf(PasswordResetTokenUsedException.class);
    }

    @Test
    void verifyTokenShouldRejectRevokedToken() {
        PasswordResetToken token = validToken();
        token.setRevokedAt(NOW.minusSeconds(30));
        when(tokenHashService.hash("revoked-reset-token")).thenReturn("revoked-hash");
        when(passwordResetTokenRepository.findByTokenHash("revoked-hash")).thenReturn(java.util.Optional.of(token));

        assertThatThrownBy(() -> service.verifyToken("revoked-reset-token"))
                .isInstanceOf(PasswordResetTokenUsedException.class);
    }

    @Test
    void resetPasswordShouldEncodePasswordConsumeTokenAndRevokeSessions() {
        PasswordResetToken token = validToken();
        User user = token.getUser();
        when(tokenHashService.hash("raw-reset-token")).thenReturn("hashed-reset-token");
        when(passwordResetTokenRepository.findByTokenHashForUpdate("hashed-reset-token"))
                .thenReturn(java.util.Optional.of(token));
        when(passwordEncoder.encode("NewPassword123")).thenReturn("encoded-new-password");

        var response =
                service.resetPassword(new ResetPasswordRequest("raw-reset-token", "NewPassword123", "NewPassword123"));

        assertThat(response.status()).isEqualTo("PASSWORD_RESET");
        assertThat(user.getPasswordHash()).isEqualTo("encoded-new-password");
        assertThat(token.getUsedAt()).isEqualTo(NOW);
        verify(userRepository).save(user);
        verify(passwordResetTokenRepository).revokeOtherActiveTokensByUserId(user.getId(), token.getId(), NOW);
        verify(refreshTokenRepository).revokeActiveTokensByUserId(user.getId(), NOW);
        verify(accessTokenRevocationService).revokeAll(user.getId());
    }

    @Test
    void resetPasswordShouldRejectPasswordMismatchBeforeTokenLookup() {
        ResetPasswordRequest request =
                new ResetPasswordRequest("raw-reset-token", "NewPassword123", "OtherPassword123");

        assertThatThrownBy(() -> service.resetPassword(request)).isInstanceOf(PasswordMismatchException.class);
        org.mockito.Mockito.verifyNoInteractions(tokenHashService, passwordResetTokenRepository, passwordEncoder);
    }

    @Test
    void resetTokenShouldNotBeReusableAfterSuccessfulReset() {
        PasswordResetToken token = validToken();
        when(tokenHashService.hash("raw-reset-token")).thenReturn("hashed-reset-token");
        when(passwordResetTokenRepository.findByTokenHashForUpdate("hashed-reset-token"))
                .thenReturn(java.util.Optional.of(token));
        when(passwordEncoder.encode("NewPassword123")).thenReturn("encoded-new-password");
        ResetPasswordRequest request = new ResetPasswordRequest("raw-reset-token", "NewPassword123", "NewPassword123");

        service.resetPassword(request);

        assertThatThrownBy(() -> service.resetPassword(request)).isInstanceOf(PasswordResetTokenUsedException.class);
    }

    private PasswordResetToken validToken() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setStatus(UserStatus.ACTIVE);
        PasswordResetToken token = new PasswordResetToken();
        token.setId(UUID.randomUUID());
        token.setUser(user);
        token.setTokenHash("hashed-reset-token");
        token.setCreatedAt(NOW.minusSeconds(60));
        token.setExpiredAt(NOW.plusSeconds(900));
        return token;
    }
}
