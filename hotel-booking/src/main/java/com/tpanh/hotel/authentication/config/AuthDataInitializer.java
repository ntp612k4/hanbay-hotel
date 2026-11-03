package com.tpanh.hotel.authentication.config;

import com.tpanh.hotel.authentication.domain.entity.Role;
import com.tpanh.hotel.authentication.domain.enums.RoleCode;
import com.tpanh.hotel.authentication.domain.enums.UserStatus;
import com.tpanh.hotel.authentication.repository.RoleRepository;
import com.tpanh.hotel.user.domain.User;
import com.tpanh.hotel.user.repository.UserRepository;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
@ConditionalOnProperty(prefix = "app.default-admin", name = "enabled", havingValue = "true")
public class AuthDataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPhone;
    private final String adminFullName;
    private final String adminPassword;

    public AuthDataInitializer(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.default-admin.email}") String adminEmail,
            @Value("${app.default-admin.phone}") String adminPhone,
            @Value("${app.default-admin.full-name}") String adminFullName,
            @Value("${app.default-admin.password}") String adminPassword) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPhone = adminPhone;
        this.adminFullName = adminFullName;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        Role adminRole = roleRepository
                .findByCode(RoleCode.ADMIN)
                .orElseThrow(() -> new IllegalStateException("Vai trò ADMIN phải được khởi tạo bằng Flyway"));
        createAdminIfAbsent(adminRole);
    }

    private void createAdminIfAbsent(Role adminRole) {
        var userByEmail = userRepository.findByEmailIgnoreCase(adminEmail);
        var userByPhone = userRepository.findByPhone(adminPhone);
        if (userByEmail.isPresent() || userByPhone.isPresent()) {
            if (userByEmail.isPresent()
                    && userByPhone.isPresent()
                    && !userByEmail.get().getId().equals(userByPhone.get().getId())) {
                throw new IllegalStateException(
                        "Email và số điện thoại quản trị mặc định thuộc về hai người dùng khác nhau");
            }
            return;
        }

        User admin = new User();
        admin.setEmail(adminEmail);
        admin.setPhone(adminPhone);
        admin.setFullName(adminFullName);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setStatus(UserStatus.ACTIVE);
        admin.setRoles(Set.of(adminRole));
        userRepository.save(admin);
    }
}
