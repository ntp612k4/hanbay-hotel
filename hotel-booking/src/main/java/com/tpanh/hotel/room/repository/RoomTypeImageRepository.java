package com.tpanh.hotel.room.repository;

import com.tpanh.hotel.room.domain.RoomTypeImage;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomTypeImageRepository extends JpaRepository<RoomTypeImage, UUID> {}
