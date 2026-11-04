package com.tpanh.hotel.authentication.service.impl;

import com.tpanh.hotel.authentication.domain.enums.UserStatus;
import com.tpanh.hotel.authentication.service.AccessTokenRevocationService;
import com.tpanh.hotel.common.config.JwtProperties;
import com.tpanh.hotel.user.domain.User;
import com.tpanh.hotel.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    public static final String ROLES_CLAIM = "roles";
    public static final String TOKEN_VERSION_CLAIM = "tokenVersion";

    private final JwtProperties jwtProperties;
    private final AccessTokenRevocationService accessTokenRevocationService;
    private final SecretKey secretKey;
    private final UserRepository userRepository;

    public JwtService(
            JwtProperties jwtProperties,
            AccessTokenRevocationService accessTokenRevocationService,
            UserRepository userRepository) {
        this.jwtProperties = jwtProperties;
        this.userRepository = userRepository;
        this.accessTokenRevocationService = accessTokenRevocationService;
        this.secretKey = Keys.hmacShaKeyFor(hash(jwtProperties.secret()));
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(jwtProperties.accessTokenExpirationSeconds());

        return Jwts.builder()
                .issuer(jwtProperties.issuer())
                .subject(user.getId().toString())
                .claim(
                        ROLES_CLAIM,
                        user.getRoles().stream()
                                .map(role -> role.getCode().name())
                                .sorted()
                                .toList())
                .claim(TOKEN_VERSION_CLAIM, accessTokenRevocationService.currentVersion(user.getId()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }

    public Optional<ValidatedJwt> validate(String token) {
        try {
            Claims claims = parseClaims(token);
            Object rolesClaim = claims.get(ROLES_CLAIM);
            Object tokenVersionClaim = claims.get(TOKEN_VERSION_CLAIM);
            if (!(rolesClaim instanceof List<?> roles)
                    || !(tokenVersionClaim instanceof Number tokenVersion)
                    || claims.getSubject() == null
                    || accessTokenRevocationService.isRevoked(claims.getSubject(), tokenVersion.longValue())) {
                return Optional.empty();
            }
            User user = userRepository
                    .findProfileById(UUID.fromString(claims.getSubject()))
                    .orElse(null);
            if (user == null
                    || user.getStatus() != UserStatus.ACTIVE
                    || !user.getRoles().stream()
                            .map(role -> role.getCode().name())
                            .collect(Collectors.toSet())
                            .equals(new HashSet<>(
                                    roles.stream().map(String::valueOf).toList()))) {
                return Optional.empty();
            }
            return Optional.of(new ValidatedJwt(
                    claims.getSubject(), roles.stream().map(String::valueOf).toList()));
        } catch (JwtException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(jwtProperties.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private byte[] hash(String secret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(secret.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Missing SHA-256 algorithm", exception);
        }
    }

    public record ValidatedJwt(String subject, List<String> roles) {}
}
