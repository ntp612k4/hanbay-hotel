package com.tpanh.hotel.authentication.service.impl;

import com.tpanh.hotel.authentication.domain.entity.RefreshToken;
import com.tpanh.hotel.authentication.domain.enums.UserStatus;
import com.tpanh.hotel.authentication.dto.request.RefreshTokenRequest;
import com.tpanh.hotel.authentication.dto.response.RefreshTokenResponse;
import com.tpanh.hotel.authentication.exception.InvalidRefreshTokenException;
import com.tpanh.hotel.authentication.exception.RefreshTokenExpiredException;
import com.tpanh.hotel.authentication.repository.RefreshTokenRepository;
import com.tpanh.hotel.authentication.security.RefreshTokenGenerator;
import com.tpanh.hotel.authentication.security.TokenHashService;
import com.tpanh.hotel.authentication.service.RefreshTokenService;
import com.tpanh.hotel.common.config.JwtProperties;
import com.tpanh.hotel.user.domain.User;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenHashService tokenHashService;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    public RefreshTokenServiceImpl(
            RefreshTokenRepository refreshTokenRepository,
            TokenHashService tokenHashService,
            RefreshTokenGenerator refreshTokenGenerator,
            JwtService jwtService,
            JwtProperties jwtProperties,
            Clock clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenHashService = tokenHashService;
        this.refreshTokenGenerator = refreshTokenGenerator;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public RefreshTokenResponse refreshAccessToken(RefreshTokenRequest request) {
        Instant now = clock.instant();
        String currentTokenHash = tokenHashService.hash(request.refreshToken());
        RefreshToken currentToken = refreshTokenRepository
                .findByTokenHashForUpdate(currentTokenHash)
                .orElseThrow(InvalidRefreshTokenException::new);

        if (currentToken.getRevokedAt() != null) {
            throw new InvalidRefreshTokenException();
        }

        User user = currentToken.getUser();
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new InvalidRefreshTokenException();
        }

        if (!currentToken.getExpiredAt().isAfter(now)) {
            throw new RefreshTokenExpiredException();
        }

        currentToken.setRevokedAt(now);
        String newRawRefreshToken = refreshTokenGenerator.generate();
        RefreshToken newRefreshToken = new RefreshToken();
        newRefreshToken.setUser(user);
        newRefreshToken.setTokenHash(tokenHashService.hash(newRawRefreshToken));
        newRefreshToken.setCreatedAt(now);
        newRefreshToken.setExpiredAt(now.plusSeconds(jwtProperties.refreshTokenExpirationSeconds()));

        refreshTokenRepository.save(currentToken);
        refreshTokenRepository.save(newRefreshToken);

        String accessToken = jwtService.generateAccessToken(user);
        return new RefreshTokenResponse(
                accessToken, newRawRefreshToken, "Bearer", jwtProperties.accessTokenExpirationSeconds());
    }

    @Override
    @Transactional
    public String issueRefreshToken(User user) {
        Instant now = clock.instant();
        String rawToken = refreshTokenGenerator.generate();
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(tokenHashService.hash(rawToken));
        token.setCreatedAt(now);
        token.setExpiredAt(now.plusSeconds(jwtProperties.refreshTokenExpirationSeconds()));
        refreshTokenRepository.save(token);
        return rawToken;
    }
}
