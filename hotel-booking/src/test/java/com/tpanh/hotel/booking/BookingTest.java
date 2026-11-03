package com.tpanh.hotel.booking;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.tpanh.hotel.addon.domain.AddOnService;
import com.tpanh.hotel.booking.domain.*;
import com.tpanh.hotel.booking.domain.addon.BookingAddOn;
import com.tpanh.hotel.booking.exception.BookingValidationException;
import com.tpanh.hotel.booking.exception.InvalidBookingStateException;
import com.tpanh.hotel.room.domain.RoomType;
import com.tpanh.hotel.user.domain.User;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class BookingTest {

    private Booking booking;

    @Mock
    private User mockUser;

    @Mock
    private RoomType mockRoomType;

    @BeforeEach
    void setUp() {
        booking = Booking.builder()
                .id(UUID.randomUUID())
                .bookingCode("BKG-123")
                .bookedBy(mockUser)
                .roomType(mockRoomType)
                .checkInDate(LocalDate.now())
                .checkOutDate(LocalDate.now().plusDays(2))
                .status(BookingStatus.PENDING)
                .build();
    }

    @Test
    void should_record_created_successfully() {
        booking.recordCreated(mockUser);

        assertEquals(1, booking.getStatusHistories().size());
        BookingStatusHistory history = booking.getStatusHistories().get(0);
        assertNull(history.getOldStatus());
        assertEquals(BookingStatus.PENDING, history.getNewStatus());
        assertEquals("Người dùng đặt phòng", history.getReason());
        assertEquals(mockUser, history.getChangedBy());
        assertEquals(booking, history.getBooking());
    }

    @Nested
    class GuestManagementTests {
        @Test
        void should_add_guest_successfully() {
            BookingGuest guest = mock(BookingGuest.class);
            when(guest.getIdentityNumber()).thenReturn("123456789");

            booking.addGuest(guest);

            assertTrue(booking.getGuests().contains(guest));
            verify(guest).assignBooking(booking);
        }

        @Test
        void should_throw_exception_when_adding_duplicate_guest_identity() {
            BookingGuest guest1 = mock(BookingGuest.class);
            when(guest1.getIdentityNumber()).thenReturn("123456789");
            BookingGuest guest2 = mock(BookingGuest.class);
            when(guest2.getIdentityNumber()).thenReturn("123456789"); // Trùng CMND

            booking.addGuest(guest1);

            BookingValidationException exception =
                    assertThrows(BookingValidationException.class, () -> booking.addGuest(guest2));
            assertEquals("Khách hàng với CMND/CCCD này đã tồn tại trong Booking.", exception.getMessage());
        }

        @Test
        void should_remove_guest_successfully() {
            BookingGuest guest = mock(BookingGuest.class);
            booking.getGuests().add(guest);

            booking.removeGuest(guest);

            assertFalse(booking.getGuests().contains(guest));
            verify(guest).assignBooking(null);
        }
    }

    @Nested
    class NightManagementTests {
        @Test
        void should_add_night_successfully() {
            BookingNight night = mock(BookingNight.class);

            booking.addNight(night);

            assertTrue(booking.getNights().contains(night));
            verify(night).assignBooking(booking);
        }

        @Test
        void should_throw_exception_when_adding_duplicate_night_date() {
            BookingNight night1 = mock(BookingNight.class);
            when(night1.getStayDate()).thenReturn(LocalDate.now());
            BookingNight night2 = mock(BookingNight.class);
            when(night2.getStayDate()).thenReturn(LocalDate.now());

            booking.addNight(night1);

            BookingValidationException exception =
                    assertThrows(BookingValidationException.class, () -> booking.addNight(night2));
            assertTrue(exception.getMessage().contains("đã tồn tại trong Booking."));
        }
    }

    @Nested
    class AddOnManagementTests {
        @Test
        void should_add_addon_successfully() {
            BookingAddOn addOn = mock(BookingAddOn.class);

            booking.addAddOn(addOn);

            assertTrue(booking.getAddOns().contains(addOn));
            verify(addOn).assignBooking(booking);
        }

        @Test
        void should_throw_exception_when_adding_duplicate_addon() {
            UUID serviceId = UUID.randomUUID();
            AddOnService service = mock(AddOnService.class);
            when(service.getId()).thenReturn(serviceId);

            BookingAddOn addOn1 = mock(BookingAddOn.class);
            when(addOn1.getAddOnService()).thenReturn(service);

            BookingAddOn addOn2 = mock(BookingAddOn.class);
            when(addOn2.getAddOnService()).thenReturn(service);

            booking.addAddOn(addOn1);

            BookingValidationException exception =
                    assertThrows(BookingValidationException.class, () -> booking.addAddOn(addOn2));
            assertEquals("Dịch vụ đã được thêm vào đặt phòng.", exception.getMessage());
        }
    }

    @Nested
    class StatusTransitionTests {

        @Test
        void should_confirm_booking_successfully() {
            // Setup điều kiện confirm hợp lệ: Status = PENDING, chưa hết hạn
            booking = Booking.builder()
                    .status(BookingStatus.PENDING)
                    .expiredAt(Instant.now().plusSeconds(3600))
                    .build();
            Instant currentTime = Instant.now();

            booking.confirm(mockUser, currentTime);

            assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
            assertNull(booking.getExpiredAt());
            assertEquals(1, booking.getStatusHistories().size()); // Kiểm tra sinh ra history record
        }

        @Test
        void should_throw_exception_when_confirming_expired_booking() {
            Instant expiredTime = Instant.now().minusSeconds(3600); // Đã hết hạn cách đây 1 tiếng
            booking = Booking.builder()
                    .status(BookingStatus.PENDING)
                    .expiredAt(expiredTime)
                    .build();

            InvalidBookingStateException exception =
                    assertThrows(InvalidBookingStateException.class, () -> booking.confirm(mockUser, Instant.now()));
            assertEquals("Không thể xác nhận đặt phòng đã quá hạn thanh toán.", exception.getMessage());
        }

        @Test
        void should_cancel_booking_successfully() {
            // Giả sử PENDING cho phép cancel
            booking = Booking.builder().status(BookingStatus.PENDING).build();
            String reason = "Khách hàng đổi ý";

            booking.cancel(mockUser, reason, java.time.Instant.parse("2026-08-12T07:00:00Z"));

            assertEquals(BookingStatus.CANCELLED, booking.getStatus());
            assertEquals(1, booking.getStatusHistories().size());
            assertEquals(reason, booking.getStatusHistories().get(0).getReason());
        }

        @Test
        void should_throw_exception_when_cancel_reason_is_empty() {
            BookingValidationException exception = assertThrows(
                    BookingValidationException.class,
                    () -> booking.cancel(mockUser, "   ", java.time.Instant.parse("2026-08-12T07:00:00Z")));
            assertEquals("Lý do hủy không được để trống", exception.getMessage());
        }

        @Test
        void should_expire_booking_successfully() {
            Instant expiredTime = Instant.now().minusSeconds(3600); // Đã quá hạn
            booking = Booking.builder()
                    .status(BookingStatus.PENDING)
                    .expiredAt(expiredTime)
                    .build();

            booking.expire(mockUser, Instant.now());

            assertEquals(BookingStatus.EXPIRED, booking.getStatus());
            assertNull(booking.getExpiredAt());
            assertEquals(
                    "Hệ thống tự động hủy do quá hạn thanh toán",
                    booking.getStatusHistories().get(0).getReason());
        }

        @Test
        void should_throw_exception_when_expiring_before_expired_at() {
            Instant expiredTime = Instant.now().plusSeconds(3600); // Chưa tới hạn
            booking = Booking.builder()
                    .status(BookingStatus.PENDING)
                    .expiredAt(expiredTime)
                    .build();

            InvalidBookingStateException exception =
                    assertThrows(InvalidBookingStateException.class, () -> booking.expire(mockUser, Instant.now()));
            assertEquals("Booking chưa đến thời hạn hết hạn.", exception.getMessage());
        }
    }

    @Test
    void should_apply_total_amount() {
        long amount = 5000000L;

        booking.applyTotalAmount(amount);

        assertEquals(amount, booking.getTotalAmount());
    }
}
