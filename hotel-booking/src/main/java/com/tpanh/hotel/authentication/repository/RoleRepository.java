package com.tpanh.hotel.authentication.repository;

import com.tpanh.hotel.authentication.domain.entity.Role;
import com.tpanh.hotel.authentication.domain.enums.RoleCode;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByCode(RoleCode code);
}
