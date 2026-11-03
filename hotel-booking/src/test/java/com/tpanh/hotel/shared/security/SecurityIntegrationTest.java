package com.tpanh.hotel.shared.security;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tpanh.hotel.authentication.service.impl.JwtService;
import com.tpanh.hotel.authentication.service.impl.JwtService.ValidatedJwt;
import com.tpanh.hotel.common.config.SecurityConfig;
import com.tpanh.hotel.common.security.JwtAccessDeniedHandler;
import com.tpanh.hotel.common.security.JwtAuthenticationEntryPoint;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = SecurityTestController.class)
@ActiveProfiles("security-test")
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void loginEndpointShouldBePublic() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login"))
                .andExpect(status().isOk())
                .andExpect(content().string("public"));
    }

    @Test
    void anotherAuthEndpointShouldNotBePublicByWildcard() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));
    }

    @Test
    void protectedEndpointShouldReturnJsonUnauthorizedWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/test/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/v1/test/protected"));
    }

    @Test
    void unifiedAiChatRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/ai/chat/stream"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));

        when(jwtService.validate("valid-receptionist"))
                .thenReturn(Optional.of(new ValidatedJwt("receptionist-id", List.of("RECEPTIONIST"))));
        mockMvc.perform(post("/api/v1/ai/chat/stream").header("Authorization", "Bearer valid-receptionist"))
                .andExpect(status().isOk());
    }

    @Test
    void tamperedTokenShouldReturnUnauthorized() throws Exception {
        when(jwtService.validate("tampered")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/test/protected").header("Authorization", "Bearer tampered"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));
    }

    @Test
    void validGuestTokenShouldAuthenticateProtectedRequest() throws Exception {
        when(jwtService.validate("valid-receptionist"))
                .thenReturn(Optional.of(new ValidatedJwt("receptionist-id", List.of("RECEPTIONIST"))));

        mockMvc.perform(get("/api/v1/test/protected").header("Authorization", "Bearer valid-receptionist"))
                .andExpect(status().isOk())
                .andExpect(content().string("protected"));
    }

    @Test
    void authenticatedGuestShouldReceiveJsonForbiddenForManagerEndpoint() throws Exception {
        when(jwtService.validate("valid-receptionist"))
                .thenReturn(Optional.of(new ValidatedJwt("receptionist-id", List.of("RECEPTIONIST"))));

        mockMvc.perform(get("/api/v1/test/manager").header("Authorization", "Bearer valid-receptionist"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }
}
