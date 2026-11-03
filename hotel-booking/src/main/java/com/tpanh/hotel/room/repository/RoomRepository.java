package com.tpanh.hotel.room.repository;

import com.tpanh.hotel.room.domain.Room;
import com.tpanh.hotel.room.domain.RoomStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomRepository extends JpaRepository<Room, UUID> {
    @Query("select room.roomType.id from Room room where room.id = :id")
    Optional<UUID> findRoomTypeId(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select room from Room room join fetch room.roomType where room.id = :id")
    Optional<Room> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select room from Room room where room.roomNumber = :roomNumber")
    Optional<Room> findByRoomNumberForUpdate(@Param("roomNumber") String roomNumber);

    Collection<Room> findAllByRoomTypeIdAndStatusOrderByFloorAscRoomNumberAsc(UUID roomTypeId, RoomStatus status);

    @EntityGraph(attributePaths = "roomType")
    Page<Room> findAllByStatus(RoomStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "roomType")
    Page<Room> findAllByStatusIn(Collection<RoomStatus> statuses, Pageable pageable);

    Page<Room> findAllByOrderByFloorAscRoomNumberAsc(Pageable pageable);

    @Query(
            """
            SELECT r.roomType.id AS roomTypeId, COUNT(r) AS roomCount
            FROM Room r
            WHERE r.roomType.id IN :roomTypeIds
              AND r.status IN :statuses
            GROUP BY r.roomType.id
            """)
    Collection<RoomTypeRoomCount> countByRoomTypeIdsAndStatusIn(
            @Param("roomTypeIds") Collection<UUID> roomTypeIds, @Param("statuses") Collection<RoomStatus> statuses);

    interface RoomTypeRoomCount {
        UUID getRoomTypeId();

        long getRoomCount();
    }
}
