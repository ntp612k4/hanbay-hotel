package com.tpanh.hotel.user.domain;

import com.tpanh.hotel.authentication.domain.enums.UserStatus;
import com.tpanh.hotel.common.domain.BaseEntity;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "account_access_audits")
@Getter
@Setter
@NoArgsConstructor
public class AccountAccessAudit extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 30)
    private String action;

    @Column(name = "old_roles", columnDefinition = "text")
    private String oldRoles;

    @Column(name = "new_roles", nullable = false, columnDefinition = "text")
    private String newRoles;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 30)
    private UserStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 30)
    private UserStatus newStatus;
}
