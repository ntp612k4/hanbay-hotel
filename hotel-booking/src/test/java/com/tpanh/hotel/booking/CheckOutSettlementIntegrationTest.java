package com.tpanh.hotel.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.tpanh.hotel.booking.domain.Booking;
import com.tpanh.hotel.booking.domain.BookingStatus;
import com.tpanh.hotel.booking.dto.request.CheckOutGuestRequest;
import com.tpanh.hotel.booking.exception.CheckOutConflictException;
import com.tpanh.hotel.booking.repository.BookingRepository;
import com.tpanh.hotel.booking.service.CheckOutService;
import com.tpanh.hotel.common.security.CurrentUserProvider;
import com.tpanh.hotel.payment.domain.PaymentStatus;
import com.tpanh.hotel.payment.repository.PaymentRepository;
import com.tpanh.hotel.room.domain.Room;
import com.tpanh.hotel.room.domain.RoomStatus;
import com.tpanh.hotel.room.domain.RoomType;
import com.tpanh.hotel.room.repository.RoomRepository;
import com.tpanh.hotel.room.repository.RoomTypeRepository;
import com.tpanh.hotel.user.domain.User;
import com.tpanh.hotel.user.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(properties = "booking.expiration.cron=-")
@Testcontainers(disabledWithoutDocker = true)
class CheckOutSettlementIntegrationTest {
    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("pgvector/pgvector:pg17");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.docker.compose.enabled", () -> false);
    }

    @Autowired
    private CheckOutService service;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private TransactionTemplate transactions;

    @Autowired
    private Clock clock;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    private UUID bookingId;
    private UUID roomId;
    private UUID staffId;
    private final CheckOutGuestRequest request =
            new CheckOutGuestRequest("Đã kiểm tra phòng", 200_000L, "Vỡ đèn", 300_000L);

    @BeforeEach
    void seed() {
        transactions.executeWithoutResult(status -> {
            staffId = userRepository.saveAndFlush(User.builder().build()).getId();
            RoomType type = roomTypeRepository.saveAndFlush(RoomType.builder()
                    .code("TEST-" + UUID.randomUUID())
                    .name("Phòng kiểm thử")
                    .basePrice(1_000_000L)
                    .capacityAdults(2)
                    .maxGuests(2)
                    .build());
            Room room = roomRepository.saveAndFlush(Room.builder()
                    .roomType(type)
                    .roomNumber("T-" + UUID.randomUUID().toString().substring(0, 8))
                    .status(RoomStatus.OCCUPIED)
                    .build());
            roomId = room.getId();
            bookingId = bookingRepository
                    .saveAndFlush(Booking.builder()
                            .bookingCode("TEST-" + UUID.randomUUID())
                            .roomType(type)
                            .assignedRoom(room)
                            .checkInDate(LocalDate.now(clock).minusDays(1))
                            .checkOutDate(LocalDate.now(clock))
                            .status(BookingStatus.CHECKED_IN)
                            .totalAmount(1_000_000L)
                            .paidAmount(900_000L)
                            .build())
                    .getId();
        });
        when(currentUserProvider.currentUserId()).thenReturn(Optional.of(staffId));
    }

    @Test
    void persistsCashReceiptAndInspectionAuditWithCheckout() {
        service.checkOut(bookingId, request);
        transactions.executeWithoutResult(status -> {
            Booking booking = bookingRepository.findById(bookingId).orElseThrow();
            assertThat(booking.getStatus()).isEqualTo(BookingStatus.CHECKED_OUT);
            assertThat(booking.getTotalAmount()).isEqualTo(1_200_000L);
            assertThat(booking.getPaidAmount()).isEqualTo(1_200_000L);
            assertThat(booking.getStatusHistories())
                    .singleElement()
                    .satisfies(history -> assertThat(history.getReason()).contains("Vỡ đèn", "300000 VND"));
            assertThat(paymentRepository.findAllByBookingIdInOrderByCreatedAtDesc(List.of(bookingId)))
                    .singleElement()
                    .satisfies(payment -> {
                        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
                        assertThat(payment.getAmount()).isEqualTo(300_000L);
                        assertThat(payment.getTransactions()).singleElement().satisfies(receipt -> {
                            assertThat(receipt.getAmount()).isEqualTo(300_000L);
                            assertThat(receipt.getRawPayload()).containsEntry("collectedBy", staffId.toString());
                        });
                    });
        });
        assertThat(roomRepository.findById(roomId).orElseThrow().getStatus()).isEqualTo(RoomStatus.DIRTY);
    }

    @Test
    void rollsBackReceiptBalancesAndRoomWhenTransactionFails() {
        assertThatThrownBy(() -> transactions.executeWithoutResult(status -> {
                    service.checkOut(bookingId, request);
                    bookingRepository.flush();
                    throw new IllegalStateException("Simulated failure before commit");
                }))
                .isInstanceOf(IllegalStateException.class);
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CHECKED_IN);
        assertThat(booking.getTotalAmount()).isEqualTo(1_000_000L);
        assertThat(booking.getPaidAmount()).isEqualTo(900_000L);
        assertThat(paymentRepository.findAllByBookingIdInOrderByCreatedAtDesc(List.of(bookingId)))
                .isEmpty();
        assertThat(roomRepository.findById(roomId).orElseThrow().getStatus()).isEqualTo(RoomStatus.OCCUPIED);
    }

    @Test
    void concurrentCheckoutCollectsOnlyOnce() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        java.util.concurrent.Callable<String> attempt = () -> {
            start.await(10, TimeUnit.SECONDS);
            try {
                service.checkOut(bookingId, request);
                return "SUCCESS";
            } catch (CheckOutConflictException exception) {
                return exception.getErrorCode();
            }
        };
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(attempt);
            var second = executor.submit(attempt);
            start.countDown();
            assertThat(List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("SUCCESS", "BOOKING_ALREADY_CHECKED_OUT");
        } finally {
            executor.shutdownNow();
        }
        assertThat(paymentRepository.findAllByBookingIdInOrderByCreatedAtDesc(List.of(bookingId)))
                .hasSize(1);
        assertThat(bookingRepository.findById(bookingId).orElseThrow().getPaidAmount())
                .isEqualTo(1_200_000L);
    }
}
