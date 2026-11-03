package com.tpanh.hotel.authentication.service.impl;

import com.tpanh.hotel.authentication.config.LoginSecurityProperties;
import com.tpanh.hotel.authentication.domain.enums.UserStatus;
import com.tpanh.hotel.authentication.dto.request.ForgotPasswordRequest;
import com.tpanh.hotel.authentication.dto.request.GuestRegisterRequest;
import com.tpanh.hotel.authentication.dto.request.LoginRequest;
import com.tpanh.hotel.authentication.dto.request.ResetPasswordRequest;
import com.tpanh.hotel.authentication.dto.response.ForgotPasswordResponse;
import com.tpanh.hotel.authentication.dto.response.GuestRegisterResponse;
import com.tpanh.hotel.authentication.dto.response.LoginResponse;
import com.tpanh.hotel.authentication.dto.response.ResetPasswordResponse;
import com.tpanh.hotel.authentication.dto.response.ResetTokenVerificationResponse;
import com.tpanh.hotel.authentication.exception.EmailAlreadyExistsException;
import com.tpanh.hotel.authentication.exception.InvalidCredentialsException;
import com.tpanh.hotel.authentication.exception.PasswordMismatchException;
import com.tpanh.hotel.authentication.exception.PhoneAlreadyExistsException;
import com.tpanh.hotel.authentication.mapper.AuthMapper;
import com.tpanh.hotel.authentication.service.AuthService;
import com.tpanh.hotel.authentication.service.AuthenticationRateLimitService;
import com.tpanh.hotel.authentication.service.EmailService;
import com.tpanh.hotel.authentication.service.PasswordResetTokenService;
import com.tpanh.hotel.authentication.service.RefreshTokenService;
import com.tpanh.hotel.common.config.JwtProperties;
import com.tpanh.hotel.user.domain.User;
import com.tpanh.hotel.user.repository.UserRepository;
import com.tpanh.hotel.user.service.UserService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String TOKEN_TYPE = "Bearer";
    private static final String RESET_INSTRUCTIONS_ACCEPTED = "RESET_INSTRUCTIONS_ACCEPTED";
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final String LOGIN_ACCOUNT_IP_ACTION = "login-account-ip";
    private static final String DUMMY_PASSWORD_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthMapper authMapper;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final JwtProperties jwtProperties;
    private final PasswordResetTokenService passwordResetTokenService;
    private final EmailService emailService;
    private final AuthenticationRateLimitService rateLimitService;
    private final LoginSecurityProperties loginSecurityProperties;

    @Override
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        String normalizedEmail = request.email();
        userRepository
                .findByEmailIgnoreCase(normalizedEmail)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .ifPresent(user -> {
                    String rawToken = null;
                    try {
                        rawToken = passwordResetTokenService.issueToken(user);
                        emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), rawToken);
                    } catch (RuntimeException exception) {
                        if (rawToken != null) {
                            passwordResetTokenService.revokeToken(rawToken);
                        }
                        LOGGER.error("Unable to process password reset instructions", exception);
                    }
                });
        return new ForgotPasswordResponse(RESET_INSTRUCTIONS_ACCEPTED);
    }

    @Override
    public ResetTokenVerificationResponse verifyResetToken(String rawToken) {
        return passwordResetTokenService.verifyToken(rawToken);
    }

    @Override
    public ResetPasswordResponse resetPassword(ResetPasswordRequest request) {
        return passwordResetTokenService.resetPassword(request);
    }

    @Override
    public LoginResponse login(LoginRequest request, String clientIp) {
        String normalizedIdentifier = request.identifier().trim().toLowerCase(Locale.ROOT);
        String rateLimitIdentity = hashIdentifier(normalizedIdentifier + "\n" + clientIp);
        Duration accountWindow = Duration.ofSeconds(loginSecurityProperties.accountWindowSeconds());
        rateLimitService.ensureAllowed(
                LOGIN_ACCOUNT_IP_ACTION,
                rateLimitIdentity,
                loginSecurityProperties.accountFailureLimit(),
                accountWindow);

        User user = userService.findByIdentifier(normalizedIdentifier).orElse(null);
        if (user == null) {
            passwordEncoder.matches(request.password(), DUMMY_PASSWORD_HASH);
            recordLoginFailure(rateLimitIdentity, accountWindow);
            throw new InvalidCredentialsException();
        }

        boolean passwordMatches = passwordEncoder.matches(request.password(), user.getPasswordHash());
        if (user.getStatus() != UserStatus.ACTIVE || !passwordMatches) {
            recordLoginFailure(rateLimitIdentity, accountWindow);
            throw new InvalidCredentialsException();
        }

        rateLimitService.clear(LOGIN_ACCOUNT_IP_ACTION, rateLimitIdentity);
        List<String> roles = user.getRoles().stream()
                .map(role -> role.getCode().name())
                .sorted()
                .toList();

        String accessToken = jwtService.generateAccessToken(user);

        return new LoginResponse(
                accessToken,
                refreshTokenService.issueRefreshToken(user),
                TOKEN_TYPE,
                jwtProperties.accessTokenExpirationSeconds(),
                new LoginResponse.LoginUserView(
                        user.getId().toString(), user.getFullName(), user.getEmail(), user.getPhone(), roles));
    }

    private void recordLoginFailure(String identity, Duration window) {
        rateLimitService.check(
                LOGIN_ACCOUNT_IP_ACTION, identity, loginSecurityProperties.accountFailureLimit(), window);
    }

    private String hashIdentifier(String identifier) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256").digest(identifier.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    @Override
    @Transactional
    public GuestRegisterResponse registerGuest(GuestRegisterRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new PasswordMismatchException();
        }

        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }

        String normalizedPhone = request.phone().trim();
        if (userRepository.existsByPhone(normalizedPhone)) {
            throw new PhoneAlreadyExistsException(normalizedPhone);
        }

        User user = authMapper.toUser(request, passwordEncoder.encode(request.password()));
        try {
            return authMapper.toGuestRegisterResponse(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException exception) {
            throw mapUniqueConstraintViolation(exception, normalizedEmail, normalizedPhone);
        }
    }

    private RuntimeException mapUniqueConstraintViolation(
            DataIntegrityViolationException exception, String email, String phone) {
        String violation = (findConstraintName(exception) + " "
                        + exception.getMostSpecificCause().getMessage())
                .toLowerCase();
        if (violation.contains("email")) return new EmailAlreadyExistsException(email);
        if (violation.contains("phone")) return new PhoneAlreadyExistsException(phone);
        return exception;
    }

    private String findConstraintName(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ConstraintViolationException violation) {
                return violation.getConstraintName() == null ? "" : violation.getConstraintName();
            }
            current = current.getCause();
        }
        return "";
    }
}
