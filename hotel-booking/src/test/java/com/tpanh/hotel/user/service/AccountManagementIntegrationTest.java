package com.tpanh.hotel.user.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.tpanh.hotel.authentication.domain.entity.RefreshToken;
import com.tpanh.hotel.authentication.domain.entity.Role;
import com.tpanh.hotel.authentication.domain.enums.RoleCode;
import com.tpanh.hotel.authentication.domain.enums.UserStatus;
import com.tpanh.hotel.authentication.repository.RefreshTokenRepository;
import com.tpanh.hotel.authentication.repository.RoleRepository;
import com.tpanh.hotel.authentication.service.AccessTokenRevocationService;
import com.tpanh.hotel.common.exception.RequestValidationException;
import com.tpanh.hotel.common.security.CurrentUserProvider;
import com.tpanh.hotel.user.domain.User;
import com.tpanh.hotel.user.mapper.UserMapper;
import com.tpanh.hotel.user.repository.AccountAccessAuditRepository;
import com.tpanh.hotel.user.repository.UserRepository;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DataJpaTest(
        showSql = false,
        properties = {"spring.jpa.hibernate.ddl-auto=validate", "debug=false"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({AccountManagementService.class, AccountManagementIntegrationTest.Config.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Testcontainers(disabledWithoutDocker = true)
class AccountManagementIntegrationTest {
    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("pgvector/pgvector:pg17");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.docker.compose.enabled", () -> false);
    }

    @Autowired
    AccountManagementService service;

    @Autowired
    UserRepository users;

    @Autowired
    RoleRepository roles;

    @Autowired
    AccountAccessAuditRepository audits;

    @Autowired
    RefreshTokenRepository refresh;

    @Autowired
    PlatformTransactionManager manager;

    @MockitoBean
    CurrentUserProvider current;

    @MockitoBean
    AccessTokenRevocationService access;

    final ThreadLocal<UUID> actor = new ThreadLocal<>();
    TransactionTemplate tx;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(manager);
        when(current.currentUserId()).thenAnswer(invocation -> Optional.ofNullable(actor.get()));
    }

    @AfterEach
    void clearActor() {
        actor.remove();
    }

    @Test
    void realDatabasePersistsRolesAuditAndRevokesRefreshToken() {
        User admin = createUser("Admin", RoleCode.ADMIN);
        User employee = createUser("Staff", RoleCode.RECEPTIONIST);
        UUID refreshId = tx.execute(status -> {
            RefreshToken token = new RefreshToken();
            token.setUser(users.findById(employee.getId()).orElseThrow());
            token.setTokenHash(UUID.randomUUID().toString());
            token.setCreatedAt(Instant.now());
            token.setExpiredAt(Instant.now().plusSeconds(3600));
            return refresh.saveAndFlush(token).getId();
        });
        actor.set(admin.getId());
        var response = service.updateRoles(employee.getId(), Set.of(RoleCode.MANAGER, RoleCode.HOUSEKEEPER));
        assertThat(response.roles()).containsExactly("HOUSEKEEPER", "MANAGER");
        assertThat(users.findProfileById(employee.getId()).orElseThrow().getRoles())
                .extracting(Role::getCode)
                .containsExactlyInAnyOrder(RoleCode.MANAGER, RoleCode.HOUSEKEEPER);
        assertThat(refresh.findById(refreshId).orElseThrow().getRevokedAt()).isNotNull();
        assertThat(audits.findAll())
                .anyMatch(entry -> entry.getAction().equals("UPDATE_ROLES")
                        && entry.getNewRoles().equals("HOUSEKEEPER,MANAGER"));
    }

    @Test
    void revocationFailureRollsBackDatabaseChangesAndAudit() {
        User admin = createUser("Admin", RoleCode.ADMIN);
        User employee = createUser("Staff", RoleCode.RECEPTIONIST);
        actor.set(admin.getId());
        long before = audits.count();
        doThrow(new IllegalStateException("Redis unavailable")).when(access).revokeAll(employee.getId());
        assertThatThrownBy(() -> service.updateStatus(employee.getId(), UserStatus.LOCKED))
                .isInstanceOf(IllegalStateException.class);
        assertThat(users.findById(employee.getId()).orElseThrow().getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(audits.count()).isEqualTo(before);
    }

    @Test
    void concurrentAdminsCannotDemoteEachOtherAndLoseAdministrativeAccess() throws Exception {
        User first = createUser("AdminA", RoleCode.ADMIN);
        User second = createUser("AdminB", RoleCode.ADMIN);
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<String> a = pool.submit(() -> demote(first.getId(), second.getId(), ready, start));
            Future<String> b = pool.submit(() -> demote(second.getId(), first.getId(), ready, start));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("SUCCESS", "REJECTED");
            long remaining = List.of(first.getId(), second.getId()).stream()
                    .map(id -> users.findProfileById(id).orElseThrow())
                    .filter(user -> user.getRoles().stream().anyMatch(role -> role.getCode() == RoleCode.ADMIN))
                    .count();
            assertThat(remaining).isEqualTo(1);
        } finally {
            start.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    void searchEscapesWildcardCharactersAndSupportsRoleAndStatus() {
        User admin = createUser("Admin", RoleCode.ADMIN);
        User staff = createUser("Literal%_Needle", RoleCode.HOUSEKEEPER);
        actor.set(admin.getId());
        var page = service.list(0, 10, "%_Needle", "HOUSEKEEPER", UserStatus.ACTIVE);
        assertThat(page.content()).extracting(item -> item.id()).containsExactly(staff.getId());
    }

    private String demote(UUID from, UUID to, CountDownLatch ready, CountDownLatch start) throws Exception {
        actor.set(from);
        ready.countDown();
        if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Concurrent start timed out");
        try {
            service.updateRoles(to, Set.of());
            return "SUCCESS";
        } catch (AccessDeniedException | RequestValidationException exception) {
            return "REJECTED";
        } finally {
            actor.remove();
        }
    }

    private User createUser(String name, RoleCode code) {
        return tx.execute(status -> users.saveAndFlush(User.builder()
                .fullName(name)
                .email(UUID.randomUUID() + "@example.com")
                .passwordHash("test-hash")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(roles.findByCode(code).orElseThrow())))
                .build()));
    }

    @TestConfiguration
    static class Config {
        @Bean
        Clock clock() {
            return Clock.systemUTC();
        }

        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }

        @Bean
        UserMapper userMapper() {
            return Mappers.getMapper(UserMapper.class);
        }
    }
}
