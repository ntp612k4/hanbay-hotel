package com.tpanh.hotel.features.authentication.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.tpanh.hotel.authentication.domain.entity.Role;
import com.tpanh.hotel.authentication.domain.enums.RoleCode;
import com.tpanh.hotel.authentication.domain.enums.UserStatus;
import com.tpanh.hotel.authentication.repository.RoleRepository;
import com.tpanh.hotel.user.domain.User;
import com.tpanh.hotel.user.repository.UserRepository;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class UserRepositoryIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("pgvector/pgvector:pg17")
            .withDatabaseName("hotelbooking_test")
            .withUsername("hotelbooking_test")
            .withPassword("hotelbooking_test");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.docker.compose.enabled", () -> false);
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void shouldFindUserByCaseInsensitiveEmailAndFetchRoles() {
        Role managerRole = roleRepository.findByCode(RoleCode.MANAGER).orElseThrow();
        User user = createUser("Manager.Case@Example.com", "0900000011", managerRole);
        userRepository.saveAndFlush(user);

        User result =
                userRepository.findByEmailIgnoreCase("manager.case@example.com").orElseThrow();

        assertThat(result.getId()).isEqualTo(user.getId());
        assertThat(result.getRoles()).extracting(role -> role.getCode().name()).containsExactly("MANAGER");
    }

    @Test
    void shouldFindUserByPhoneAndFetchRoles() {
        Role receptionistRole = roleRepository.findByCode(RoleCode.RECEPTIONIST).orElseThrow();
        User user = createUser("phone@example.com", "0900000012", receptionistRole);
        userRepository.saveAndFlush(user);

        User result = userRepository.findByPhone("0900000012").orElseThrow();

        assertThat(result.getId()).isEqualTo(user.getId());
        assertThat(result.getRoles()).extracting(role -> role.getCode().name()).containsExactly("RECEPTIONIST");
    }

    @Test
    void shouldFetchMultipleRolesAssignedThroughUserRoles() {
        Role managerRole = roleRepository.findByCode(RoleCode.MANAGER).orElseThrow();
        Role housekeeperRole = roleRepository.findByCode(RoleCode.HOUSEKEEPER).orElseThrow();
        User user = createUser("multi-role@example.com", "0900000013", managerRole, housekeeperRole);
        userRepository.saveAndFlush(user);

        User result =
                userRepository.findByEmailIgnoreCase("multi-role@example.com").orElseThrow();

        assertThat(result.getRoles())
                .extracting(Role::getCode)
                .containsExactlyInAnyOrder(RoleCode.MANAGER, RoleCode.HOUSEKEEPER);
    }

    private User createUser(String email, String phone, Role... roles) {
        User user = new User();
        user.setEmail(email);
        user.setPhone(phone);
        user.setPasswordHash("$2a$10$test");
        user.setFullName("Repository Test User");
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(Set.of(roles));
        return user;
    }
}
