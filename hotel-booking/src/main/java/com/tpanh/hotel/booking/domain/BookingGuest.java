package com.tpanh.hotel.booking.domain;

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
@Table(
        name = "booking_guests",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"booking_id", "identity_number"})},
        indexes = {@Index(columnList = "booking_id")})
public class BookingGuest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @EqualsAndHashCode.Include
    @Builder.Default
    @Column(name = "reference_id", updatable = false, nullable = false, unique = true)
    private UUID referenceId = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "phone", length = 10)
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "identity_number", length = 50)
    private String identityNumber;

    @Builder.Default
    @Column(name = "is_primary")
    private boolean isPrimary = false;

    public void assignBooking(Booking booking) {
        this.booking = booking;
    }
}
