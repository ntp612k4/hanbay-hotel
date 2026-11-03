package com.tpanh.hotel.booking.repository;

import com.tpanh.hotel.booking.domain.BookingGuest;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingGuestRepository extends JpaRepository<BookingGuest, UUID> {

    @Query(
            """
            SELECT guest
            FROM BookingGuest guest
            WHERE guest.booking.id IN :bookingIds
              AND guest.isPrimary = true
            ORDER BY guest.createdAt ASC, guest.id ASC
            """)
    List<BookingGuest> findPrimaryGuestsByBookingIds(@Param("bookingIds") Collection<UUID> bookingIds);
}
