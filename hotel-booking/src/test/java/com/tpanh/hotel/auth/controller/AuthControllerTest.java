package com.tpanh.hotel.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tpanh.hotel.authentication.controller.AuthController;
import com.tpanh.hotel.authentication.dto.request.GuestRegisterRequest;
import com.tpanh.hotel.authentication.dto.response.GuestRegisterResponse;
import com.tpanh.hotel.authentication.exception.EmailAlreadyExistsException;
import com.tpanh.hotel.authentication.service.AuthService;
import com.tpanh.hotel.authentication.service.RefreshTokenService;
import com.tpanh.hotel.common.exception.GlobalExceptionHandler;
import java.util.UUID;
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

    @Mock
    private AuthService authService;

    @Mock
    private RefreshTokenService refreshTokenService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService, refreshTokenService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addPlaceholderValue("api.prefix", "/api/v1")
                .build();
    }

    @Test
    void registerGuestShouldReturnCreatedResponse() throws Exception {
        UUID userId = UUID.randomUUID();
        GuestRegisterResponse response =
                new GuestRegisterResponse(userId, "Nguyen Van A", "customer@example.com", "0901234567", "ACTIVE");

        when(authService.registerGuest(any(GuestRegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đăng ký khách hàng thành công"))
                .andExpect(jsonPath("$.data.id").value(userId.toString()))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void registerGuestShouldReturnBadRequestWhenRequestInvalid() throws Exception {
        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "fullName": "A",
                                  "email": "invalid-email",
                                  "phone": "abc",
                                  "password": "123",
                                  "confirmPassword": "123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.fullName").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.phone").exists())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.confirmPassword").exists());
    }

    @Test
    void registerGuestShouldReturnConflictWhenEmailAlreadyExists() throws Exception {
        when(authService.registerGuest(any(GuestRegisterRequest.class)))
                .thenThrow(new EmailAlreadyExistsException("customer@example.com"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.errors.email").exists());
    }

    private String validRequestJson() {
        return """
                {
                  "fullName": "Nguyen Van A",
                  "email": "customer@example.com",
                  "phone": "0901234567",
                  "password": "Password123",
                  "confirmPassword": "Password123"
                }
                """;
    }
}
