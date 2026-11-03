package com.tpanh.hotel.booking.repository;

import com.tpanh.hotel.booking.domain.BookingNight;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingNightRepository extends JpaRepository<BookingNight, UUID> {}
