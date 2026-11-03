package com.tpanh.hotel.room.repository;

import com.tpanh.hotel.room.domain.Amenity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AmenityRepository extends JpaRepository<Amenity, UUID> {}
