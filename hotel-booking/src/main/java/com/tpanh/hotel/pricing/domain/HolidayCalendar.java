package com.tpanh.hotel.pricing.domain;

import com.tpanh.hotel.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(
        name = "holiday_calendar",
        indexes = @Index(name = "idx_holiday_calendar_dates", columnList = "start_date, end_date"))
public class HolidayCalendar extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @EqualsAndHashCode.Include
    @Builder.Default
    @Column(name = "reference_id", updatable = false, nullable = false, unique = true)
    private UUID referenceId = UUID.randomUUID();

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;

    public static HolidayCalendar create(String name, LocalDate startDate, LocalDate endDate) {
        return HolidayCalendar.builder()
                .name(name)
                .startDate(startDate)
                .endDate(endDate)
                .active(true)
                .build();
    }

    public void updateDetails(String name, LocalDate startDate, LocalDate endDate) {
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void updateStatus(boolean active) {
        this.active = active;
    }

    public boolean includes(LocalDate date) {
        return date != null && !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    @PrePersist
    @PreUpdate
    private void validateInvariants() {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalStateException("Ngày bắt đầu không thể lớn hơn ngày kết thúc.");
        }
    }
}
