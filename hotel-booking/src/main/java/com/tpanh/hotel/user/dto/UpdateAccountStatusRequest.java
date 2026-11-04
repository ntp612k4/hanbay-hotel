package com.tpanh.hotel.user.dto;

import com.tpanh.hotel.authentication.domain.enums.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateAccountStatusRequest(@NotNull UserStatus status) {}
