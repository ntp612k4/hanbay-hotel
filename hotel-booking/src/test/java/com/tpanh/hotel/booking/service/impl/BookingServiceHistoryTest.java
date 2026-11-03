package com.tpanh.hotel.booking.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tpanh.hotel.booking.domain.Booking;
import com.tpanh.hotel.booking.domain.BookingStatus;
import com.tpanh.hotel.booking.exception.BookingValidationException;
import com.tpanh.hotel.booking.repository.BookingRepository;
import com.tpanh.hotel.common.exception.RequestValidationException;
import com.tpanh.hotel.common.security.CurrentUserProvider;
import com.tpanh.hotel.payment.domain.Payment;
import com.tpanh.hotel.payment.domain.PaymentStatus;
import com.tpanh.hotel.payment.repository.PaymentRepository;
import com.tpanh.hotel.room.domain.RoomType;
import com.tpanh.hotel.user.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class BookingServiceHistoryTest {

    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID BOOKING_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private BookingServiceImpl service;

    @Test
    void returnsOnlyCurrentUsersBookingsWithLatestPaymentStatus() {
        authenticate(USER_ID);
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        Booking booking = booking();
        PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(bookingRepository.findAllByBookedById(USER_ID, pageable))
                .thenReturn(new PageImpl<>(List.of(booking), pageable, 1));
        Payment olderPayment = payment(booking, PaymentStatus.FAILED, Instant.parse("2026-08-09T09:00:00Z"));
        Payment latestPayment = payment(booking, PaymentStatus.SUCCESS, Instant.parse("2026-08-10T09:00:00Z"));
        when(paymentRepository.findAllByBookingIdInOrderByCreatedAtDesc(List.of(BOOKING_ID)))
                .thenReturn(List.of(latestPayment, olderPayment));

        var result = service.getCurrentUserBookingHistory(0, 10, null);

        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content()).singleElement().satisfies(item -> {
            assertThat(item.bookingId()).isEqualTo(BOOKING_ID);
            assertThat(item.totalNights()).isEqualTo(2);
            assertThat(item.totalAmount()).isEqualByComparingTo("3500000");
            assertThat(item.paymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(item.roomType().name()).isEqualTo("Phòng Deluxe");
        });
    }

    @Test
    void appliesStatusFilterInsideOwnershipQuery() {
        authenticate(USER_ID);
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        PageRequest pageable = PageRequest.of(1, 5, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(bookingRepository.findAllByBookedByIdAndStatus(USER_ID, BookingStatus.CONFIRMED, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        var result = service.getCurrentUserBookingHistory(1, 5, BookingStatus.CONFIRMED);

        assertThat(result.content()).isEmpty();
        verify(bookingRepository).findAllByBookedByIdAndStatus(USER_ID, BookingStatus.CONFIRMED, pageable);
        verify(paymentRepository, never()).findAllByBookingIdInOrderByCreatedAtDesc(List.of());
    }

    @Test
    void rejectsInvalidPaginationBeforeQueryingDatabase() {
        assertThatThrownBy(() -> service.getCurrentUserBookingHistory(-1, 10, null))
                .isInstanceOf(RequestValidationException.class)
                .hasMessage("Chỉ số trang phải lớn hơn hoặc bằng 0");
        assertThatThrownBy(() -> service.getCurrentUserBookingHistory(0, 101, null))
                .isInstanceOf(RequestValidationException.class)
                .hasMessage("Số phần tử mỗi trang phải từ 1 đến 100");

        verify(bookingRepository, never()).findAllByBookedById(USER_ID, PageRequest.of(0, 10));
    }

    @Test
    void rejectsRequestWithoutAuthenticatedUser() {
        assertThatThrownBy(() -> service.getCurrentUserBookingHistory(0, 10, null))
                .isInstanceOf(BookingValidationException.class)
                .hasMessage("Bạn cần đăng nhập để xem lịch sử đặt phòng.");
    }

    private void authenticate(UUID userId) {
        when(currentUserProvider.currentUserId()).thenReturn(java.util.Optional.of(userId));
    }

    private Booking booking() {
        RoomType roomType = RoomType.builder()
                .id(UUID.fromString("30000000-0000-0000-0000-000000000001"))
                .name("Phòng Deluxe")
                .thumbnailUrl("https://example.com/deluxe.jpg")
                .basePrice(1_750_000)
                .maxGuests(3)
                .capacityAdults(2)
                .capacityChildren(1)
                .build();
        Booking booking = Booking.builder()
                .id(BOOKING_ID)
                .bookingCode("BK20260810000001")
                .roomType(roomType)
                .checkInDate(LocalDate.of(2026, 8, 10))
                .checkOutDate(LocalDate.of(2026, 8, 12))
                .adultCount(2)
                .childCount(1)
                .totalAmount(3_500_000)
                .status(BookingStatus.CONFIRMED)
                .build();
        booking.setCreatedAt(Instant.parse("2026-08-10T03:30:00Z"));
        return booking;
    }

    private Payment payment(Booking booking, PaymentStatus status, Instant createdAt) {
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .booking(booking)
                .paymentCode(UUID.randomUUID().toString())
                .amount(3_500_000)
                .status(status)
                .expiresAt(createdAt.plusSeconds(900))
                .build();
        payment.setCreatedAt(createdAt);
        return payment;
    }
}
