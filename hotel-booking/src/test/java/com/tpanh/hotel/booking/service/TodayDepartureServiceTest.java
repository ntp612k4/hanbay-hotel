package com.tpanh.hotel.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tpanh.hotel.booking.domain.Booking;
import com.tpanh.hotel.booking.domain.BookingGuest;
import com.tpanh.hotel.booking.domain.BookingStatus;
import com.tpanh.hotel.booking.dto.response.TodayDepartureResponse;
import com.tpanh.hotel.booking.mapper.TodayDepartureMapper;
import com.tpanh.hotel.booking.repository.BookingGuestRepository;
import com.tpanh.hotel.booking.repository.BookingRepository;
import com.tpanh.hotel.common.response.PageResponse;
import com.tpanh.hotel.room.domain.RoomType;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class TodayDepartureServiceTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 19);

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingGuestRepository bookingGuestRepository;

    private TodayDepartureService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-19T03:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        service = new TodayDepartureService(
                bookingRepository, bookingGuestRepository, Mappers.getMapper(TodayDepartureMapper.class), clock);
    }

    @Test
    void getTodayDeparturesMapsBookingAndPrimaryGuest() {
        Booking booking = createBooking(5_000_000L, 3_500_000L);
        BookingGuest guest = BookingGuest.builder()
                .booking(booking)
                .fullName("Nguyễn Văn An")
                .phone("0901234567")
                .isPrimary(true)
                .build();
        PageRequest pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "bookingCode"));
        when(bookingRepository.findAllByStatus(BookingStatus.CHECKED_IN, pageable))
                .thenReturn(new PageImpl<>(List.of(booking), pageable, 1));
        when(bookingGuestRepository.findPrimaryGuestsByBookingIds(List.of(booking.getId())))
                .thenReturn(List.of(guest));

        PageResponse<TodayDepartureResponse> result = service.getTodayDepartures(0, 20);

        assertThat(result.content()).hasSize(1);
        TodayDepartureResponse departure = result.content().get(0);
        assertThat(departure.bookingId()).isEqualTo(booking.getId());
        assertThat(departure.bookingCode()).isEqualTo("BK202608190001");
        assertThat(departure.guestName()).isEqualTo("Nguyễn Văn An");
        assertThat(departure.guestPhone()).isEqualTo("0901234567");
        assertThat(departure.roomTypeName()).isEqualTo("Phòng Deluxe");
        assertThat(departure.checkInDate()).isEqualTo(TODAY.minusDays(2));
        assertThat(departure.checkOutDate()).isEqualTo(TODAY);
        assertThat(departure.adults()).isEqualTo(2);
        assertThat(departure.children()).isEqualTo(1);
        assertThat(departure.status()).isEqualTo(BookingStatus.CHECKED_IN);
        assertThat(departure.totalAmount()).isEqualTo(5_000_000L);
        assertThat(departure.paidAmount()).isEqualTo(3_500_000L);
        assertThat(departure.outstandingAmount()).isEqualTo(1_500_000L);
    }

    @Test
    void getTodayDeparturesSkipsGuestQueryWhenPageIsEmpty() {
        PageRequest pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "bookingCode"));
        when(bookingRepository.findAllByStatus(BookingStatus.CHECKED_IN, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        PageResponse<TodayDepartureResponse> result = service.getTodayDepartures(0, 20);

        assertThat(result.content()).isEmpty();
        verify(bookingGuestRepository, never()).findPrimaryGuestsByBookingIds(any());
    }

    @Test
    void getTodayDeparturesDoesNotReturnNegativeOutstandingAmount() {
        Booking booking = createBooking(4_000_000L, 4_500_000L);
        PageRequest pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "bookingCode"));
        when(bookingRepository.findAllByStatus(BookingStatus.CHECKED_IN, pageable))
                .thenReturn(new PageImpl<>(List.of(booking), pageable, 1));
        when(bookingGuestRepository.findPrimaryGuestsByBookingIds(List.of(booking.getId())))
                .thenReturn(List.of());

        TodayDepartureResponse departure =
                service.getTodayDepartures(0, 20).content().get(0);

        assertThat(departure.outstandingAmount()).isZero();
        assertThat(departure.guestName()).isEqualTo("Chưa cập nhật");
        assertThat(departure.guestPhone()).isNull();
    }

    @Test
    void getTodayDeparturesRejectsInvalidPaginationBeforeQueryingDatabase() {
        assertThatThrownBy(() -> service.getTodayDepartures(-1, 20)).isInstanceOf(IllegalArgumentException.class);

        verify(bookingRepository, never()).findAllByStatus(any(), any());
    }

    @Test
    void getTodayDeparturesIncludesAllCheckedInStaysRegardlessOfServerTimeZone() {
        Clock utcClock = Clock.fixed(Instant.parse("2026-08-19T17:00:00Z"), ZoneOffset.UTC);
        service = new TodayDepartureService(
                bookingRepository, bookingGuestRepository, Mappers.getMapper(TodayDepartureMapper.class), utcClock);
        PageRequest pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "bookingCode"));
        when(bookingRepository.findAllByStatus(BookingStatus.CHECKED_IN, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        service.getTodayDepartures(0, 20);

        verify(bookingRepository).findAllByStatus(BookingStatus.CHECKED_IN, pageable);
    }

    private Booking createBooking(long totalAmount, long paidAmount) {
        RoomType roomType = RoomType.builder().name("Phòng Deluxe").build();
        return Booking.builder()
                .id(UUID.randomUUID())
                .bookingCode("BK202608190001")
                .roomType(roomType)
                .checkInDate(TODAY.minusDays(2))
                .checkOutDate(TODAY)
                .adultCount(2)
                .childCount(1)
                .status(BookingStatus.CHECKED_IN)
                .totalAmount(totalAmount)
                .paidAmount(paidAmount)
                .build();
    }
}
