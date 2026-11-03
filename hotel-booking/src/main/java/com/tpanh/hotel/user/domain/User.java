package com.tpanh.hotel.user.domain;

import com.tpanh.hotel.authentication.domain.entity.Role;
import com.tpanh.hotel.authentication.domain.enums.UserStatus;
import com.tpanh.hotel.common.domain.BaseEntity;
import jakarta.persistence.*;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import lombok.*;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "full_name", length = 150)
    private String fullName;

    @Column(length = 30)
    private String phone;

    @Column(name = "avatar_url", columnDefinition = "text")
    private String avatarUrl;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserStatus status = UserStatus.ACTIVE;

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new LinkedHashSet<>();
}
