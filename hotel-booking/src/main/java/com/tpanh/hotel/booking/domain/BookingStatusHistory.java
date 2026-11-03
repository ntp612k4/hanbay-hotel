package com.tpanh.hotel.booking.domain;

import com.tpanh.hotel.common.domain.BaseEntity;
import com.tpanh.hotel.user.domain.User;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(
        name = "booking_status_histories",
        indexes = {@Index(columnList = "booking_id")})
public class BookingStatusHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @EqualsAndHashCode.Include
    @Builder.Default
    @Column(name = "reference_id", updatable = false, nullable = false, unique = true)
    private UUID referenceId = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, updatable = false)
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "old_status", nullable = true, updatable = false)
    private BookingStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "new_status", nullable = false, updatable = false)
    private BookingStatus newStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by", updatable = false)
    private User changedBy;

    @Column(name = "reason", updatable = false, columnDefinition = "TEXT")
    private String reason;

    void assignBooking(Booking booking) {
        this.booking = booking;
    }
}
