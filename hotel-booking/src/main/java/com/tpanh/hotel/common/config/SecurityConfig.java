package com.tpanh.hotel.common.config;

import com.tpanh.hotel.authentication.config.LoginSecurityProperties;
import com.tpanh.hotel.authentication.config.PasswordResetProperties;
import com.tpanh.hotel.authentication.security.JwtAuthenticationFilter;
import com.tpanh.hotel.authentication.security.LoginRateLimitFilter;
import com.tpanh.hotel.authentication.security.PasswordResetRateLimitFilter;
import com.tpanh.hotel.authentication.service.AuthenticationRateLimitService;
import com.tpanh.hotel.authentication.service.impl.JwtService;
import com.tpanh.hotel.common.security.JwtAccessDeniedHandler;
import com.tpanh.hotel.common.security.JwtAuthenticationEntryPoint;
import jakarta.servlet.DispatcherType;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties({JwtProperties.class, PasswordResetProperties.class, LoginSecurityProperties.class})
public class SecurityConfig {

    private static final String[] SWAGGER_WHITELIST = {"/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"};

    @Bean
    @SuppressWarnings("java:S4502")
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ObjectProvider<JwtService> jwtServiceProvider,
            CorsConfigurationSource corsConfigurationSource,
            JwtAuthenticationEntryPoint authenticationEntryPoint,
            JwtAccessDeniedHandler accessDeniedHandler,
            ObjectProvider<AuthenticationRateLimitService> rateLimitServiceProvider,
            PasswordResetProperties passwordResetProperties,
            LoginSecurityProperties loginSecurityProperties,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
        http.cors(cors -> {})
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR)
                        .permitAll()
                        .requestMatchers(SWAGGER_WHITELIST)
                        .permitAll()
                        .requestMatchers(
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh-token",
                                "/api/v1/auth/forgot-password",
                                "/api/v1/auth/reset-password",
                                "/api/v1/auth/reset-password/verify")
                        .permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**")
                        .permitAll()
                        .requestMatchers("/actuator/**")
                        .hasRole("ADMIN")
                        .requestMatchers("/api/v1/auth/register")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/bookings")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/bookings/*/check-in")
                        .hasRole("RECEPTIONIST")
                        .requestMatchers(HttpMethod.GET, "/api/v1/bookings/*/available-rooms")
                        .hasRole("RECEPTIONIST")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/bookings/*/check-out")
                        .hasAnyRole("RECEPTIONIST", "MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/pricing/stay-price-calculations")
                        .permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/bookings/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/payments/requests")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/payments/*/status")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/media/**")
                        .permitAll()
                        .requestMatchers("/api/v1/room-types/**", "/api/v1/add-ons/**")
                        .permitAll()
                        .requestMatchers("/api/v1/payments/sepay/webhook")
                        .permitAll()
                        .requestMatchers("/api/v1/admin/**")
                        .hasRole("ADMIN")
                        .anyRequest()
                        .authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler));

        JwtService jwtService = jwtServiceProvider.getIfAvailable();
        if (jwtService != null) {
            http.addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);
        }
        AuthenticationRateLimitService rateLimitService = rateLimitServiceProvider.getIfAvailable();
        if (rateLimitService != null) {
            http.addFilterBefore(
                    new LoginRateLimitFilter(rateLimitService, loginSecurityProperties, exceptionResolver),
                    JwtAuthenticationFilter.class);
            http.addFilterBefore(
                    new PasswordResetRateLimitFilter(rateLimitService, passwordResetProperties, exceptionResolver),
                    JwtAuthenticationFilter.class);
        }

        return http.build();
    }

    @Bean
    public org.springframework.security.crypto.password.PasswordEncoder passwordEncoder() {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of(HttpHeaders.RETRY_AFTER));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
