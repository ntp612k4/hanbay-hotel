package com.tpanh.hotel.frontdesk.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.tpanh.hotel.booking.domain.Booking;
import com.tpanh.hotel.booking.domain.BookingGuest;
import com.tpanh.hotel.booking.domain.BookingStatus;
import com.tpanh.hotel.booking.repository.BookingGuestRepository;
import com.tpanh.hotel.booking.repository.BookingRepository;
import com.tpanh.hotel.room.domain.Room;
import com.tpanh.hotel.room.domain.RoomStatus;
import com.tpanh.hotel.room.domain.RoomType;
import com.tpanh.hotel.room.repository.RoomRepository;
import com.tpanh.hotel.room.repository.RoomTypeRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Transactional
@Testcontainers(disabledWithoutDocker = true)
class FrontDeskRepositoryIntegrationTest {

    private static final LocalDate ARRIVAL_DATE = LocalDate.of(2099, 8, 19);

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("pgvector/pgvector:pg17");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.docker.compose.enabled", () -> false);
    }

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingGuestRepository bookingGuestRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void queriesOnlyConfirmedBookingsForDateAndBatchLoadsPrimaryGuests() {
        RoomType roomType = roomTypeRepository.saveAndFlush(roomType());
        Booking first = booking("ARRIVAL-A-" + UUID.randomUUID(), roomType, ARRIVAL_DATE, BookingStatus.CONFIRMED);
        first.addGuest(guest("Khách chính A", true));
        first.addGuest(guest("Khách đi cùng A", false));
        Booking second = booking("ARRIVAL-B-" + UUID.randomUUID(), roomType, ARRIVAL_DATE, BookingStatus.CONFIRMED);
        second.addGuest(guest("Khách chính B", true));
        Booking pending = booking("ARRIVAL-P-" + UUID.randomUUID(), roomType, ARRIVAL_DATE, BookingStatus.PENDING);
        Booking checkedIn = booking("ARRIVAL-I-" + UUID.randomUUID(), roomType, ARRIVAL_DATE, BookingStatus.CHECKED_IN);
        Booking anotherDate =
                booking("ARRIVAL-D-" + UUID.randomUUID(), roomType, ARRIVAL_DATE.plusDays(1), BookingStatus.CONFIRMED);
        bookingRepository.saveAllAndFlush(List.of(first, second, pending, checkedIn, anotherDate));

        PageRequest pageable =
                PageRequest.of(0, 1, Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("bookingCode")));
        var firstPage =
                bookingRepository.findAllByCheckInDateAndStatus(ARRIVAL_DATE, BookingStatus.CONFIRMED, pageable);
        var allConfirmed = bookingRepository.findAllByCheckInDateAndStatus(
                ARRIVAL_DATE, BookingStatus.CONFIRMED, PageRequest.of(0, 10, pageable.getSort()));
        List<UUID> bookingIds =
                allConfirmed.getContent().stream().map(Booking::getId).toList();
        List<BookingGuest> primaryGuests = bookingGuestRepository.findPrimaryGuestsByBookingIds(bookingIds);

        assertThat(firstPage.getTotalElements()).isEqualTo(2);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.getContent()).hasSize(1).allSatisfy(booking -> {
            assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
            assertThat(booking.getCheckInDate()).isEqualTo(ARRIVAL_DATE);
            assertThat(booking.getRoomType().getCode()).isEqualTo(roomType.getCode());
        });
        assertThat(allConfirmed.getContent())
                .extracting(Booking::getId)
                .containsExactlyInAnyOrder(first.getId(), second.getId());
        assertThat(primaryGuests)
                .extracting(BookingGuest::getFullName)
                .containsExactlyInAnyOrder("Khách chính A", "Khách chính B");
        assertThat(primaryGuests).allMatch(BookingGuest::isPrimary);
    }

    @Test
    void includesOverdueCheckedInBookingsAndLocksAssignedRoomSafely() {
        RoomType roomType = roomTypeRepository.saveAndFlush(roomType());
        Room room = roomRepository.saveAndFlush(Room.builder()
                .roomType(roomType)
                .roomNumber("CHECKOUT-" + UUID.randomUUID().toString().substring(0, 8))
                .floor(5)
                .status(RoomStatus.OCCUPIED)
                .build());
        Booking overdue = Booking.builder()
                .bookingCode("OVERDUE-" + UUID.randomUUID())
                .roomType(roomType)
                .assignedRoom(room)
                .checkInDate(ARRIVAL_DATE.minusDays(6))
                .checkOutDate(ARRIVAL_DATE.minusDays(2))
                .adultCount(2)
                .status(BookingStatus.CHECKED_IN)
                .totalAmount(2_000_000L)
                .paidAmount(2_000_000L)
                .build();
        overdue = bookingRepository.saveAndFlush(overdue);
        UUID bookingId = overdue.getId();
        entityManager.clear();

        var departures = bookingRepository.findAllByCheckOutDateLessThanEqualAndStatus(
                ARRIVAL_DATE, BookingStatus.CHECKED_IN, PageRequest.of(0, 10));
        Booking locked =
                bookingRepository.findByIdWithAssignedRoomForUpdate(bookingId).orElseThrow();

        assertThat(departures.getContent()).extracting(Booking::getId).contains(bookingId);
        assertThat(locked.getAssignedRoom().getId()).isEqualTo(room.getId());
    }

    private RoomType roomType() {
        return RoomType.builder()
                .code("ARRIVAL_" + UUID.randomUUID().toString().replace("-", ""))
                .name("Arrival Test Deluxe")
                .description("Integration test room type")
                .capacityAdults(2)
                .capacityChildren(1)
                .basePrice(1_000_000L)
                .maxGuests(3)
                .build();
    }

    private Booking booking(String code, RoomType roomType, LocalDate checkInDate, BookingStatus status) {
        return Booking.builder()
                .bookingCode(code)
                .roomType(roomType)
                .checkInDate(checkInDate)
                .checkOutDate(checkInDate.plusDays(2))
                .adultCount(2)
                .childCount(1)
                .status(status)
                .totalAmount(2_000_000L)
                .build();
    }

    private BookingGuest guest(String fullName, boolean primary) {
        return BookingGuest.builder()
                .fullName(fullName)
                .phone("0901234567")
                .email("guest@example.com")
                .identityNumber(UUID.randomUUID().toString())
                .isPrimary(primary)
                .build();
    }
}
