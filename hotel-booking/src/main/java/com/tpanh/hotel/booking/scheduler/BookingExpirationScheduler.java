package com.tpanh.hotel.booking.scheduler;

import com.tpanh.hotel.booking.domain.Booking;
import com.tpanh.hotel.booking.domain.BookingStatus;
import com.tpanh.hotel.booking.repository.BookingRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class BookingExpirationScheduler {

    private final BookingRepository bookingRepository;
    private final Clock clock;

    @Scheduled(cron = "${booking.expiration.cron}")
    @Transactional
    public void expirePendingBookings() {
        Instant now = Instant.now(clock);
        Collection<Booking> expiredBookings =
                bookingRepository.findExpiredBookingsForUpdate(BookingStatus.PENDING, now);

        expiredBookings.forEach(booking -> booking.expireAutomatically(now));
        bookingRepository.saveAll(expiredBookings);
    }
}
