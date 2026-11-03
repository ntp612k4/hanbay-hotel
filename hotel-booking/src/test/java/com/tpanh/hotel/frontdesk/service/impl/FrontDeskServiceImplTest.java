package com.tpanh.hotel.frontdesk.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tpanh.hotel.booking.domain.Booking;
import com.tpanh.hotel.booking.domain.BookingGuest;
import com.tpanh.hotel.booking.domain.BookingStatus;
import com.tpanh.hotel.booking.repository.BookingGuestRepository;
import com.tpanh.hotel.booking.repository.BookingRepository;
import com.tpanh.hotel.common.exception.RequestValidationException;
import com.tpanh.hotel.room.domain.RoomType;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class FrontDeskServiceImplTest {

    private static final UUID BOOKING_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID ROOM_TYPE_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final LocalDate HOTEL_TODAY = LocalDate.of(2026, 8, 19);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-18T17:30:00Z"), ZoneOffset.UTC);
    private static final Sort ARRIVAL_SORT = Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("bookingCode"));

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingGuestRepository bookingGuestRepository;

    private FrontDeskServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FrontDeskServiceImpl(bookingRepository, bookingGuestRepository, CLOCK);
    }

    @Test
    void getsConfirmedArrivalsUsingHotelDateAndStableSort() {
        Booking booking = booking();
        BookingGuest primaryGuest = primaryGuest(booking, "Nguyễn Văn A");
        PageRequest pageable = PageRequest.of(0, 20, ARRIVAL_SORT);
        when(bookingRepository.findAllByCheckInDateAndStatus(HOTEL_TODAY, BookingStatus.CONFIRMED, pageable))
                .thenReturn(new PageImpl<>(List.of(booking), pageable, 1));
        when(bookingGuestRepository.findPrimaryGuestsByBookingIds(List.of(BOOKING_ID)))
                .thenReturn(List.of(primaryGuest));

        var result = service.getTodayArrivals(0, 20);

        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content()).singleElement().satisfies(item -> {
            assertThat(item.bookingId()).isEqualTo(BOOKING_ID);
            assertThat(item.bookingCode()).isEqualTo("HAN-20260819-001");
            assertThat(item.bookingStatus()).isEqualTo(BookingStatus.CONFIRMED);
            assertThat(item.checkInDate()).isEqualTo(HOTEL_TODAY);
            assertThat(item.checkOutDate()).isEqualTo(LocalDate.of(2026, 8, 21));
            assertThat(item.adultCount()).isEqualTo(2);
            assertThat(item.childCount()).isEqualTo(1);
            assertThat(item.createdAt()).isEqualTo(Instant.parse("2026-08-10T08:30:00Z"));
            assertThat(item.roomType().id()).isEqualTo(ROOM_TYPE_ID);
            assertThat(item.roomType().code()).isEqualTo("DLX");
            assertThat(item.roomType().name()).isEqualTo("Deluxe");
            assertThat(item.primaryGuest().fullName()).isEqualTo("Nguyễn Văn A");
            assertThat(item.primaryGuest().phone()).isEqualTo("0901234567");
            assertThat(item.primaryGuest().email()).isEqualTo("guest@example.com");
        });
        verify(bookingRepository).findAllByCheckInDateAndStatus(HOTEL_TODAY, BookingStatus.CONFIRMED, pageable);
    }

    @Test
    void returnsNullGuestInsteadOfFallingBackWhenPrimaryGuestIsMissing() {
        Booking booking = booking();
        PageRequest pageable = PageRequest.of(0, 20, ARRIVAL_SORT);
        when(bookingRepository.findAllByCheckInDateAndStatus(HOTEL_TODAY, BookingStatus.CONFIRMED, pageable))
                .thenReturn(new PageImpl<>(List.of(booking), pageable, 1));
        when(bookingGuestRepository.findPrimaryGuestsByBookingIds(List.of(BOOKING_ID)))
                .thenReturn(List.of());

        var result = service.getTodayArrivals(0, 20);

        assertThat(result.content())
                .singleElement()
                .extracting(item -> item.primaryGuest())
                .isNull();
    }

    @Test
    void usesFirstGuestWhenCorruptDataContainsMultiplePrimaryGuests() {
        Booking booking = booking();
        BookingGuest first = primaryGuest(booking, "Khách chính đầu tiên");
        BookingGuest duplicate = primaryGuest(booking, "Khách chính trùng");
        PageRequest pageable = PageRequest.of(0, 20, ARRIVAL_SORT);
        when(bookingRepository.findAllByCheckInDateAndStatus(HOTEL_TODAY, BookingStatus.CONFIRMED, pageable))
                .thenReturn(new PageImpl<>(List.of(booking), pageable, 1));
        when(bookingGuestRepository.findPrimaryGuestsByBookingIds(List.of(BOOKING_ID)))
                .thenReturn(List.of(first, duplicate));

        var result = service.getTodayArrivals(0, 20);

        assertThat(result.content())
                .singleElement()
                .extracting(item -> item.primaryGuest().fullName())
                .isEqualTo("Khách chính đầu tiên");
    }

    @Test
    void returnsEmptyPageWithoutLoadingGuests() {
        PageRequest pageable = PageRequest.of(2, 10, ARRIVAL_SORT);
        when(bookingRepository.findAllByCheckInDateAndStatus(HOTEL_TODAY, BookingStatus.CONFIRMED, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        var result = service.getTodayArrivals(2, 10);

        assertThat(result.content()).isEmpty();
        assertThat(result.page()).isEqualTo(2);
        assertThat(result.totalElements()).isZero();
        assertThat(result.totalPages()).isZero();
        verify(bookingGuestRepository, never()).findPrimaryGuestsByBookingIds(List.of());
    }

    @Test
    void rejectsPageBelowZeroBeforeQueryingDatabase() {
        assertThatThrownBy(() -> service.getTodayArrivals(-1, 20))
                .isInstanceOf(RequestValidationException.class)
                .hasMessage("Chỉ số trang phải lớn hơn hoặc bằng 0");

        verify(bookingRepository, never())
                .findAllByCheckInDateAndStatus(HOTEL_TODAY, BookingStatus.CONFIRMED, PageRequest.of(0, 20));
    }

    @Test
    void rejectsSizeOutsideSupportedRangeBeforeQueryingDatabase() {
        assertThatThrownBy(() -> service.getTodayArrivals(0, 0))
                .isInstanceOf(RequestValidationException.class)
                .hasMessage("Số phần tử mỗi trang phải từ 1 đến 100");
        assertThatThrownBy(() -> service.getTodayArrivals(0, 101))
                .isInstanceOf(RequestValidationException.class)
                .hasMessage("Số phần tử mỗi trang phải từ 1 đến 100");

        verify(bookingRepository, never())
                .findAllByCheckInDateAndStatus(HOTEL_TODAY, BookingStatus.CONFIRMED, PageRequest.of(0, 20));
    }

    @Test
    void usesNextHotelDateWhenUtcIsStillOnPreviousDate() {
        Clock utcNearHotelMidnight = Clock.fixed(Instant.parse("2026-08-19T17:00:00Z"), ZoneOffset.UTC);
        service = new FrontDeskServiceImpl(bookingRepository, bookingGuestRepository, utcNearHotelMidnight);
        LocalDate nextHotelDate = LocalDate.of(2026, 8, 20);
        PageRequest pageable = PageRequest.of(0, 20, ARRIVAL_SORT);
        when(bookingRepository.findAllByCheckInDateAndStatus(nextHotelDate, BookingStatus.CONFIRMED, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        service.getTodayArrivals(0, 20);

        verify(bookingRepository).findAllByCheckInDateAndStatus(nextHotelDate, BookingStatus.CONFIRMED, pageable);
    }

    private Booking booking() {
        RoomType roomType = RoomType.builder()
                .id(ROOM_TYPE_ID)
                .code("DLX")
                .name("Deluxe")
                .basePrice(1_750_000)
                .maxGuests(3)
                .capacityAdults(2)
                .capacityChildren(1)
                .build();
        Booking booking = Booking.builder()
                .id(BOOKING_ID)
                .bookingCode("HAN-20260819-001")
                .roomType(roomType)
                .checkInDate(HOTEL_TODAY)
                .checkOutDate(LocalDate.of(2026, 8, 21))
                .adultCount(2)
                .childCount(1)
                .status(BookingStatus.CONFIRMED)
                .build();
        booking.setCreatedAt(Instant.parse("2026-08-10T08:30:00Z"));
        return booking;
    }

    private BookingGuest primaryGuest(Booking booking, String fullName) {
        return BookingGuest.builder()
                .id(UUID.randomUUID())
                .booking(booking)
                .fullName(fullName)
                .phone("0901234567")
                .email("guest@example.com")
                .identityNumber("012345678901")
                .isPrimary(true)
                .build();
    }
}
