package com.tpanh.hotel.booking.domain.addon;

import com.tpanh.hotel.addon.domain.AddOnService;
import com.tpanh.hotel.booking.domain.Booking;
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
        name = "booking_add_ons",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"booking_id", "add_on_service_id"})},
        indexes = {@Index(columnList = "booking_id")})
public class BookingAddOn extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "add_on_service_id", nullable = false)
    private AddOnService addOnService;

    @Builder.Default
    @Column(name = "quantity", nullable = false)
    private int quantity = 1;

    @Column(name = "unit_price", nullable = false)
    private long unitPrice;

    @Column(name = "total_price", nullable = false)
    private long totalPrice;

    public void assignBooking(Booking booking) {
        this.booking = booking;
    }

    public void updateSnapshot(int quantity, long unitPrice) {
        if (quantity < 1) {
            throw new IllegalArgumentException("Số lượng dịch vụ phải lớn hơn hoặc bằng 1");
        }
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = Math.multiplyExact(unitPrice, quantity);
    }
}
