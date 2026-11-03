package com.tpanh.hotel.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tpanh.hotel.booking.domain.Booking;
import com.tpanh.hotel.booking.domain.BookingFactory;
import com.tpanh.hotel.booking.domain.BookingStatus;
import com.tpanh.hotel.booking.exception.InvalidBookingStateException;
import com.tpanh.hotel.room.domain.RoomType;
import com.tpanh.hotel.user.domain.User;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class BookingAggregateTest {

    private static final Instant NOW = Instant.parse("2026-08-04T06:00:00Z");

    @Test
    void factoryCreatesPendingBookingWithFifteenMinuteHoldAndHistory() {
        User user = User.builder().build();
        BookingFactory factory = new BookingFactory(Clock.fixed(NOW, ZoneOffset.UTC));

        Booking booking = factory.createPendingBooking(
                user,
                "BKG-001",
                RoomType.builder().maxGuests(2).build(),
                LocalDate.of(2026, 8, 5),
                LocalDate.of(2026, 8, 6),
                2,
                0);

        assertEquals(BookingStatus.PENDING, booking.getStatus());
        assertEquals(NOW.plusSeconds(900), booking.getExpiredAt());
        assertEquals(1, booking.getStatusHistories().size());
        assertEquals(BookingStatus.PENDING, booking.getStatusHistories().get(0).getNewStatus());
        assertEquals(user, booking.getStatusHistories().get(0).getChangedBy());
    }

    @Test
    void automaticExpirationAllowsSystemActorAndRecordsHistory() {
        Booking booking = booking(BookingStatus.PENDING, NOW.minusSeconds(1));

        booking.expireAutomatically(NOW);

        assertEquals(BookingStatus.EXPIRED, booking.getStatus());
        assertNull(booking.getExpiredAt());
        assertNull(booking.getStatusHistories().get(0).getChangedBy());
        assertEquals(
                "Hệ thống tự động hủy do quá hạn thanh toán",
                booking.getStatusHistories().get(0).getReason());
    }

    @Test
    void expirationAtExactDeadlineIsAllowed() {
        Booking booking = booking(BookingStatus.PENDING, NOW);

        booking.expireAutomatically(NOW);

        assertEquals(BookingStatus.EXPIRED, booking.getStatus());
    }

    @Test
    void cannotExpireBeforeDeadlineOrFromNonPendingState() {
        assertThrows(InvalidBookingStateException.class, () -> booking(BookingStatus.PENDING, NOW.plusSeconds(1))
                .expireAutomatically(NOW));
        assertThrows(InvalidBookingStateException.class, () -> booking(BookingStatus.CONFIRMED, null)
                .expireAutomatically(NOW));
    }

    @Test
    void cannotConfirmFromNonPendingState() {
        Booking booking = booking(BookingStatus.CANCELLED, null);

        assertThrows(
                InvalidBookingStateException.class,
                () -> booking.confirm(User.builder().build(), NOW));
    }

    @Test
    void confirmationAtExactDeadlineIsAllowedAndClearsExpiration() {
        Booking booking = booking(BookingStatus.PENDING, NOW);

        booking.confirm(User.builder().build(), NOW);

        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        assertNull(booking.getExpiredAt());
    }

    @Test
    void cannotCancelFromTerminalState() {
        Booking booking = booking(BookingStatus.EXPIRED, null);

        assertThrows(
                InvalidBookingStateException.class,
                () -> booking.cancel(
                        User.builder().build(), "Khách đổi ý", java.time.Instant.parse("2026-08-12T07:00:00Z")));
    }

    private Booking booking(BookingStatus status, Instant expiredAt) {
        return Booking.builder().status(status).expiredAt(expiredAt).build();
    }
}
