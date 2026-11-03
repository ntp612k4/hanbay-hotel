package com.tpanh.hotel.user.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tpanh.hotel.authentication.domain.enums.UserStatus;
import com.tpanh.hotel.authentication.service.impl.JwtService;
import com.tpanh.hotel.authentication.service.impl.JwtService.ValidatedJwt;
import com.tpanh.hotel.common.config.SecurityConfig;
import com.tpanh.hotel.common.security.JwtAccessDeniedHandler;
import com.tpanh.hotel.common.security.JwtAuthenticationEntryPoint;
import com.tpanh.hotel.user.dto.UserResponse;
import com.tpanh.hotel.user.service.UserService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = UserController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173")
class UserSecurityIntegrationTest {

    private static final UUID USER_ID = UUID.fromString("96a8f221-c24d-4a52-a91d-b110fbde8c0e");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserService userProfileService;

    @Test
    void anonymousRequestCannotReadProfile() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Vui lòng đăng nhập để tiếp tục"));
    }

    @Test
    void validGuestTokenCanReadCurrentProfile() throws Exception {
        when(jwtService.validate("valid-guest"))
                .thenReturn(Optional.of(new ValidatedJwt(USER_ID.toString(), List.of())));
        when(userProfileService.getCurrentProfile()).thenReturn(profileResponse());

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer valid-guest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.data.roles").isEmpty());
    }

    private UserResponse profileResponse() {
        return new UserResponse(
                USER_ID,
                "guest@example.com",
                "Nguyễn Văn A",
                "0901234567",
                UserStatus.ACTIVE,
                List.of(),
                Instant.parse("2026-08-01T08:30:00Z"),
                Instant.parse("2026-08-10T10:00:00Z"));
    }
}
