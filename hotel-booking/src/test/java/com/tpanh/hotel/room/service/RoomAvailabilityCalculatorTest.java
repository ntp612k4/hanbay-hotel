package com.tpanh.hotel.room.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.tpanh.hotel.booking.domain.BookingStatus;
import com.tpanh.hotel.booking.repository.BookingRepository;
import com.tpanh.hotel.room.domain.RoomStatus;
import com.tpanh.hotel.room.repository.RoomRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoomAvailabilityCalculatorTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-08T00:00:00Z"), ZoneId.of("UTC"));
    private static final LocalDate CHECK_IN = LocalDate.of(2026, 8, 12);
    private static final LocalDate CHECK_OUT = LocalDate.of(2026, 8, 18);

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private BookingRepository bookingRepository;

    private RoomAvailabilityCalculator calculator;

    private static final List<RoomStatus> SELLABLE_STATUSES =
            List.of(RoomStatus.AVAILABLE, RoomStatus.OCCUPIED, RoomStatus.DIRTY, RoomStatus.CLEANING);

    @BeforeEach
    void setUp() {
        calculator = new RoomAvailabilityCalculator(roomRepository, bookingRepository, CLOCK);
    }

    @Test
    void subtractsOverlappingBookingsFromSellableRooms() {
        UUID suiteId = UUID.randomUUID();
        when(roomRepository.countByRoomTypeIdsAndStatusIn(List.of(suiteId), SELLABLE_STATUSES))
                .thenReturn(List.of(roomCount(suiteId, 3L)));
        when(bookingRepository.countOverlappingActiveBookingsByRoomTypeIds(
                        eq(List.of(suiteId)),
                        eq(CHECK_IN),
                        eq(CHECK_OUT),
                        eq(Instant.now(CLOCK)),
                        anyBookingStatuses(),
                        eq(BookingStatus.PENDING)))
                .thenReturn(List.of(bookingCount(suiteId, 1L)));

        Map<UUID, Long> counts = calculator.countAvailableRooms(List.of(suiteId), CHECK_IN, CHECK_OUT);

        assertEquals(2L, counts.get(suiteId));
    }

    @Test
    void neverReturnsNegativeAvailability() {
        UUID suiteId = UUID.randomUUID();
        when(roomRepository.countByRoomTypeIdsAndStatusIn(List.of(suiteId), SELLABLE_STATUSES))
                .thenReturn(List.of(roomCount(suiteId, 1L)));
        when(bookingRepository.countOverlappingActiveBookingsByRoomTypeIds(
                        eq(List.of(suiteId)),
                        eq(CHECK_IN),
                        eq(CHECK_OUT),
                        eq(Instant.now(CLOCK)),
                        anyBookingStatuses(),
                        eq(BookingStatus.PENDING)))
                .thenReturn(List.of(bookingCount(suiteId, 3L)));

        Map<UUID, Long> counts = calculator.countAvailableRooms(List.of(suiteId), CHECK_IN, CHECK_OUT);

        assertEquals(0L, counts.get(suiteId));
    }

    private RoomRepository.RoomTypeRoomCount roomCount(UUID roomTypeId, long roomCount) {
        return new RoomRepository.RoomTypeRoomCount() {
            @Override
            public UUID getRoomTypeId() {
                return roomTypeId;
            }

            @Override
            public long getRoomCount() {
                return roomCount;
            }
        };
    }

    private BookingRepository.RoomTypeBookingCount bookingCount(UUID roomTypeId, long bookingCount) {
        return new BookingRepository.RoomTypeBookingCount() {
            @Override
            public UUID getRoomTypeId() {
                return roomTypeId;
            }

            @Override
            public long getBookingCount() {
                return bookingCount;
            }
        };
    }

    @SuppressWarnings("unchecked")
    private Collection<BookingStatus> anyBookingStatuses() {
        return any(Collection.class);
    }
}
