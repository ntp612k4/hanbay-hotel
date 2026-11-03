package com.tpanh.hotel.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tpanh.hotel.authentication.config.LoginSecurityProperties;
import com.tpanh.hotel.authentication.dto.request.GuestRegisterRequest;
import com.tpanh.hotel.authentication.dto.response.GuestRegisterResponse;
import com.tpanh.hotel.authentication.exception.EmailAlreadyExistsException;
import com.tpanh.hotel.authentication.exception.PasswordMismatchException;
import com.tpanh.hotel.authentication.exception.PhoneAlreadyExistsException;
import com.tpanh.hotel.authentication.mapper.AuthMapper;
import com.tpanh.hotel.authentication.service.AuthenticationRateLimitService;
import com.tpanh.hotel.authentication.service.EmailService;
import com.tpanh.hotel.authentication.service.PasswordResetTokenService;
import com.tpanh.hotel.authentication.service.RefreshTokenService;
import com.tpanh.hotel.authentication.service.impl.AuthServiceImpl;
import com.tpanh.hotel.authentication.service.impl.JwtService;
import com.tpanh.hotel.common.config.JwtProperties;
import com.tpanh.hotel.user.domain.User;
import com.tpanh.hotel.user.repository.UserRepository;
import com.tpanh.hotel.user.service.UserService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String RAW_PASSWORD = "Password123";
    private static final String ENCODED_PASSWORD = "encoded-password";

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private PasswordResetTokenService passwordResetTokenService;

    @Mock
    private EmailService emailService;

    @Mock
    private AuthenticationRateLimitService rateLimitService;

    private JwtProperties jwtProperties;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        jwtProperties =
                new JwtProperties("hotel-booking-system", "test-secret-must-have-at-least-32-characters", 3600, 604800);
        authService = new AuthServiceImpl(
                userService,
                userRepository,
                passwordEncoder,
                Mappers.getMapper(AuthMapper.class),
                jwtService,
                refreshTokenService,
                jwtProperties,
                passwordResetTokenService,
                emailService,
                rateLimitService,
                new LoginSecurityProperties(30, 60, 5, 900));
    }

    @Test
    void registerGuestShouldCreateActiveCustomerUser() {
        GuestRegisterRequest request = validRequest();
        UUID userId = UUID.randomUUID();

        when(userRepository.existsByEmailIgnoreCase("customer@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("0901234567")).thenReturn(false);
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(userId);
            return user;
        });

        GuestRegisterResponse response = authService.registerGuest(request);

        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.fullName()).isEqualTo("Nguyen Van A");
        assertThat(response.email()).isEqualTo("customer@example.com");
        assertThat(response.phone()).isEqualTo("0901234567");
        assertThat(response.status()).isEqualTo("ACTIVE");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getPasswordHash()).isEqualTo(ENCODED_PASSWORD);
        assertThat(savedUser.getPasswordHash()).isNotEqualTo(RAW_PASSWORD);
        assertThat(savedUser.getRoles()).isEmpty();
    }

    @Test
    void registerGuestShouldThrowWhenPasswordMismatch() {
        GuestRegisterRequest request = new GuestRegisterRequest(
                "Nguyen Van A", "customer@example.com", "0901234567", RAW_PASSWORD, "Different123");

        assertThatThrownBy(() -> authService.registerGuest(request)).isInstanceOf(PasswordMismatchException.class);

        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void registerGuestShouldThrowWhenEmailAlreadyExists() {
        GuestRegisterRequest request = validRequest();

        when(userRepository.existsByEmailIgnoreCase("customer@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.registerGuest(request)).isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).existsByPhone(any());
        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void registerGuestShouldThrowWhenPhoneAlreadyExists() {
        GuestRegisterRequest request = validRequest();

        when(userRepository.existsByEmailIgnoreCase("customer@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("0901234567")).thenReturn(true);

        assertThatThrownBy(() -> authService.registerGuest(request)).isInstanceOf(PhoneAlreadyExistsException.class);

        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void registerGuestShouldMapEmailConstraintViolationToEmailAlreadyExists() {
        GuestRegisterRequest request = validRequest();

        when(userRepository.existsByEmailIgnoreCase("customer@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("0901234567")).thenReturn(false);
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "duplicate key value violates unique constraint \"uk_users_email\""));

        assertThatThrownBy(() -> authService.registerGuest(request)).isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void registerGuestShouldMapPhoneConstraintViolationToPhoneAlreadyExists() {
        GuestRegisterRequest request = validRequest();

        when(userRepository.existsByEmailIgnoreCase("customer@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("0901234567")).thenReturn(false);
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "duplicate key value violates unique constraint \"uk_users_phone\""));

        assertThatThrownBy(() -> authService.registerGuest(request)).isInstanceOf(PhoneAlreadyExistsException.class);
    }

    private GuestRegisterRequest validRequest() {
        return new GuestRegisterRequest(
                " Nguyen Van A ", " Customer@Example.COM ", " 0901234567 ", RAW_PASSWORD, RAW_PASSWORD);
    }
}
