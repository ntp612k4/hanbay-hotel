package com.tpanh.hotel.user.repository;

import com.tpanh.hotel.user.domain.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    @EntityGraph(attributePaths = "roles")
    Optional<User> findByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = "roles")
    Optional<User> findByPhone(String phone);

    @EntityGraph(attributePaths = "roles")
    Optional<User> findProfileById(UUID id);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhone(String phone);

    boolean existsByPhoneAndIdNot(String phone, UUID id);
}
