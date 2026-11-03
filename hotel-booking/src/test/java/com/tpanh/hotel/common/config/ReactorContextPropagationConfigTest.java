package com.tpanh.hotel.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.tpanh.hotel.common.security.SecurityContextCurrentUserProvider;
import io.micrometer.context.ContextSnapshot;
import io.micrometer.context.ContextSnapshotFactory;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

class ReactorContextPropagationConfigTest {

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        new ReactorContextPropagationConfig().disable();
    }

    @Test
    void propagatesAuthenticatedUserToBoundedElasticToolThread() {
        ReactorContextPropagationConfig config = new ReactorContextPropagationConfig();
        config.enable();
        UUID userId = UUID.fromString("11111111-1111-4111-8111-111111111111");
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(userId.toString(), null, java.util.List.of()));
        ContextSnapshot snapshot = ContextSnapshotFactory.builder().build().captureAll();

        UUID propagatedUser = Mono.fromCallable(() ->
                        new SecurityContextCurrentUserProvider().currentUserId().orElse(null))
                .subscribeOn(Schedulers.boundedElastic())
                .contextWrite(snapshot::updateContext)
                .block();

        assertThat(propagatedUser).isEqualTo(userId);
    }
}
