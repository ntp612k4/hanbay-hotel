package com.tpanh.hotel.user.service;

import com.tpanh.hotel.authentication.domain.entity.Role;
import com.tpanh.hotel.authentication.domain.enums.RoleCode;
import com.tpanh.hotel.authentication.domain.enums.UserStatus;
import com.tpanh.hotel.authentication.exception.EmailAlreadyExistsException;
import com.tpanh.hotel.authentication.exception.PhoneAlreadyExistsException;
import com.tpanh.hotel.authentication.repository.RefreshTokenRepository;
import com.tpanh.hotel.authentication.repository.RoleRepository;
import com.tpanh.hotel.authentication.service.AccessTokenRevocationService;
import com.tpanh.hotel.common.exception.RequestValidationException;
import com.tpanh.hotel.common.response.PageResponse;
import com.tpanh.hotel.common.security.CurrentUserProvider;
import com.tpanh.hotel.common.validation.PaginationValidator;
import com.tpanh.hotel.user.domain.AccountAccessAudit;
import com.tpanh.hotel.user.domain.User;
import com.tpanh.hotel.user.dto.CreateAccountRequest;
import com.tpanh.hotel.user.dto.UserResponse;
import com.tpanh.hotel.user.exception.UserNotFoundException;
import com.tpanh.hotel.user.mapper.UserMapper;
import com.tpanh.hotel.user.repository.AccountAccessAuditRepository;
import com.tpanh.hotel.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountManagementService {
    private final UserRepository users;
    private final RoleRepository roles;
    private final AccountAccessAuditRepository audits;
    private final RefreshTokenRepository refreshTokens;
    private final AccessTokenRevocationService accessTokens;
    private final CurrentUserProvider currentUser;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper mapper;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(int page, int size, String search, String role, UserStatus status) {
        requireAdmin();
        PaginationValidator.validate(page, size, 100);
        String term = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        if (term.length() > 150) throw new RequestValidationException("search", "Từ khóa tối đa 150 ký tự");
        RoleCode roleCode = null;
        boolean guestOnly = "GUEST".equals(role);
        if (role != null && !role.isBlank() && !guestOnly) {
            try {
                roleCode = RoleCode.valueOf(role);
            } catch (IllegalArgumentException exception) {
                throw new RequestValidationException("role", "Vai trò không hợp lệ");
            }
        }
        RoleCode selectedRole = roleCode;
        String pattern = "%" + term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        Specification<User> filter = (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (!term.isEmpty())
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), pattern, '\\'),
                        cb.like(cb.lower(root.get("email")), pattern, '\\'),
                        cb.like(root.get("phone"), pattern, '\\')));
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (guestOnly) predicates.add(cb.isEmpty(root.get("roles")));
            if (selectedRole != null) predicates.add(cb.equal(root.join("roles").get("code"), selectedRole));
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        return PageResponse.from(users.findAll(
                        filter, PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id"))))
                .map(mapper::toResponse));
    }

    @Transactional
    public UserResponse create(CreateAccountRequest request) {
        lockAdministration();
        User actor = requireAdmin();
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String phone = request.phone().trim();
        if (users.existsByEmailIgnoreCase(email)) throw new EmailAlreadyExistsException(email);
        if (users.existsByPhone(phone)) throw new PhoneAlreadyExistsException(phone);
        if (request.fullName().trim().length() < 2)
            throw new RequestValidationException("fullName", "Họ tên phải có ít nhất 2 ký tự");
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72)
            throw new RequestValidationException("password", "Mật khẩu không được vượt quá 72 byte UTF-8");
        User user = User.builder()
                .fullName(request.fullName().trim())
                .email(email)
                .phone(phone)
                .passwordHash(passwordEncoder.encode(request.password()))
                .status(UserStatus.ACTIVE)
                .roles(resolveRoles(request.roles()))
                .build();
        try {
            users.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            String detail = exception.getMostSpecificCause().getMessage().toLowerCase(Locale.ROOT);
            if (detail.contains("phone")) throw new PhoneAlreadyExistsException(phone);
            if (detail.contains("email")) throw new EmailAlreadyExistsException(email);
            throw exception;
        }
        audit(actor, user, "CREATE", null, null);
        return mapper.toResponse(user);
    }

    @Transactional
    public UserResponse updateRoles(UUID id, Set<RoleCode> requested) {
        lockAdministration();
        User actor = requireAdmin();
        User user = target(id);
        Set<Role> nextRoles = resolveRoles(requested);
        Set<RoleCode> previous = codes(user);
        if (previous.equals(requested)) return mapper.toResponse(user);
        if (previous.contains(RoleCode.ADMIN) && !requested.contains(RoleCode.ADMIN)) {
            protectLastAdmin(user);
            if (actor.getId().equals(id))
                throw new RequestValidationException("roles", "Bạn không thể tự gỡ vai trò quản trị viên");
        }
        String oldRoles = roleText(user);
        user.setRoles(nextRoles);
        users.saveAndFlush(user);
        audit(actor, user, "UPDATE_ROLES", oldRoles, user.getStatus());
        invalidateSessions(id);
        return mapper.toResponse(user);
    }

    @Transactional
    public UserResponse updateStatus(UUID id, UserStatus status) {
        lockAdministration();
        User actor = requireAdmin();
        User user = target(id);
        if (user.getStatus() == status) return mapper.toResponse(user);
        if (status != UserStatus.ACTIVE) {
            if (codes(user).contains(RoleCode.ADMIN)) protectLastAdmin(user);
            if (actor.getId().equals(id))
                throw new RequestValidationException("status", "Bạn không thể tự khóa hoặc vô hiệu hóa tài khoản");
        }
        UserStatus oldStatus = user.getStatus();
        user.setStatus(status);
        users.saveAndFlush(user);
        audit(actor, user, "UPDATE_STATUS", roleText(user), oldStatus);
        invalidateSessions(id);
        return mapper.toResponse(user);
    }

    private void lockAdministration() {
        // Serialize access mutations so concurrent demotions cannot remove all active admins.
        roles.findByCodeForUpdate(RoleCode.ADMIN)
                .orElseThrow(() -> new IllegalStateException("Thiếu vai trò ADMIN trong database"));
    }

    private User requireAdmin() {
        User actor = currentUser
                .currentUserId()
                .flatMap(users::findProfileById)
                .orElseThrow(() -> new AccessDeniedException("Không có quyền quản trị"));
        if (actor.getStatus() != UserStatus.ACTIVE || !codes(actor).contains(RoleCode.ADMIN))
            throw new AccessDeniedException("Không có quyền quản trị");
        return actor;
    }

    private User target(UUID id) {
        return users.findByIdForUpdate(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    private Set<Role> resolveRoles(Set<RoleCode> requested) {
        if (requested == null || requested.stream().anyMatch(Objects::isNull))
            throw new RequestValidationException("roles", "Danh sách vai trò không hợp lệ");
        Set<Role> result = new LinkedHashSet<>();
        for (RoleCode code : requested)
            result.add(roles.findByCode(code)
                    .orElseThrow(() -> new RequestValidationException("roles", "Vai trò chưa được cấu hình: " + code)));
        return result;
    }

    private void protectLastAdmin(User user) {
        if (user.getStatus() == UserStatus.ACTIVE && users.countActiveByRole(RoleCode.ADMIN, UserStatus.ACTIVE) <= 1)
            throw new RequestValidationException("roles", "Phải giữ ít nhất một quản trị viên đang hoạt động");
    }

    private Set<RoleCode> codes(User user) {
        Set<RoleCode> result = new HashSet<>();
        user.getRoles().forEach(role -> result.add(role.getCode()));
        return result;
    }

    private String roleText(User user) {
        return String.join(",", codes(user).stream().map(Enum::name).sorted().toList());
    }

    private void audit(User actor, User user, String action, String oldRoles, UserStatus oldStatus) {
        AccountAccessAudit entry = new AccountAccessAudit();
        entry.setActor(actor);
        entry.setUser(user);
        entry.setAction(action);
        entry.setOldRoles(oldRoles);
        entry.setNewRoles(roleText(user));
        entry.setOldStatus(oldStatus);
        entry.setNewStatus(user.getStatus());
        audits.save(entry);
    }

    private void invalidateSessions(UUID id) {
        refreshTokens.revokeActiveTokensByUserId(id, Instant.now(clock));
        // Fail the DB transaction when revocation is unavailable rather than retain old access.
        accessTokens.revokeAll(id);
    }
}
