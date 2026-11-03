package com.tpanh.hotel.booking.domain.pricing;

import com.tpanh.hotel.booking.domain.BookingNight;
import com.tpanh.hotel.common.domain.BaseEntity;
import com.tpanh.hotel.pricing.domain.PriceRule;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(
        name = "booking_night_price_rules",
        uniqueConstraints = @UniqueConstraint(columnNames = {"booking_night_id", "price_rule_id"}))
public class BookingNightPriceRule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_night_id", nullable = false)
    private BookingNight bookingNight;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "price_rule_id", nullable = false)
    private PriceRule priceRule;

    @Column(nullable = false)
    private long adjustmentAmount;

    public void assignBookingNight(BookingNight bookingNight) {
        this.bookingNight = bookingNight;
    }
}
