package com.tpanh.hotel.room.repository;

import com.tpanh.hotel.room.domain.RoomType;
import com.tpanh.hotel.room.domain.RoomTypeStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomTypeRepository extends JpaRepository<RoomType, UUID> {

    /*
     * develop / HAN-21
     * Lấy danh sách loại phòng đang active.
     */
    List<RoomType> findAllByStatusOrderByNameAsc(RoomTypeStatus status);

    Optional<RoomType> findByIdAndStatus(UUID id, RoomTypeStatus status);

    /*
     * develop
     * Dùng khi cần lock RoomType để update.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT rt FROM RoomType rt WHERE rt.id = :id")
    Optional<RoomType> findByIdForUpdate(@Param("id") UUID id);

    /*
     * HAN-22
     * Lấy chi tiết RoomType cùng images và amenities.
     */
    @Query(
            """
            SELECT DISTINCT roomType
            FROM RoomType roomType
            LEFT JOIN FETCH roomType.roomTypeImages
            LEFT JOIN FETCH roomType.amenities
            WHERE roomType.id = :id
              AND roomType.status = :status
            """)
    Optional<RoomType> findDetailByIdAndStatus(@Param("id") UUID id, @Param("status") RoomTypeStatus status);

    @Query(
            """
            SELECT DISTINCT roomType
            FROM RoomType roomType
            LEFT JOIN FETCH roomType.roomTypeImages
            LEFT JOIN FETCH roomType.amenities
            WHERE LOWER(roomType.name) = LOWER(:name)
              AND roomType.status = :status
            """)
    Optional<RoomType> findDetailByNameIgnoreCaseAndStatus(
            @Param("name") String name, @Param("status") RoomTypeStatus status);

    /*
     * develop / HAN-21
     * Search + pagination danh sách loại phòng.
     */
    Page<RoomType> findByStatusAndNameContainingIgnoreCase(RoomTypeStatus status, String name, Pageable pageable);
}
