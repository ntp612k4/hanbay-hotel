package com.tpanh.hotel.authentication.mapper;

import com.tpanh.hotel.authentication.dto.request.GuestRegisterRequest;
import com.tpanh.hotel.authentication.dto.response.GuestRegisterResponse;
import com.tpanh.hotel.common.mapper.MapperConfig;
import com.tpanh.hotel.user.domain.User;
import java.util.LinkedHashSet;
import java.util.Locale;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(config = MapperConfig.class, imports = LinkedHashSet.class)
public interface AuthMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fullName", source = "request.fullName", qualifiedByName = "trim")
    @Mapping(target = "email", source = "request.email", qualifiedByName = "normalizeEmail")
    @Mapping(target = "phone", source = "request.phone", qualifiedByName = "trim")
    @Mapping(target = "passwordHash", source = "encodedPassword")
    @Mapping(target = "avatarUrl", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "roles", expression = "java(new LinkedHashSet<>())")
    User toUser(GuestRegisterRequest request, String encodedPassword);

    GuestRegisterResponse toGuestRegisterResponse(User user);

    @Named("trim")
    default String trim(String value) {
        return value == null ? null : value.trim();
    }

    @Named("normalizeEmail")
    default String normalizeEmail(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}
