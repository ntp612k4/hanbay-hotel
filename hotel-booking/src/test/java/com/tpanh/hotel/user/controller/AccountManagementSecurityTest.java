package com.tpanh.hotel.user.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.tpanh.hotel.common.config.SecurityConfig;
import com.tpanh.hotel.common.response.PageResponse;
import com.tpanh.hotel.user.service.AccountManagementService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AccountManagementController.class)
@Import({
    SecurityConfig.class,
    com.tpanh.hotel.common.security.JwtAuthenticationEntryPoint.class,
    com.tpanh.hotel.common.security.JwtAccessDeniedHandler.class
})
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173")
class AccountManagementSecurityTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    AccountManagementService service;

    @Test
    void anonymousCannotListAccounts() throws Exception {
        mvc.perform(get("/api/v1/admin/users")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MANAGER", "RECEPTIONIST", "HOUSEKEEPER"})
    void operationalRolesCannotGrantPrivileges(String role) throws Exception {
        mvc.perform(
                        patch("/api/v1/admin/users/00000000-0000-4000-8000-000000000001/roles")
                                .with(org.springframework.security.test.web.servlet.request
                                        .SecurityMockMvcRequestPostProcessors.user("staff")
                                        .roles(role))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"roles\":[\"ADMIN\"]}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanListAccounts() throws Exception {
        when(service.list(0, 10, null, null, null)).thenReturn(new PageResponse<>(List.of(), 0, 10, 0, 0, true, true));
        mvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void unknownRoleIsRejected() throws Exception {
        mvc.perform(patch("/api/v1/admin/users/00000000-0000-4000-8000-000000000001/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roles\":[\"SUPER_ADMIN\"]}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void missingRolesIsRejectedInsteadOfSilentlyRemovingThem() throws Exception {
        mvc.perform(patch("/api/v1/admin/users/00000000-0000-4000-8000-000000000001/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void emptyRolesCanRemoveStaffAccess() throws Exception {
        mvc.perform(patch("/api/v1/admin/users/00000000-0000-4000-8000-000000000001/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roles\":[]}"))
                .andExpect(status().isOk());
        verify(service).updateRoles(any(), eq(java.util.Set.of()));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void managerCannotListOrCreateOrLockAccounts() throws Exception {
        mvc.perform(get("/api/v1/admin/users")).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/admin/users/00000000-0000-4000-8000-000000000001/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"LOCKED\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
}
