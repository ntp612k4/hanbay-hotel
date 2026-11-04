package com.tpanh.hotel.features.authentication.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tpanh.hotel.authentication.domain.entity.Role;
import com.tpanh.hotel.authentication.domain.enums.UserStatus;
import com.tpanh.hotel.authentication.service.AccessTokenRevocationService;
import com.tpanh.hotel.authentication.service.impl.JwtService;
import com.tpanh.hotel.common.config.JwtProperties;
import com.tpanh.hotel.user.domain.User;
import com.tpanh.hotel.user.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private final AccessTokenRevocationService revocationService = revocationService();
    private final UserRepository userRepository = mock(UserRepository.class);
    private final JwtService jwtService = new JwtService(
            new JwtProperties("hotel-booking-system", "test-secret-123", 3600, 604800),
            revocationService,
            userRepository);

    @Test
    void generatedTokenShouldContainUserIdAndRoles() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setRoles(Set.of(role("MANAGER"), role("HOUSEKEEPER")));

        when(userRepository.findProfileById(user.getId())).thenReturn(Optional.of(user));
        String token = jwtService.generateAccessToken(user);

        assertThat(jwtService.validate(token)).get().satisfies(validatedJwt -> {
            assertThat(validatedJwt.subject()).isEqualTo(user.getId().toString());
            assertThat(validatedJwt.roles()).containsExactly("HOUSEKEEPER", "MANAGER");
        });
    }

    @Test
    void invalidTokenShouldBeRejected() {
        assertThat(jwtService.validate("not-a-jwt")).isEmpty();
    }

    @Test
    void accessTokenShouldBeRejectedAfterItsVersionIsRevoked() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setRoles(Set.of(role("MANAGER")));
        when(userRepository.findProfileById(user.getId())).thenReturn(Optional.of(user));
        String token = jwtService.generateAccessToken(user);
        when(revocationService.isRevoked(user.getId().toString(), 0L)).thenReturn(true);

        assertThat(jwtService.validate(token)).isEmpty();
    }

    @Test
    void tokenFromAnotherIssuerShouldBeRejected() throws NoSuchAlgorithmException {
        byte[] signingKey =
                MessageDigest.getInstance("SHA-256").digest("test-secret-123".getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .issuer("another-system")
                .subject(UUID.randomUUID().toString())
                .claim(JwtService.ROLES_CLAIM, java.util.List.of("MANAGER"))
                .signWith(Keys.hmacShaKeyFor(signingKey))
                .compact();

        assertThat(jwtService.validate(token)).isEmpty();
    }

    @Test
    void expiredTokenShouldBeRejected() {
        JwtService expiredTokenService = new JwtService(
                new JwtProperties("hotel-booking-system", "test-secret-123", -1, 604800),
                revocationService,
                userRepository);
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setRoles(Set.of(role("MANAGER")));

        String token = expiredTokenService.generateAccessToken(user);

        assertThat(expiredTokenService.validate(token)).isEmpty();
    }

    @Test
    void generatedTokenShouldPreserveAnEmptyRolesList() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setRoles(Set.of());

        when(userRepository.findProfileById(user.getId())).thenReturn(Optional.of(user));
        String token = jwtService.generateAccessToken(user);

        assertThat(jwtService.validate(token))
                .get()
                .extracting(JwtService.ValidatedJwt::roles)
                .isEqualTo(java.util.List.of());
    }

    @Test
    void tokenWithoutRolesShouldBeRejected() throws NoSuchAlgorithmException {
        byte[] signingKey =
                MessageDigest.getInstance("SHA-256").digest("test-secret-123".getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .issuer("hotel-booking-system")
                .subject(UUID.randomUUID().toString())
                .signWith(Keys.hmacShaKeyFor(signingKey))
                .compact();

        assertThat(jwtService.validate(token)).isEmpty();
    }

    @Test
    void lockedAccountCannotUseAnExistingToken() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .roles(Set.of(role("MANAGER")))
                .build();
        when(userRepository.findProfileById(user.getId())).thenReturn(Optional.of(user));
        String token = jwtService.generateAccessToken(user);
        user.setStatus(UserStatus.LOCKED);
        assertThat(jwtService.validate(token)).isEmpty();
    }

    @Test
    void changedRolesRejectOldClaimsEvenIfRedisVersionIsUnchanged() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .roles(Set.of(role("ADMIN")))
                .build();
        when(userRepository.findProfileById(user.getId())).thenReturn(Optional.of(user));
        String token = jwtService.generateAccessToken(user);
        user.setRoles(Set.of(role("RECEPTIONIST")));
        assertThat(jwtService.validate(token)).isEmpty();
    }

    @Test
    void deletedAccountCannotUseAnExistingToken() {
        User user = User.builder().id(UUID.randomUUID()).build();
        String token = jwtService.generateAccessToken(user);
        assertThat(jwtService.validate(token)).isEmpty();
    }

    private Role role(String code) {
        Role role = new Role();
        role.setCode(com.tpanh.hotel.authentication.domain.enums.RoleCode.valueOf(code));
        role.setName(code);
        return role;
    }

    private static AccessTokenRevocationService revocationService() {
        AccessTokenRevocationService service = mock(AccessTokenRevocationService.class);
        when(service.currentVersion(any())).thenReturn(0L);
        when(service.isRevoked(any(), anyLong())).thenReturn(false);
        return service;
    }
}
