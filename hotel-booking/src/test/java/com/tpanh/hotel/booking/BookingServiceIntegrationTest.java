package com.tpanh.hotel.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tpanh.hotel.addon.domain.AddOnService;
import com.tpanh.hotel.addon.repository.AddOnServiceRepository;
import com.tpanh.hotel.booking.domain.Booking;
import com.tpanh.hotel.booking.domain.BookingCodeGenerator;
import com.tpanh.hotel.booking.domain.BookingNight;
import com.tpanh.hotel.booking.domain.BookingStatus;
import com.tpanh.hotel.booking.dto.request.AddOnSelectionRequest;
import com.tpanh.hotel.booking.dto.request.CreatePendingBookingRequest;
import com.tpanh.hotel.booking.dto.request.GuestInfo;
import com.tpanh.hotel.booking.dto.response.CreatePendingBookingResponse;
import com.tpanh.hotel.booking.exception.BookingValidationException;
import com.tpanh.hotel.booking.exception.CapacityExceededException;
import com.tpanh.hotel.booking.exception.InvalidBookingDateException;
import com.tpanh.hotel.booking.exception.RoomNotAvailableException;
import com.tpanh.hotel.booking.repository.BookingRepository;
import com.tpanh.hotel.booking.service.BookingService;
import com.tpanh.hotel.pricing.dto.result.PricingResult;
import com.tpanh.hotel.pricing.dto.result.RoomPricingResult;
import com.tpanh.hotel.pricing.service.PricingService;
import com.tpanh.hotel.room.domain.Room;
import com.tpanh.hotel.room.domain.RoomStatus;
import com.tpanh.hotel.room.domain.RoomType;
import com.tpanh.hotel.room.exception.RoomTypeNotFoundException;
import com.tpanh.hotel.room.repository.RoomRepository;
import com.tpanh.hotel.room.repository.RoomTypeRepository;
import com.tpanh.hotel.room.service.AvailabilityService;
import com.tpanh.hotel.user.domain.User;
import com.tpanh.hotel.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = "booking.expiration.cron=-")
@Transactional
class BookingServiceIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-08-04T06:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 4);

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("pgvector/pgvector:pg17")
            .withDatabaseName("hotel_booking_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.docker.compose.enabled", () -> "false");
    }

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AddOnServiceRepository addOnServiceRepository;

    @MockitoBean
    private BookingCodeGenerator bookingCodeGenerator;

    @MockitoBean
    private PricingService pricingService;

    @MockitoBean
    private AvailabilityService availabilityService;

    @MockitoBean
    private Clock clock;

    private RoomType roomType;

    @BeforeEach
    void setUp() {
        roomType = roomTypeRepository.saveAndFlush(RoomType.builder()
                .code("DELUXE-" + UUID.randomUUID())
                .name("Deluxe")
                .basePrice(1_000)
                .capacityAdults(4)
                .capacityChildren(1)
                .maxGuests(4)
                .build());
        roomRepository.saveAndFlush(Room.builder()
                .roomType(roomType)
                .roomNumber("TEST-" + UUID.randomUUID().toString().substring(0, 12))
                .status(RoomStatus.AVAILABLE)
                .build());

        when(clock.instant()).thenReturn(NOW);
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        when(bookingCodeGenerator.generate()).thenReturn("BKG-" + UUID.randomUUID());
        when(pricingService.calculatePrice(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            List<BookingNight> nights = booking.getCheckInDate()
                    .datesUntil(booking.getCheckOutDate())
                    .map(stayDate -> BookingNight.builder()
                            .stayDate(stayDate)
                            .pricePerNight(1_000)
                            .build())
                    .toList();
            return new PricingResult(new RoomPricingResult(nights, 2_000, 0, 2_000), 0, 2_000);
        });
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void persistsAnonymousPendingBookingWithGuestAndPricingSnapshot() {
        CreatePendingBookingResponse response = bookingService.createPendingBooking(validRequest(List.of()));

        Booking persisted = bookingRepository.findById(response.bookingId()).orElseThrow();
        assertEquals(BookingStatus.PENDING, persisted.getStatus());
        assertEquals(2_000, persisted.getTotalAmount());
        assertEquals(NOW.plusSeconds(900), persisted.getExpiredAt());
        assertEquals(1, persisted.getGuests().size());
        assertEquals("012345678901", persisted.getGuests().get(0).getIdentityNumber());
        assertEquals(1, persisted.getStatusHistories().size());
    }

    @Test
    void persistsAuthenticatedUserAsBookingOwner() {
        User user = userRepository.saveAndFlush(User.builder().build());
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken(user.getId().toString(), null, "ROLE_USER"));

        CreatePendingBookingResponse response = bookingService.createPendingBooking(validRequest(List.of()));

        Booking persisted = bookingRepository.findById(response.bookingId()).orElseThrow();
        assertEquals(user.getId(), persisted.getBookedBy().getId());
        assertEquals(
                user.getId(),
                persisted.getStatusHistories().get(0).getChangedBy().getId());
    }

    @Test
    void persistsAllSelectedAddOns() {
        AddOnService breakfast = saveAddOn("BREAKFAST", 200);
        AddOnService spa = saveAddOn("SPA", 500);

        CreatePendingBookingRequest request = new CreatePendingBookingRequest(
                roomType.getId(),
                TODAY.plusDays(1),
                TODAY.plusDays(3),
                2,
                1,
                guestInfo(),
                List.of(new AddOnSelectionRequest(breakfast.getId(), 2), new AddOnSelectionRequest(spa.getId(), 1)));
        CreatePendingBookingResponse response = bookingService.createPendingBooking(request);

        Booking persisted = bookingRepository.findById(response.bookingId()).orElseThrow();
        assertEquals(2, persisted.getAddOns().size());
        assertTrue(persisted.getAddOns().stream()
                .anyMatch(addOn ->
                        addOn.getQuantity() == 2 && addOn.getUnitPrice() == 200 && addOn.getTotalPrice() == 400));
    }

    @Test
    void rollsBackWhenAnySelectedAddOnDoesNotExist() {
        AddOnService existing = saveAddOn("BREAKFAST", 200);
        UUID missingId = UUID.randomUUID();
        long bookingCountBefore = bookingRepository.count();

        BookingValidationException exception = assertThrows(
                BookingValidationException.class,
                () -> bookingService.createPendingBooking(validRequest(List.of(existing.getId(), missingId))));

        assertTrue(exception.getMessage().contains(missingId.toString()));
        assertEquals(bookingCountBefore, bookingRepository.count());
    }

    @Test
    void updatesPendingBookingAndReplacesPriceSnapshots() {
        AddOnService breakfast = saveAddOn("BREAKFAST", 200);
        CreatePendingBookingResponse created = bookingService.createPendingBooking(validRequest(List.of()));
        CreatePendingBookingRequest updatedRequest = new CreatePendingBookingRequest(
                roomType.getId(),
                TODAY.plusDays(2),
                TODAY.plusDays(4),
                2,
                1,
                guestInfo(),
                List.of(new AddOnSelectionRequest(breakfast.getId(), 2)));

        bookingService.updatePendingBooking(created.bookingId(), updatedRequest, created.bookingAccessToken());
        bookingRepository.flush();

        Booking updated = bookingRepository.findById(created.bookingId()).orElseThrow();
        assertEquals(TODAY.plusDays(2), updated.getCheckInDate());
        assertEquals(1, updated.getAddOns().size());
        assertEquals(2, updated.getAddOns().get(0).getQuantity());
        assertEquals(400, updated.getAddOns().get(0).getTotalPrice());
    }

    @Test
    void rejectsInvalidDatesBeforeAccessingPersistenceCollaborators() {
        assertThrows(
                InvalidBookingDateException.class,
                () -> bookingService.createPendingBooking(request(TODAY.minusDays(1), TODAY, 2, 1, List.of())));

        verifyNoInteractions(availabilityService, pricingService);
    }

    @Test
    void allowsBookingWithCheckInToday() {
        CreatePendingBookingResponse response =
                bookingService.createPendingBooking(request(TODAY, TODAY.plusDays(1), 2, 1, List.of()));

        assertEquals(BookingStatus.PENDING, response.status());
    }

    @Test
    void rejectsUnknownRoomType() {
        CreatePendingBookingRequest request = new CreatePendingBookingRequest(
                UUID.randomUUID(), TODAY.plusDays(1), TODAY.plusDays(2), 1, 0, guestInfo(), List.of());

        assertThrows(RoomTypeNotFoundException.class, () -> bookingService.createPendingBooking(request));
    }

    @Test
    void rejectsGuestCountAbovePersistedRoomCapacity() {
        assertThrows(
                CapacityExceededException.class,
                () -> bookingService.createPendingBooking(
                        request(TODAY.plusDays(1), TODAY.plusDays(2), 4, 1, List.of())));
        verifyNoInteractions(availabilityService, pricingService);
    }

    @Test
    void rollsBackWhenRoomIsNoLongerAvailable() {
        doThrow(new RoomNotAvailableException("Hết phòng"))
                .when(availabilityService)
                .ensureAvailable(roomType.getId(), TODAY.plusDays(1), TODAY.plusDays(3));
        long bookingCountBefore = bookingRepository.count();

        assertThrows(
                RoomNotAvailableException.class, () -> bookingService.createPendingBooking(validRequest(List.of())));

        assertEquals(bookingCountBefore, bookingRepository.count());
    }

    private CreatePendingBookingRequest validRequest(List<UUID> addOnIds) {
        return request(TODAY.plusDays(1), TODAY.plusDays(3), 2, 1, addOnIds);
    }

    private CreatePendingBookingRequest request(
            LocalDate checkIn, LocalDate checkOut, int adults, int children, List<UUID> addOnIds) {
        return new CreatePendingBookingRequest(
                roomType.getId(),
                checkIn,
                checkOut,
                adults,
                children,
                guestInfo(),
                addOnIds.stream()
                        .map(id -> new com.tpanh.hotel.booking.dto.request.AddOnSelectionRequest(id, 1))
                        .toList());
    }

    private GuestInfo guestInfo() {
        return new GuestInfo("Nguyễn Văn A", "guest@example.com", "0912345678", "012345678901");
    }

    private AddOnService saveAddOn(String code, long price) {
        return addOnServiceRepository.saveAndFlush(AddOnService.builder()
                .code(code + "-" + UUID.randomUUID())
                .name(code)
                .price(price)
                .build());
    }
}
