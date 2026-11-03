package com.tpanh.hotel.features.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tpanh.hotel.common.security.CurrentUserProvider;
import com.tpanh.hotel.user.domain.User;
import com.tpanh.hotel.user.mapper.UserMapper;
import com.tpanh.hotel.user.repository.UserRepository;
import com.tpanh.hotel.user.service.UserService;
import com.tpanh.hotel.user.service.impl.UserServiceImpl;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private UserMapper userMapper;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, currentUserProvider, userMapper);
    }

    @Test
    void findByIdentifierShouldReturnUserFoundByEmail() {
        User user = new User();
        String identifier = "manager@example.com";
        when(userRepository.findByEmailIgnoreCase(identifier)).thenReturn(Optional.of(user));

        assertThat(userService.findByIdentifier(identifier)).contains(user);

        verify(userRepository).findByEmailIgnoreCase(identifier);
    }

    @Test
    void findByIdentifierShouldFallBackToPhone() {
        User user = new User();
        String identifier = "0900000001";
        when(userRepository.findByEmailIgnoreCase(identifier)).thenReturn(Optional.empty());
        when(userRepository.findByPhone(identifier)).thenReturn(Optional.of(user));

        assertThat(userService.findByIdentifier(identifier)).contains(user);

        verify(userRepository).findByPhone(identifier);
    }
}
