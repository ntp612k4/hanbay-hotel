package com.tpanh.hotel.user.dto;

import com.tpanh.hotel.authentication.domain.enums.RoleCode;
import jakarta.validation.constraints.*;
import java.util.Set;

public record CreateAccountRequest(
        @NotBlank @Size(min = 2, max = 150) String fullName,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Pattern(regexp = "^[0-9]{9,15}$") String phone,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotNull @Size(max = 4) Set<@NotNull RoleCode> roles) {
    @Override
    public String toString() {
        return "CreateAccountRequest[password=<redacted>]";
    }
}
