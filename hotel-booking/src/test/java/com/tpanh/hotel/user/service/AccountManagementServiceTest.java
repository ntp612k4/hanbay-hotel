package com.tpanh.hotel.user.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.tpanh.hotel.authentication.domain.entity.Role;
import com.tpanh.hotel.authentication.domain.enums.RoleCode;
import com.tpanh.hotel.authentication.domain.enums.UserStatus;
import com.tpanh.hotel.authentication.repository.RefreshTokenRepository;
import com.tpanh.hotel.authentication.repository.RoleRepository;
import com.tpanh.hotel.authentication.service.AccessTokenRevocationService;
import com.tpanh.hotel.common.exception.RequestValidationException;
import com.tpanh.hotel.common.security.CurrentUserProvider;
import com.tpanh.hotel.user.domain.AccountAccessAudit;
import com.tpanh.hotel.user.domain.User;
import com.tpanh.hotel.user.dto.CreateAccountRequest;
import com.tpanh.hotel.user.mapper.UserMapper;
import com.tpanh.hotel.user.repository.AccountAccessAuditRepository;
import com.tpanh.hotel.user.repository.UserRepository;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AccountManagementServiceTest {
    @Mock
    UserRepository users;

    @Mock
    RoleRepository roles;

    @Mock
    AccountAccessAuditRepository audits;

    @Mock
    RefreshTokenRepository refresh;

    @Mock
    AccessTokenRevocationService access;

    @Mock
    CurrentUserProvider current;

    @Mock
    PasswordEncoder passwords;

    @Mock
    UserMapper mapper;

    AccountManagementService service;
    User admin;
    User employee;
    static final Instant NOW = Instant.parse("2026-11-03T17:00:00Z");

    @BeforeEach
    void setUp() {
        service = new AccountManagementService(
                users, roles, audits, refresh, access, current, passwords, mapper, Clock.fixed(NOW, ZoneOffset.UTC));
        admin = user(RoleCode.ADMIN);
        employee = user(RoleCode.RECEPTIONIST);
        lenient().when(current.currentUserId()).thenReturn(Optional.of(admin.getId()));
        lenient().when(users.findProfileById(admin.getId())).thenReturn(Optional.of(admin));
        lenient().when(users.findByIdForUpdate(employee.getId())).thenReturn(Optional.of(employee));
        lenient().when(users.findByIdForUpdate(admin.getId())).thenReturn(Optional.of(admin));
        lenient().when(roles.findByCodeForUpdate(RoleCode.ADMIN)).thenReturn(Optional.of(role(RoleCode.ADMIN)));
        for (RoleCode code : RoleCode.values())
            lenient().when(roles.findByCode(code)).thenReturn(Optional.of(role(code)));
    }

    @Test
    void grantsMultipleRolesAndRevokesAllSessionsWithAudit() {
        service.updateRoles(employee.getId(), Set.of(RoleCode.MANAGER, RoleCode.HOUSEKEEPER));
        assertThat(employee.getRoles())
                .extracting(Role::getCode)
                .containsExactlyInAnyOrder(RoleCode.MANAGER, RoleCode.HOUSEKEEPER);
        verify(refresh).revokeActiveTokensByUserId(employee.getId(), NOW);
        verify(access).revokeAll(employee.getId());
        ArgumentCaptor<AccountAccessAudit> entry = ArgumentCaptor.forClass(AccountAccessAudit.class);
        verify(audits).save(entry.capture());
        assertThat(entry.getValue().getOldRoles()).isEqualTo("RECEPTIONIST");
        assertThat(entry.getValue().getNewRoles()).isEqualTo("HOUSEKEEPER,MANAGER");
        assertThat(entry.getValue().getActor()).isSameAs(admin);
    }

    @Test
    void removingAllStaffRolesReturnsAccountToGuest() {
        service.updateRoles(employee.getId(), Set.of());
        assertThat(employee.getRoles()).isEmpty();
        verify(access).revokeAll(employee.getId());
    }

    @Test
    void unchangedRolesDoNotInvalidateSessions() {
        service.updateRoles(employee.getId(), Set.of(RoleCode.RECEPTIONIST));
        verifyNoInteractions(access, refresh, audits);
    }

    @Test
    void protectsLastActiveAdministrator() {
        when(users.countActiveByRole(RoleCode.ADMIN, UserStatus.ACTIVE)).thenReturn(1L);
        assertThatThrownBy(() -> service.updateRoles(admin.getId(), Set.of()))
                .isInstanceOf(RequestValidationException.class)
                .hasMessageContaining("ít nhất một quản trị viên");
        verifyNoInteractions(access, refresh, audits);
    }

    @Test
    void administratorCannotDemoteThemselfEvenWithAnotherAdmin() {
        when(users.countActiveByRole(RoleCode.ADMIN, UserStatus.ACTIVE)).thenReturn(2L);
        assertThatThrownBy(() -> service.updateRoles(admin.getId(), Set.of()))
                .isInstanceOf(RequestValidationException.class)
                .hasMessageContaining("tự gỡ");
    }

    @Test
    void administratorCannotLockThemself() {
        when(users.countActiveByRole(RoleCode.ADMIN, UserStatus.ACTIVE)).thenReturn(2L);
        assertThatThrownBy(() -> service.updateStatus(admin.getId(), UserStatus.LOCKED))
                .isInstanceOf(RequestValidationException.class);
        verifyNoInteractions(access, refresh, audits);
    }

    @Test
    void lockingAnEmployeeRevokesSessions() {
        service.updateStatus(employee.getId(), UserStatus.LOCKED);
        assertThat(employee.getStatus()).isEqualTo(UserStatus.LOCKED);
        verify(refresh).revokeActiveTokensByUserId(employee.getId(), NOW);
        verify(access).revokeAll(employee.getId());
    }

    @Test
    void unchangedStatusDoesNotInvalidateSessions() {
        service.updateStatus(employee.getId(), UserStatus.ACTIVE);
        verifyNoInteractions(access, refresh, audits);
    }

    @Test
    void staleAdminRoleCannotAuthorizeAnAccessChange() {
        admin.setRoles(Set.of(role(RoleCode.MANAGER)));
        assertThatThrownBy(() -> service.updateRoles(employee.getId(), Set.of(RoleCode.ADMIN)))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(access, refresh, audits);
    }

    @Test
    void unavailableRevocationFailsTheMutation() {
        doThrow(new IllegalStateException("Redis unavailable")).when(access).revokeAll(employee.getId());
        assertThatThrownBy(() -> service.updateStatus(employee.getId(), UserStatus.LOCKED))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void createHashesPasswordAndRecordsAdmin() {
        when(passwords.encode("Strong123!")).thenReturn("hashed-password");
        service.create(new CreateAccountRequest(
                "  Trần Mai Anh  ", "ANH@example.com", "0901234567", "Strong123!", Set.of(RoleCode.HOUSEKEEPER)));
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("anh@example.com");
        assertThat(saved.getValue().getFullName()).isEqualTo("Trần Mai Anh");
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("hashed-password");
        verify(audits).save(any(AccountAccessAudit.class));
    }

    @Test
    void createRejectsDuplicateEmailBeforeSaving() {
        when(users.existsByEmailIgnoreCase("anh@example.com")).thenReturn(true);
        assertThatThrownBy(() -> service.create(
                        new CreateAccountRequest("Mai Anh", "anh@example.com", "0901234567", "Strong123!", Set.of())))
                .isInstanceOf(com.tpanh.hotel.authentication.exception.EmailAlreadyExistsException.class);
        verify(users, never()).saveAndFlush(any());
    }

    private static User user(RoleCode code) {
        return User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(role(code))))
                .build();
    }

    private static Role role(RoleCode code) {
        Role role = new Role();
        role.setCode(code);
        return role;
    }
}
