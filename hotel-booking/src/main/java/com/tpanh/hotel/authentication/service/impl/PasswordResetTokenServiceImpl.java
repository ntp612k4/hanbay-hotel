package com.tpanh.hotel.authentication.service.impl;

import com.tpanh.hotel.authentication.config.PasswordResetProperties;
import com.tpanh.hotel.authentication.domain.entity.PasswordResetToken;
import com.tpanh.hotel.authentication.domain.enums.UserStatus;
import com.tpanh.hotel.authentication.dto.request.ResetPasswordRequest;
import com.tpanh.hotel.authentication.dto.response.ResetPasswordResponse;
import com.tpanh.hotel.authentication.dto.response.ResetTokenVerificationResponse;
import com.tpanh.hotel.authentication.exception.InvalidPasswordResetTokenException;
import com.tpanh.hotel.authentication.exception.PasswordMismatchException;
import com.tpanh.hotel.authentication.exception.PasswordResetTokenExpiredException;
import com.tpanh.hotel.authentication.exception.PasswordResetTokenUsedException;
import com.tpanh.hotel.authentication.repository.PasswordResetTokenRepository;
import com.tpanh.hotel.authentication.repository.RefreshTokenRepository;
import com.tpanh.hotel.authentication.security.RefreshTokenGenerator;
import com.tpanh.hotel.authentication.security.TokenHashService;
import com.tpanh.hotel.authentication.service.AccessTokenRevocationService;
import com.tpanh.hotel.authentication.service.PasswordResetTokenService;
import com.tpanh.hotel.user.domain.User;
import com.tpanh.hotel.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordResetTokenServiceImpl implements PasswordResetTokenService {

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenGenerator tokenGenerator;
    private final TokenHashService tokenHashService;
    private final PasswordResetProperties passwordResetProperties;
    private final AccessTokenRevocationService accessTokenRevocationService;
    private final Clock clock;

    @Override
    @Transactional
    public String issueToken(User user) {
        Instant now = clock.instant();
        passwordResetTokenRepository.revokeActiveTokensByUserId(user.getId(), now);

        String rawToken = tokenGenerator.generate();
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(tokenHashService.hash(rawToken));
        token.setCreatedAt(now);
        token.setExpiredAt(now.plus(passwordResetProperties.expirationMinutes(), ChronoUnit.MINUTES));
        passwordResetTokenRepository.saveAndFlush(token);
        return rawToken;
    }

    @Override
    @Transactional
    public void revokeToken(String rawToken) {
        passwordResetTokenRepository.revokeByTokenHash(tokenHashService.hash(rawToken), clock.instant());
    }

    @Override
    @Transactional(readOnly = true)
    public ResetTokenVerificationResponse verifyToken(String rawToken) {
        Instant now = clock.instant();
        PasswordResetToken token = passwordResetTokenRepository
                .findByTokenHash(tokenHashService.hash(rawToken))
                .orElseThrow(InvalidPasswordResetTokenException::new);
        validateToken(token, now);
        return new ResetTokenVerificationResponse("RESET_TOKEN_VALID", token.getExpiredAt());
    }

    @Override
    @Transactional
    public ResetPasswordResponse resetPassword(ResetPasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new PasswordMismatchException();
        }

        Instant now = clock.instant();
        PasswordResetToken token = passwordResetTokenRepository
                .findByTokenHashForUpdate(tokenHashService.hash(request.token()))
                .orElseThrow(InvalidPasswordResetTokenException::new);
        validateToken(token, now);

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        token.setUsedAt(now);

        userRepository.save(user);
        passwordResetTokenRepository.save(token);
        passwordResetTokenRepository.revokeOtherActiveTokensByUserId(user.getId(), token.getId(), now);
        refreshTokenRepository.revokeActiveTokensByUserId(user.getId(), now);
        accessTokenRevocationService.revokeAll(user.getId());
        return new ResetPasswordResponse("PASSWORD_RESET");
    }

    private void validateToken(PasswordResetToken token, Instant now) {
        if (token.getUsedAt() != null || token.getRevokedAt() != null) {
            throw new PasswordResetTokenUsedException();
        }
        if (!token.getExpiredAt().isAfter(now)) {
            throw new PasswordResetTokenExpiredException();
        }
        if (token.getUser().getStatus() != UserStatus.ACTIVE) {
            throw new InvalidPasswordResetTokenException();
        }
    }
}
