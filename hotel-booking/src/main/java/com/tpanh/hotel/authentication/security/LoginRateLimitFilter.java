package com.tpanh.hotel.authentication.security;

import com.tpanh.hotel.authentication.config.LoginSecurityProperties;
import com.tpanh.hotel.authentication.exception.AuthenticationRateLimitExceededException;
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
public class LoginRateLimitFilter extends OncePerRequestFilter {
    private final AuthenticationRateLimitService rateLimitService;
    private final LoginSecurityProperties properties;
    private final HandlerExceptionResolver exceptionResolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // Servlet paths are decoded by the container; raw requestURI can contain
        // encoded characters even when MVC resolves the same login endpoint.
        String path = request.getServletPath();
        if (path == null || path.isEmpty()) {
            path = request.getRequestURI();
        }
        if (HttpMethod.POST.matches(request.getMethod()) && path.endsWith("/auth/login")) {
            try {
                rateLimitService.check(
                        "login-ip",
                        request.getRemoteAddr(),
                        properties.ipLimit(),
                        Duration.ofSeconds(properties.ipWindowSeconds()));
            } catch (AuthenticationRateLimitExceededException exception) {
                exceptionResolver.resolveException(request, response, null, exception);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
