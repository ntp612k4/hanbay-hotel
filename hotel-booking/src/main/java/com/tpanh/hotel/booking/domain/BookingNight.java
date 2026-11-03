package com.tpanh.hotel.booking.domain;

import com.tpanh.hotel.booking.domain.pricing.BookingNightPriceRule;
import com.tpanh.hotel.common.domain.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.*;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(
        name = "booking_nights",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "idx_booking_nights_booking_date",
                    columnNames = {"booking_id", "stay_date"})
        })
public class BookingNight extends BaseEntity {

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

    @Column(name = "stay_date", nullable = false)
    private LocalDate stayDate;

    @Setter
    @Column(name = "price_per_night", nullable = false)
    private long pricePerNight;

    @OneToMany(mappedBy = "bookingNight", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BookingNightPriceRule> appliedRules = new ArrayList<>();

    public void assignBooking(Booking booking) {
        this.booking = booking;
    }

    public void addAppliedRule(BookingNightPriceRule appliedRule) {
        appliedRules.add(appliedRule);
        appliedRule.assignBookingNight(this);
    }
}
