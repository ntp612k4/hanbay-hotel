package com.tpanh.hotel.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.tpanh.hotel.addon.service.AddOnCatalogService;
import com.tpanh.hotel.booking.domain.BookingStatus;
import com.tpanh.hotel.booking.service.BookingService;
import com.tpanh.hotel.payment.service.PaymentService;
import com.tpanh.hotel.pricing.mapper.PricingMapper;
import com.tpanh.hotel.pricing.service.PricingService;
import com.tpanh.hotel.room.service.AvailableRoomTypeSearchService;
import com.tpanh.hotel.room.service.RoomTypeService;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.tool.annotation.Tool;

@ExtendWith(MockitoExtension.class)
class HotelAiToolsTest {

    @Mock
    private AvailableRoomTypeSearchService availableRoomTypeSearchService;

    @Mock
    private PricingService pricingService;

    @Mock
    private PricingMapper pricingMapper;

    @Mock
    private RoomTypeService roomTypeService;

    @Mock
    private AddOnCatalogService addOnCatalogService;

    @Mock
    private BookingService bookingService;

    @Mock
    private PaymentService paymentService;

    private HotelAiTools tools;

    @BeforeEach
    void setUp() {
        tools = new HotelAiTools(
                availableRoomTypeSearchService,
                pricingService,
                pricingMapper,
                roomTypeService,
                addOnCatalogService,
                bookingService,
                paymentService);
    }

    @Test
    void exposesExactlyTheEightRequestedReadOnlyTools() {
        Set<String> names = Stream.of(HotelAiTools.class.getDeclaredMethods())
                .map(method -> method.getAnnotation(Tool.class))
                .filter(java.util.Objects::nonNull)
                .map(Tool::name)
                .collect(Collectors.toSet());

        assertThat(names)
                .containsExactlyInAnyOrder(
                        "search_available_rooms",
                        "calculate_stay_price",
                        "get_room_type_details",
                        "list_active_addons",
                        "get_my_bookings",
                        "get_booking_details",
                        "get_payment_status",
                        "check_cancellation_eligibility");
    }

    @Test
    void searchAvailableRoomsUsesSafeServerSidePagination() {
        LocalDate checkIn = LocalDate.of(2026, 9, 2);
        LocalDate checkOut = LocalDate.of(2026, 9, 5);

        tools.searchAvailableRooms(checkIn, checkOut, 2, 1);

        verify(availableRoomTypeSearchService).search(checkIn, checkOut, 2, 1, null, 0, 20);
    }

    @Test
    void bookingToolsDelegateToOwnerScopedBookingService() {
        tools.getMyBookings(BookingStatus.CONFIRMED);
        tools.getBookingDetails("HAN-123");
        tools.checkCancellationEligibility("HAN-123");

        verify(bookingService).getCurrentUserBookingHistory(0, 20, BookingStatus.CONFIRMED);
        verify(bookingService).getCurrentUserBooking("HAN-123");
        verify(bookingService).getCancellationEligibility("HAN-123");
    }

    @Test
    void paymentStatusDoesNotAcceptAnAiSuppliedAccessToken() {
        UUID paymentId = UUID.randomUUID();

        tools.getPaymentStatus(paymentId);

        verify(paymentService).getPaymentStatus(paymentId, null);
    }

    @Test
    void roomDetailsCanBeResolvedFromTheDisplayedName() {
        tools.getRoomTypeDetails("Junior Suite");

        verify(roomTypeService).getDetailByName("Junior Suite");
    }

    @Test
    void everyToolHasAUsefulDescription() {
        for (Method method : HotelAiTools.class.getDeclaredMethods()) {
            Tool annotation = method.getAnnotation(Tool.class);
            if (annotation != null) {
                assertThat(annotation.description()).isNotBlank();
            }
        }
    }
}
