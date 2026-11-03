package com.tpanh.hotel.user.service.impl;

import com.tpanh.hotel.authentication.domain.enums.UserStatus;
import com.tpanh.hotel.authentication.exception.PhoneAlreadyExistsException;
import com.tpanh.hotel.common.security.CurrentUserProvider;
import com.tpanh.hotel.user.domain.User;
import com.tpanh.hotel.user.dto.UserResponse;
import com.tpanh.hotel.user.exception.ProfileValidationException;
import com.tpanh.hotel.user.exception.UserAccountNotActiveException;
import com.tpanh.hotel.user.exception.UserNotFoundException;
import com.tpanh.hotel.user.mapper.UserMapper;
import com.tpanh.hotel.user.repository.UserRepository;
import com.tpanh.hotel.user.service.UserService;
import com.tpanh.hotel.user.service.model.PatchField;
import com.tpanh.hotel.user.service.model.UpdateUserCommand;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final int MIN_FULL_NAME_LENGTH = 2;
    private static final int MAX_FULL_NAME_LENGTH = 100;
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{8,15}$");
    private static final String PHONE_UNIQUE_CONSTRAINT = "uq_users_phone_not_null";

    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByIdentifier(String identifier) {
        return userRepository.findByEmailIgnoreCase(identifier).or(() -> userRepository.findByPhone(identifier));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentProfile() {
        return userMapper.toResponse(getActiveUser());
    }

    @Override
    @Transactional
    public UserResponse updateCurrentProfile(UpdateUserCommand command) {
        Objects.requireNonNull(command, "Lệnh cập nhật thông tin cá nhân không được để trống");

        User user = getActiveUser();
        Map<String, String> errors = new LinkedHashMap<>();
        if (!command.fullName().present() && !command.phone().present()) {
            errors.put("request", "Phải cung cấp ít nhất một trong hai trường fullName hoặc phone");
        }

        String fullName = resolveFullName(command.fullName(), user.getFullName(), errors);
        String phone = resolvePhone(command.phone(), user.getPhone(), errors);
        if (!errors.isEmpty()) throw new ProfileValidationException(errors);
        if (phone != null && userRepository.existsByPhoneAndIdNot(phone, user.getId())) {
            throw new PhoneAlreadyExistsException(phone);
        }

        user.setFullName(fullName);
        user.setPhone(phone);
        try {
            return userMapper.toResponse(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException exception) {
            if (isPhoneUniqueConstraintViolation(exception)) throw new PhoneAlreadyExistsException(phone);
            throw exception;
        }
    }

    private User getActiveUser() {
        UUID userId = currentUserProvider
                .currentUserId()
                .orElseThrow(() -> new InsufficientAuthenticationException("Phiên đăng nhập không hợp lệ"));
        User user = userRepository.findProfileById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        if (user.getStatus() != UserStatus.ACTIVE) throw new UserAccountNotActiveException();
        return user;
    }

    private String resolveFullName(PatchField<String> field, String currentValue, Map<String, String> errors) {
        if (!field.present()) return currentValue;
        if (field.value() == null) {
            errors.put("fullName", "Họ và tên không được để trống");
            return currentValue;
        }
        String value = field.value().trim();
        if (value.length() < MIN_FULL_NAME_LENGTH || value.length() > MAX_FULL_NAME_LENGTH) {
            errors.put("fullName", "Họ và tên phải có từ 2 đến 100 ký tự");
        }
        return value;
    }

    private String resolvePhone(PatchField<String> field, String currentValue, Map<String, String> errors) {
        if (!field.present()) return currentValue;
        if (field.value() == null) {
            errors.put("phone", "Số điện thoại không được để trống");
            return currentValue;
        }
        String value = field.value().trim();
        if (!PHONE_PATTERN.matcher(value).matches()) {
            errors.put("phone", "Số điện thoại phải có từ 8 đến 15 chữ số và có thể bắt đầu bằng dấu +");
        }
        return value;
    }

    private boolean isPhoneUniqueConstraintViolation(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof org.hibernate.exception.ConstraintViolationException constraintViolation
                    && PHONE_UNIQUE_CONSTRAINT.equalsIgnoreCase(constraintViolation.getConstraintName())) return true;
            current = current.getCause();
        }
        return false;
    }
}
