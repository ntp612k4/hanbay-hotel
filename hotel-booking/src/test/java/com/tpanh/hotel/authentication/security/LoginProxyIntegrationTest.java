package com.tpanh.hotel.authentication.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tpanh.hotel.authentication.exception.InvalidCredentialsException;
import com.tpanh.hotel.authentication.service.AuthService;
import com.tpanh.hotel.authentication.service.AuthenticationRateLimitService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "server.tomcat.remoteip.internal-proxies=192[.]0[.]2[.]10")
@Testcontainers(disabledWithoutDocker = true)
class LoginProxyIntegrationTest {
    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("pgvector/pgvector:pg17");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Value("${local.server.port}")
    int port;

    @MockitoBean
    AuthService authService;

    @MockitoBean
    AuthenticationRateLimitService rateLimitService;

    @Test
    void doesNotTrustForwardingHeadersFromADirectUntrustedClient() throws Exception {
        when(authService.login(any(), anyString())).thenThrow(new InvalidCredentialsException());
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/auth/login"))
                .header("Content-Type", "application/json")
                .header("Forwarded", "for=198.51.100.7;proto=https")
                .header("X-Forwarded-For", "198.51.100.8")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"identifier\":\"guest@example.com\",\"password\":\"fixture-login-value\"}"))
                .build();
        var client = HttpClient.newHttpClient();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(401);
        var address = ArgumentCaptor.forClass(String.class);
        verify(authService).login(any(), address.capture());
        assertThat(address.getValue()).isEqualTo("127.0.0.1");
        verify(rateLimitService).check(eq("login-ip"), eq("127.0.0.1"), org.mockito.ArgumentMatchers.anyInt(), any());
    }
}
