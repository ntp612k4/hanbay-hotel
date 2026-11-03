package com.tpanh.hotel.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tpanh.hotel.booking.domain.Booking;
import com.tpanh.hotel.booking.domain.BookingStatus;
import com.tpanh.hotel.booking.repository.BookingRepository;
import com.tpanh.hotel.booking.scheduler.BookingExpirationScheduler;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookingExpirationSchedulerTest {

    @Mock
    private BookingRepository bookingRepository;

    @Test
    void expiresAllOverduePendingBookingsAndSavesThem() {
        Instant now = Instant.parse("2026-08-04T06:00:00Z");
        Booking first = Booking.builder()
                .status(BookingStatus.PENDING)
                .expiredAt(now.minusSeconds(10))
                .build();
        Booking second =
                Booking.builder().status(BookingStatus.PENDING).expiredAt(now).build();
        List<Booking> expiredBookings = List.of(first, second);
        when(bookingRepository.findExpiredBookingsForUpdate(BookingStatus.PENDING, now))
                .thenReturn(expiredBookings);
        BookingExpirationScheduler scheduler =
                new BookingExpirationScheduler(bookingRepository, Clock.fixed(now, ZoneOffset.UTC));

        scheduler.expirePendingBookings();

        assertEquals(BookingStatus.EXPIRED, first.getStatus());
        assertEquals(BookingStatus.EXPIRED, second.getStatus());
        verify(bookingRepository).saveAll(expiredBookings);
    }
}
