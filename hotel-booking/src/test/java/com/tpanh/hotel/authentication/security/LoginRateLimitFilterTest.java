package com.tpanh.hotel.authentication.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tpanh.hotel.authentication.config.LoginSecurityProperties;
import com.tpanh.hotel.authentication.exception.AuthenticationRateLimitExceededException;
import com.tpanh.hotel.authentication.service.AuthenticationRateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.HandlerExceptionResolver;

class LoginRateLimitFilterTest {

    private final AuthenticationRateLimitService rateLimitService =
            org.mockito.Mockito.mock(AuthenticationRateLimitService.class);
    private final HandlerExceptionResolver exceptionResolver = org.mockito.Mockito.mock(HandlerExceptionResolver.class);
    private final LoginRateLimitFilter filter =
            new LoginRateLimitFilter(rateLimitService, new LoginSecurityProperties(30, 60, 5, 900), exceptionResolver);

    @Test
    void checksTheClientIpForLoginRequests() throws Exception {
        HttpServletRequest request = request("POST", "/api/v1/auth/login", "192.0.2.10");
        HttpServletResponse response = org.mockito.Mockito.mock(HttpServletResponse.class);
        FilterChain chain = org.mockito.Mockito.mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(rateLimitService).check("login-ip", "192.0.2.10", 30, Duration.ofSeconds(60));
        verify(chain).doFilter(request, response);
    }

    @Test
    void stopsTheFilterChainWhenTheIpLimitIsExceeded() throws Exception {
        HttpServletRequest request = request("POST", "/api/v1/auth/login", "192.0.2.10");
        HttpServletResponse response = org.mockito.Mockito.mock(HttpServletResponse.class);
        FilterChain chain = org.mockito.Mockito.mock(FilterChain.class);
        AuthenticationRateLimitExceededException exception = new AuthenticationRateLimitExceededException(42);
        org.mockito.Mockito.doThrow(exception)
                .when(rateLimitService)
                .check("login-ip", "192.0.2.10", 30, Duration.ofSeconds(60));

        filter.doFilter(request, response, chain);

        verify(exceptionResolver).resolveException(request, response, null, exception);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void limitsEncodedLoginPathsUsingTheContainerDecodedPath() throws Exception {
        var request = request("POST", "/api/v1/auth/log%69n", "192.0.2.10");
        when(request.getServletPath()).thenReturn("/api/v1/auth/login");
        var response = org.mockito.Mockito.mock(HttpServletResponse.class);
        var chain = org.mockito.Mockito.mock(FilterChain.class);
        filter.doFilter(request, response, chain);
        verify(rateLimitService).check("login-ip", "192.0.2.10", 30, Duration.ofSeconds(60));
    }

    private HttpServletRequest request(String method, String uri, String remoteAddress) {
        HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn(method);
        when(request.getRequestURI()).thenReturn(uri);
        when(request.getRemoteAddr()).thenReturn(remoteAddress);
        return request;
    }
}
