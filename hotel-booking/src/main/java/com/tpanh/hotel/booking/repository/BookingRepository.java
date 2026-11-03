package com.tpanh.hotel.booking.repository;

import com.tpanh.hotel.booking.domain.Booking;
import com.tpanh.hotel.booking.domain.BookingStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.time.LocalDate;
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

public interface BookingRepository extends JpaRepository<Booking, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT booking
            FROM Booking booking
            WHERE booking.bookingCode = :bookingCode
            """)
    Optional<Booking> findByBookingCodeForUpdate(@Param("bookingCode") String bookingCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT booking
            FROM Booking booking
            JOIN FETCH booking.roomType
            WHERE booking.bookingCode = :bookingCode
            """)
    Optional<Booking> findByBookingCodeWithRoomTypeForUpdate(@Param("bookingCode") String bookingCode);

    @EntityGraph(attributePaths = "roomType")
    Optional<Booking> findByBookingCode(String bookingCode);

    @EntityGraph(attributePaths = "bookedBy")
    Optional<Booking> findByBookingCodeAndBookedById(String bookingCode, UUID userId);

    boolean existsByBookingCode(String bookingCode);

    @EntityGraph(attributePaths = "roomType")
    Page<Booking> findAllByBookedById(UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = "roomType")
    Page<Booking> findAllByBookedByIdAndStatus(UUID userId, BookingStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "roomType")
    Page<Booking> findAllByCheckInDateAndStatus(LocalDate checkInDate, BookingStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "roomType")
    Page<Booking> findAllByCheckOutDateLessThanEqualAndStatus(
            LocalDate checkOutDate, BookingStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "roomType")
    Page<Booking> findAllByStatus(BookingStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select booking from Booking booking join fetch booking.roomType where booking.id = :id")
    java.util.Optional<Booking> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select booking from Booking booking where booking.id = :id")
    java.util.Optional<Booking> findByIdWithAssignedRoomForUpdate(@Param("id") UUID id);

    @Query(
            """
            SELECT b.roomType.id AS roomTypeId, COUNT(b) AS bookingCount
            FROM Booking b
            WHERE b.roomType.id IN :roomTypeIds
              AND b.checkInDate < :checkOut
              AND b.checkOutDate > :checkIn
              AND (
                b.status IN :confirmedStatuses
                OR (b.status = :pendingStatus AND b.expiredAt > :now)
              )
            GROUP BY b.roomType.id
            """)
    Collection<RoomTypeBookingCount> countOverlappingActiveBookingsByRoomTypeIds(
            @Param("roomTypeIds") Collection<UUID> roomTypeIds,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut,
            @Param("now") Instant now,
            @Param("confirmedStatuses") Collection<BookingStatus> confirmedStatuses,
            @Param("pendingStatus") BookingStatus pendingStatus);

    @Query(
            """
            SELECT COUNT(b)
            FROM Booking b
            WHERE b.roomType.id = :roomTypeId
              AND b.id <> :excludedBookingId
              AND b.checkInDate < :checkOut
              AND b.checkOutDate > :checkIn
              AND (b.status IN :confirmedStatuses OR (b.status = :pendingStatus AND b.expiredAt > :now))
            """)
    long countOverlappingActiveBookingsExcluding(
            @Param("roomTypeId") UUID roomTypeId,
            @Param("excludedBookingId") UUID excludedBookingId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut,
            @Param("now") Instant now,
            @Param("confirmedStatuses") Collection<BookingStatus> confirmedStatuses,
            @Param("pendingStatus") BookingStatus pendingStatus);

    @Query(
            """
            select count(b) from Booking b
            where b.assignedRoom.id = :roomId and b.id <> :excludedBookingId
              and b.checkInDate < :checkOut and b.checkOutDate > :checkIn
              and b.status in :statuses
            """)
    long countAssignedRoomConflicts(
            @Param("roomId") UUID roomId,
            @Param("excludedBookingId") UUID excludedBookingId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut,
            @Param("statuses") Collection<BookingStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT b
            FROM Booking b
            WHERE b.status = :status
              AND b.expiredAt IS NOT NULL
              AND b.expiredAt <= :now
            """)
    Collection<Booking> findExpiredBookingsForUpdate(@Param("status") BookingStatus status, @Param("now") Instant now);

    interface RoomTypeBookingCount {
        UUID getRoomTypeId();

        long getBookingCount();
    }
}
