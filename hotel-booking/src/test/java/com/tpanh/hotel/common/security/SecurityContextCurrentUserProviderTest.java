package com.tpanh.hotel.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityContextCurrentUserProviderTest {

    private final SecurityContextCurrentUserProvider provider = new SecurityContextCurrentUserProvider();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsAuthenticatedUserId() {
        UUID userId = UUID.randomUUID();
        SecurityContextHolder.getContext()
                .setAuthentication(
                        UsernamePasswordAuthenticationToken.authenticated(userId.toString(), null, List.of()));

        assertThat(provider.currentUserId()).contains(userId);
    }

    @Test
    void returnsEmptyWhenAuthenticationIsMissing() {
        assertThat(provider.currentUserId()).isEmpty();
    }

    @Test
    void returnsEmptyWhenSubjectIsNotUuid() {
        SecurityContextHolder.getContext()
                .setAuthentication(UsernamePasswordAuthenticationToken.authenticated("invalid", null, List.of()));

        assertThat(provider.currentUserId()).isEmpty();
    }
}
