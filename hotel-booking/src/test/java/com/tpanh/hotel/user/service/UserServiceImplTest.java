package com.tpanh.hotel.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tpanh.hotel.authentication.domain.enums.UserStatus;
import com.tpanh.hotel.common.security.CurrentUserProvider;
import com.tpanh.hotel.user.domain.User;
import com.tpanh.hotel.user.exception.ProfileValidationException;
import com.tpanh.hotel.user.mapper.UserMapper;
import com.tpanh.hotel.user.repository.UserRepository;
import com.tpanh.hotel.user.service.impl.UserServiceImpl;
import com.tpanh.hotel.user.service.model.PatchField;
import com.tpanh.hotel.user.service.model.UpdateUserCommand;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final UUID USER_ID = UUID.fromString("96a8f221-c24d-4a52-a91d-b110fbde8c0e");

    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    private UserService service;

    @BeforeEach
    void setUp() {
        UserMapper mapper = Mappers.getMapper(UserMapper.class);
        service = new UserServiceImpl(userRepository, currentUserProvider, mapper);
        when(currentUserProvider.currentUserId()).thenReturn(Optional.of(USER_ID));
    }

    @Test
    void updateNormalizesEditableFields() {
        User user = activeUser("0900000000");
        when(userRepository.findProfileById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.existsByPhoneAndIdNot("+84912345678", USER_ID)).thenReturn(false);
        when(userRepository.saveAndFlush(user)).thenReturn(user);

        var result = service.updateCurrentProfile(
                new UpdateUserCommand(PatchField.of("  Nguyễn Văn B  "), PatchField.of("  +84912345678  ")));

        assertThat(result.fullName()).isEqualTo("Nguyễn Văn B");
        assertThat(result.phone()).isEqualTo("+84912345678");
    }

    @Test
    void updateFullNameDoesNotCheckDuplicateWhenCurrentPhoneIsNull() {
        User user = activeUser(null);
        when(userRepository.findProfileById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(user)).thenReturn(user);

        service.updateCurrentProfile(new UpdateUserCommand(PatchField.of("Nguyễn Văn B"), PatchField.absent()));

        verify(userRepository, never()).existsByPhoneAndIdNot(null, USER_ID);
    }

    @Test
    void unrelatedDatabaseViolationIsNotReportedAsDuplicatePhone() {
        User user = activeUser("0900000000");
        when(userRepository.findProfileById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.existsByPhoneAndIdNot("0911111111", USER_ID)).thenReturn(false);
        DataIntegrityViolationException databaseError = new DataIntegrityViolationException("constraint khác");
        when(userRepository.saveAndFlush(user)).thenThrow(databaseError);

        assertThatThrownBy(() -> service.updateCurrentProfile(
                        new UpdateUserCommand(PatchField.absent(), PatchField.of("0911111111"))))
                .isSameAs(databaseError);
    }

    @Test
    void updateRejectsBlankNormalizedValues() {
        when(userRepository.findProfileById(USER_ID)).thenReturn(Optional.of(activeUser("0900000000")));

        assertThatThrownBy(() ->
                        service.updateCurrentProfile(new UpdateUserCommand(PatchField.of("  "), PatchField.absent())))
                .isInstanceOf(ProfileValidationException.class);
    }

    private User activeUser(String phone) {
        User user = User.builder()
                .id(USER_ID)
                .email("guest@example.com")
                .fullName("Nguyễn Văn A")
                .phone(phone)
                .status(UserStatus.ACTIVE)
                .build();
        user.setCreatedAt(Instant.parse("2026-08-01T08:30:00Z"));
        user.setUpdatedAt(Instant.parse("2026-08-10T10:00:00Z"));
        return user;
    }
}
