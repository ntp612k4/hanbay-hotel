package com.tpanh.hotel.user.mapper;

import com.tpanh.hotel.authentication.domain.entity.Role;
import com.tpanh.hotel.common.mapper.MapperConfig;
import com.tpanh.hotel.user.domain.User;
import com.tpanh.hotel.user.dto.UserResponse;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.mapstruct.Mapper;

@Mapper(config = MapperConfig.class)
public interface UserMapper {

    UserResponse toResponse(User user);

    default List<String> mapRoles(Set<Role> roles) {
        if (roles == null) return List.of();
        return roles.stream()
                .map(Role::getCode)
                .map(Enum::name)
                .sorted(Comparator.naturalOrder())
                .toList();
    }
}
