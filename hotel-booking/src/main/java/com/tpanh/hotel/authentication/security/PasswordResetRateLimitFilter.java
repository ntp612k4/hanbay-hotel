package com.tpanh.hotel.authentication.security;

import com.tpanh.hotel.authentication.config.PasswordResetProperties;
import com.tpanh.hotel.authentication.service.AuthenticationRateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

@RequiredArgsConstructor
public class PasswordResetRateLimitFilter extends OncePerRequestFilter {

    private final AuthenticationRateLimitService rateLimitService;
    private final PasswordResetProperties properties;
    private final HandlerExceptionResolver exceptionResolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        int limit = resolveLimit(request.getMethod(), path);
        if (limit > 0) {
            try {
                rateLimitService.check(
                        path, request.getRemoteAddr(), limit, Duration.ofSeconds(properties.rateLimitWindowSeconds()));
            } catch (RuntimeException exception) {
                exceptionResolver.resolveException(request, response, null, exception);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private int resolveLimit(String method, String path) {
        if (HttpMethod.POST.matches(method) && path.endsWith("/auth/forgot-password")) {
            return properties.forgotRateLimit();
        }
        if (HttpMethod.POST.matches(method) && path.endsWith("/auth/reset-password/verify")) {
            return properties.verifyRateLimit();
        }
        if (HttpMethod.POST.matches(method) && path.endsWith("/auth/reset-password")) {
            return properties.submitRateLimit();
        }
        return 0;
    }
}
