package com.tpanh.hotel.user.dto;

import com.tpanh.hotel.authentication.domain.enums.RoleCode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record UpdateAccountRolesRequest(@NotNull @Size(max = 4) Set<@NotNull RoleCode> roles) {}
