package com.tpanh.hotel.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tpanh.hotel.authentication.domain.enums.UserStatus;
import com.tpanh.hotel.common.exception.GlobalExceptionHandler;
import com.tpanh.hotel.user.dto.UserResponse;
import com.tpanh.hotel.user.service.UserService;
import com.tpanh.hotel.user.service.model.UpdateUserCommand;
import java.time.Instant;
import java.util.List;
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
class UserControllerTest {

    private static final UUID USER_ID = UUID.fromString("96a8f221-c24d-4a52-a91d-b110fbde8c0e");

    @Mock
    private UserService userProfileService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userProfileService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getCurrentProfileReturnsVietnameseSuccessResponse() throws Exception {
        when(userProfileService.getCurrentProfile()).thenReturn(profileResponse());

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Lấy thông tin cá nhân thành công"))
                .andExpect(jsonPath("$.data.id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.data.roles[0]").value("GUEST"));
    }

    @Test
    void updateCurrentProfileRejectsUnsupportedFieldWithCommonValidationCode() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"other@example.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.email").value("Trường dữ liệu không được hỗ trợ"));
    }

    @Test
    void updateCurrentProfileReturnsUpdatedProfile() throws Exception {
        when(userProfileService.updateCurrentProfile(any(UpdateUserCommand.class)))
                .thenReturn(profileResponse());

        mockMvc.perform(patch("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Nguyễn Văn A\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cập nhật thông tin cá nhân thành công"));
    }

    private UserResponse profileResponse() {
        return new UserResponse(
                USER_ID,
                "guest@example.com",
                "Nguyễn Văn A",
                "0900000000",
                UserStatus.ACTIVE,
                List.of("GUEST"),
                Instant.parse("2026-08-01T08:30:00Z"),
                Instant.parse("2026-08-10T10:00:00Z"));
    }
}
