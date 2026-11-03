package com.tpanh.hotel.user.service;

import com.tpanh.hotel.user.domain.User;
import com.tpanh.hotel.user.dto.UserResponse;
import com.tpanh.hotel.user.service.model.UpdateUserCommand;
import java.util.Optional;

public interface UserService {

    Optional<User> findByIdentifier(String identifier);

    UserResponse getCurrentProfile();

    UserResponse updateCurrentProfile(UpdateUserCommand command);
}
