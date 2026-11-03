package com.tpanh.hotel.features.authentication.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tpanh.hotel.authentication.controller.AuthController;
import com.tpanh.hotel.authentication.dto.response.ForgotPasswordResponse;
import com.tpanh.hotel.authentication.dto.response.LoginResponse;
import com.tpanh.hotel.authentication.dto.response.RefreshTokenResponse;
import com.tpanh.hotel.authentication.dto.response.ResetPasswordResponse;
import com.tpanh.hotel.authentication.dto.response.ResetTokenVerificationResponse;
import com.tpanh.hotel.authentication.exception.InvalidCredentialsException;
import com.tpanh.hotel.authentication.exception.InvalidPasswordResetTokenException;
import com.tpanh.hotel.authentication.exception.PasswordMismatchException;
import com.tpanh.hotel.authentication.exception.PasswordResetTokenExpiredException;
import com.tpanh.hotel.authentication.exception.PasswordResetTokenUsedException;
import com.tpanh.hotel.authentication.service.AuthService;
import com.tpanh.hotel.authentication.service.RefreshTokenService;
import com.tpanh.hotel.common.exception.GlobalExceptionHandler;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthService authService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        AuthController authController = new AuthController(authService, refreshTokenService);
        this.mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .addPlaceholderValue("api.prefix", "/api/v1")
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void loginShouldReturnSuccessResponse() throws Exception {
        LoginResponse.LoginUserView userView = new LoginResponse.LoginUserView(
                "5d0356fe-25da-4a4b-bdc7-6fe74e90d609",
                "Quản trị viên",
                "admin@aegean-suites.com",
                "0901234567",
                java.util.List.of("ADMIN"));
        LoginResponse loginResponse = new LoginResponse("jwt-token", "refresh-token", "Bearer", 3600, userView);

        when(authService.login(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(loginResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginPayload("admin@aegean-suites.com", "Admin@123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đăng nhập thành công"))
                .andExpect(jsonPath("$.data.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(3600))
                .andExpect(jsonPath("$.data.user.roles[0]").value("ADMIN"));
    }

    @Test
    void forgotPasswordShouldReturnGenericSuccessResponse() throws Exception {
        when(authService.forgotPassword(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new ForgotPasswordResponse("RESET_INSTRUCTIONS_ACCEPTED"));

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\" Guest@Example.COM \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Nếu email tồn tại, hướng dẫn đặt lại mật khẩu sẽ được gửi"))
                .andExpect(jsonPath("$.data.status").value("RESET_INSTRUCTIONS_ACCEPTED"));
    }

    @Test
    void forgotPasswordShouldRejectInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    void forgotPasswordShouldRejectEmptyEmail() throws Exception {
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    void verifyResetTokenShouldReturnValidStatus() throws Exception {
        String token = "abcdefghijklmnopqrstuvwxyz123456";
        when(authService.verifyResetToken(token))
                .thenReturn(
                        new ResetTokenVerificationResponse("RESET_TOKEN_VALID", Instant.parse("2026-08-10T06:15:00Z")));

        mockMvc.perform(post("/api/v1/auth/reset-password/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESET_TOKEN_VALID"))
                .andExpect(jsonPath("$.data.expiresAt").value("2026-08-10T06:15:00Z"));
    }

    @Test
    void verifyResetTokenShouldReturnContractErrorWhenTokenIsInvalid() throws Exception {
        String token = "abcdefghijklmnopqrstuvwxyz123456";
        when(authService.verifyResetToken(token)).thenThrow(new InvalidPasswordResetTokenException());

        mockMvc.perform(post("/api/v1/auth/reset-password/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("AUTH_RESET_TOKEN_INVALID"));
    }

    @Test
    void verifyResetTokenShouldRejectMissingToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/reset-password/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.token").exists());
    }

    @Test
    void verifyResetTokenShouldReturnExpiredContractError() throws Exception {
        String token = "abcdefghijklmnopqrstuvwxyz123456";
        when(authService.verifyResetToken(token)).thenThrow(new PasswordResetTokenExpiredException());

        mockMvc.perform(post("/api/v1/auth/reset-password/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("AUTH_RESET_TOKEN_EXPIRED"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(token))))
                .andExpect(content()
                        .string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("stackTrace"))));
    }

    @Test
    void verifyResetTokenShouldReturnUsedContractError() throws Exception {
        String token = "abcdefghijklmnopqrstuvwxyz123456";
        when(authService.verifyResetToken(token)).thenThrow(new PasswordResetTokenUsedException());

        mockMvc.perform(post("/api/v1/auth/reset-password/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("AUTH_RESET_TOKEN_USED"));
    }

    @Test
    void resetPasswordShouldReturnSuccessResponse() throws Exception {
        when(authService.resetPassword(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new ResetPasswordResponse("PASSWORD_RESET"));

        mockMvc.perform(
                        post("/api/v1/auth/reset-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "token": "abcdefghijklmnopqrstuvwxyz123456",
                                  "newPassword": "NewPassword123",
                                  "confirmPassword": "NewPassword123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PASSWORD_RESET"));
    }

    @Test
    void resetPasswordShouldRejectPasswordPolicyViolation() throws Exception {
        mockMvc.perform(
                        post("/api/v1/auth/reset-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "token": "abcdefghijklmnopqrstuvwxyz123456",
                                  "newPassword": "short",
                                  "confirmPassword": "short"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.newPassword").exists());
    }

    @Test
    void resetPasswordShouldRejectMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.token").exists())
                .andExpect(jsonPath("$.errors.newPassword").exists())
                .andExpect(jsonPath("$.errors.confirmPassword").exists());
    }

    @Test
    void resetPasswordShouldReturnInvalidTokenContractWithoutSensitiveData() throws Exception {
        when(authService.resetPassword(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new InvalidPasswordResetTokenException());

        String payload =
                """
                {
                  "token": "abcdefghijklmnopqrstuvwxyz123456",
                  "newPassword": "NewPassword123",
                  "confirmPassword": "NewPassword123"
                }
                """;
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("AUTH_RESET_TOKEN_INVALID"))
                .andExpect(content()
                        .string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("NewPassword123"))))
                .andExpect(content()
                        .string(org.hamcrest.Matchers.not(
                                org.hamcrest.Matchers.containsString("abcdefghijklmnopqrstuvwxyz123456"))))
                .andExpect(content()
                        .string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("stackTrace"))));
    }

    @Test
    void resetPasswordShouldReturnPasswordMismatchContract() throws Exception {
        when(authService.resetPassword(org.mockito.ArgumentMatchers.any())).thenThrow(new PasswordMismatchException());

        mockMvc.perform(
                        post("/api/v1/auth/reset-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "token": "abcdefghijklmnopqrstuvwxyz123456",
                                  "newPassword": "NewPassword123",
                                  "confirmPassword": "DifferentPassword123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PASSWORD_MISMATCH"))
                .andExpect(jsonPath("$.errors.confirmPassword").exists());
    }

    @Test
    void loginShouldReturnBadRequestWhenPayloadInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginPayload("", "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Dữ liệu gửi lên không hợp lệ"));
    }

    @Test
    void loginShouldExposeRemainingCooldown() throws Exception {
        when(authService.login(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString()))
                .thenThrow(new com.tpanh.hotel.authentication.exception.AuthenticationRateLimitExceededException(42));
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"guest@example.com\",\"password\":\"fixture-login-value\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Retry-After", "42"))
                .andExpect(jsonPath("$.code").value("AUTH_RATE_LIMITED"));
    }

    @Test
    void loginShouldReturnUnauthorizedWhenCredentialsInvalid() throws Exception {
        when(authService.login(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString()))
                .thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginPayload("admin@aegean-suites.com", "Wrong@123"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Email, số điện thoại hoặc mật khẩu không đúng"));
    }

    @Test
    void loginShouldReturnBadRequestWhenIdentifierIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginPayload(null, "Admin@123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.identifier").exists());
    }

    @Test
    void loginShouldReturnBadRequestWhenPasswordIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginPayload("admin@aegean-suites.com", null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void refreshTokenShouldReturnRotatedTokenPair() throws Exception {
        when(refreshTokenService.refreshAccessToken(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new RefreshTokenResponse("new-access-token", "new-refresh-token", "Bearer", 900));

        mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"current-refresh-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("new-refresh-token"))
                .andExpect(jsonPath("$.data.expiresIn").value(900));
    }

    private record LoginPayload(String identifier, String password) {}
}
