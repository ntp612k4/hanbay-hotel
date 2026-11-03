package com.tpanh.hotel.room.service;

import com.tpanh.hotel.booking.domain.BookingStatus;
import com.tpanh.hotel.booking.repository.BookingRepository;
import com.tpanh.hotel.room.domain.RoomStatus;
import com.tpanh.hotel.room.repository.RoomRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoomAvailabilityCalculator {

    private static final List<BookingStatus> OCCUPYING_STATUSES =
            List.of(BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN);
    private static final List<RoomStatus> SELLABLE_ROOM_STATUSES =
            List.of(RoomStatus.AVAILABLE, RoomStatus.OCCUPIED, RoomStatus.DIRTY, RoomStatus.CLEANING);

    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final Clock clock;

    public long countAvailableRooms(UUID roomTypeId, LocalDate checkIn, LocalDate checkOut) {
        return countAvailableRooms(List.of(roomTypeId), checkIn, checkOut).getOrDefault(roomTypeId, 0L);
    }

    public long countAvailableRoomsExcludingBooking(
            UUID roomTypeId, LocalDate checkIn, LocalDate checkOut, UUID excludedBookingId) {
        long sellableRooms =
                roomRepository.countByRoomTypeIdsAndStatusIn(List.of(roomTypeId), SELLABLE_ROOM_STATUSES).stream()
                        .mapToLong(RoomRepository.RoomTypeRoomCount::getRoomCount)
                        .sum();
        long occupiedRooms = bookingRepository.countOverlappingActiveBookingsExcluding(
                roomTypeId,
                excludedBookingId,
                checkIn,
                checkOut,
                Instant.now(clock),
                OCCUPYING_STATUSES,
                BookingStatus.PENDING);
        return Math.max(0L, sellableRooms - occupiedRooms);
    }

    public Map<UUID, Long> countAvailableRooms(Collection<UUID> roomTypeIds, LocalDate checkIn, LocalDate checkOut) {
        if (roomTypeIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, Long> sellableRoomCounts =
                roomRepository.countByRoomTypeIdsAndStatusIn(roomTypeIds, SELLABLE_ROOM_STATUSES).stream()
                        .collect(Collectors.toMap(
                                RoomRepository.RoomTypeRoomCount::getRoomTypeId,
                                RoomRepository.RoomTypeRoomCount::getRoomCount));

        Map<UUID, Long> overlappingBookingCounts = bookingRepository
                .countOverlappingActiveBookingsByRoomTypeIds(
                        roomTypeIds, checkIn, checkOut, Instant.now(clock), OCCUPYING_STATUSES, BookingStatus.PENDING)
                .stream()
                .collect(Collectors.toMap(
                        BookingRepository.RoomTypeBookingCount::getRoomTypeId,
                        BookingRepository.RoomTypeBookingCount::getBookingCount));

        return roomTypeIds.stream()
                .distinct()
                .collect(Collectors.toMap(
                        Function.identity(),
                        roomTypeId -> Math.max(
                                0L,
                                sellableRoomCounts.getOrDefault(roomTypeId, 0L)
                                        - overlappingBookingCounts.getOrDefault(roomTypeId, 0L))));
    }
}
