package com.tpanh.hotel.addon.domain;

import com.tpanh.hotel.common.domain.BaseEntity;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "add_on_services")
public class AddOnService extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @EqualsAndHashCode.Include
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "price", nullable = false)
    private long price;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;

    public static AddOnService create(String code, String name, String description, long price) {
        return AddOnService.builder()
                .code(code)
                .name(name)
                .description(description)
                .price(price)
                .active(true)
                .build();
    }

    public void updateDetails(String name, String description, long price) {
        this.name = name;
        this.description = description;
        this.price = price;
    }

    public void updateStatus(boolean active) {
        this.active = active;
    }
}
