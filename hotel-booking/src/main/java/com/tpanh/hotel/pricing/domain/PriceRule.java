package com.tpanh.hotel.pricing.domain;

import com.tpanh.hotel.common.domain.BaseEntity;
import com.tpanh.hotel.pricing.exception.PricingValidationException;
import com.tpanh.hotel.room.domain.RoomType;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(
        name = "price_rules",
        indexes = {@Index(name = "idx_price_rules_room_availability", columnList = "room_type_id, start_date, end_date")
        })
public class PriceRule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @EqualsAndHashCode.Include
    @Builder.Default
    @Column(name = "reference_id", updatable = false, nullable = false, unique = true)
    private UUID referenceId = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "rule_type", nullable = false)
    private RuleType ruleType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "adjustment_type", nullable = false)
    private AdjustmentType adjustmentType;

    @Column(name = "adjustment_value", nullable = false)
    private long adjustmentValue;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "adjustment_direction", nullable = false)
    private AdjustmentDirection adjustmentDirection = AdjustmentDirection.INCREASE;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Builder.Default
    @Column(name = "priority", nullable = false)
    private int priority = 0;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;

    public static PriceRule create(
            RoomType roomType,
            String name,
            RuleType ruleType,
            AdjustmentType adjustmentType,
            long adjustmentValue,
            AdjustmentDirection adjustmentDirection,
            LocalDate startDate,
            LocalDate endDate,
            int priority) {
        return PriceRule.builder()
                .roomType(roomType)
                .name(name)
                .ruleType(ruleType)
                .adjustmentType(adjustmentType)
                .adjustmentValue(adjustmentValue)
                .adjustmentDirection(adjustmentDirection)
                .startDate(startDate)
                .endDate(endDate)
                .priority(priority)
                .active(true)
                .build();
    }

    public void updateDetails(
            RoomType roomType,
            String name,
            RuleType ruleType,
            AdjustmentType adjustmentType,
            long adjustmentValue,
            AdjustmentDirection adjustmentDirection,
            LocalDate startDate,
            LocalDate endDate,
            int priority) {
        this.roomType = roomType;
        this.name = name;
        this.ruleType = ruleType;
        this.adjustmentType = adjustmentType;
        this.adjustmentValue = adjustmentValue;
        this.adjustmentDirection = adjustmentDirection;
        this.startDate = startDate;
        this.endDate = endDate;
        this.priority = priority;
    }

    public void updateStatus(boolean active) {
        this.active = active;
    }

    @PrePersist
    @PreUpdate
    private void validateInvariants() {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new PricingValidationException("Ngày bắt đầu không thể lớn hơn ngày kết thúc.");
        }
        if (adjustmentValue < 0) {
            throw new PricingValidationException("Giá trị điều chỉnh (giảm giá/phụ thu) không được âm.");
        }
    }
}
