package com.tpanh.hotel.booking.repository;

import com.tpanh.hotel.booking.domain.addon.BookingAddOn;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingAddOnRepository extends JpaRepository<BookingAddOn, UUID> {}
