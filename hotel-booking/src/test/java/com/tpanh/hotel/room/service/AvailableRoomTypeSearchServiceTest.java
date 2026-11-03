package com.tpanh.hotel.room.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tpanh.hotel.booking.domain.BookingNight;
import com.tpanh.hotel.booking.domain.pricing.BookingNightPriceRule;
import com.tpanh.hotel.common.response.PageResponse;
import com.tpanh.hotel.pricing.domain.AdjustmentType;
import com.tpanh.hotel.pricing.domain.PriceRule;
import com.tpanh.hotel.pricing.domain.RuleType;
import com.tpanh.hotel.pricing.dto.result.RoomPricingResult;
import com.tpanh.hotel.pricing.service.PricingService;
import com.tpanh.hotel.room.domain.RoomType;
import com.tpanh.hotel.room.domain.RoomTypeStatus;
import com.tpanh.hotel.room.dto.response.AvailableRoomTypeResponse;
import com.tpanh.hotel.room.exception.RoomValidationException;
import com.tpanh.hotel.room.mapper.RoomTypeMapper;
import com.tpanh.hotel.room.repository.RoomTypeRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AvailableRoomTypeSearchServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-08T00:00:00Z"), ZoneId.of("UTC"));
    private static final LocalDate CHECK_IN = LocalDate.of(2026, 8, 12);
    private static final LocalDate CHECK_OUT = LocalDate.of(2026, 8, 18);

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @Mock
    private RoomAvailabilityCalculator roomAvailabilityCalculator;

    @Mock
    private PricingService pricingService;

    private AvailableRoomTypeSearchService service;

    @BeforeEach
    void setUp() {
        service = new AvailableRoomTypeSearchService(
                roomTypeRepository,
                roomAvailabilityCalculator,
                CLOCK,
                pricingService,
                Mappers.getMapper(RoomTypeMapper.class));
        lenient()
                .when(pricingService.calculateRoomPrice(any(), any(), any()))
                .thenReturn(new RoomPricingResult(List.of(), 0, 0, 0));
    }

    @Test
    void returnsRoomTypesWithAvailableInventory() {
        RoomType suite = roomType("SUITE", "Suite", 4);
        when(roomTypeRepository.findAllByStatusOrderByNameAsc(RoomTypeStatus.ACTIVE))
                .thenReturn(List.of(suite));
        when(roomAvailabilityCalculator.countAvailableRooms(List.of(suite.getId()), CHECK_IN, CHECK_OUT))
                .thenReturn(Map.of(suite.getId(), 2L));
        BookingNight adjustedNight = night(CHECK_IN.plusDays(1), 2_400_000);
        adjustedNight.addAppliedRule(BookingNightPriceRule.builder()
                .priceRule(PriceRule.builder()
                        .name("Phụ thu cuối tuần")
                        .ruleType(RuleType.WEEKEND)
                        .adjustmentType(AdjustmentType.PERCENTAGE)
                        .adjustmentValue(20)
                        .build())
                .adjustmentAmount(400_000)
                .build());
        when(pricingService.calculateRoomPrice(suite, CHECK_IN, CHECK_OUT))
                .thenReturn(new RoomPricingResult(
                        List.of(night(CHECK_IN, 2_000_000), adjustedNight), 4_400_000, 0, 4_400_000));

        PageResponse<AvailableRoomTypeResponse> responses = service.search(CHECK_IN, CHECK_OUT, 2, 0, null, 0, 6);

        assertEquals(1, responses.totalElements());
        assertEquals("SUITE", responses.content().get(0).code());
        assertEquals(2, responses.content().get(0).availableRooms());
        assertEquals(2, responses.content().get(0).totalNights());
        assertEquals(4_400_000, responses.content().get(0).roomTotal());
        var adjustment =
                responses.content().get(0).nightlyPrices().get(1).adjustments().get(0);
        assertEquals("Phụ thu cuối tuần", adjustment.name());
        assertEquals(RuleType.WEEKEND, adjustment.ruleType());
        assertEquals(AdjustmentType.PERCENTAGE, adjustment.adjustmentType());
        assertEquals(20, adjustment.adjustmentValue());
        assertEquals(400_000, adjustment.amount());
    }

    @Test
    void excludesRoomTypesWithoutAvailableInventory() {
        RoomType suite = roomType("SUITE", "Suite", 4);
        when(roomTypeRepository.findAllByStatusOrderByNameAsc(RoomTypeStatus.ACTIVE))
                .thenReturn(List.of(suite));
        when(roomAvailabilityCalculator.countAvailableRooms(List.of(suite.getId()), CHECK_IN, CHECK_OUT))
                .thenReturn(Map.of(suite.getId(), 0L));

        PageResponse<AvailableRoomTypeResponse> responses = service.search(CHECK_IN, CHECK_OUT, 2, 0, null, 0, 6);

        assertEquals(0, responses.totalElements());
        assertEquals(0, responses.totalPages());
    }

    @Test
    void rejectsPastCheckInDate() {
        assertThrows(
                RoomValidationException.class,
                () -> service.search(LocalDate.of(2026, 8, 7), CHECK_OUT, 2, 0, null, 0, 6));
    }

    @Test
    void rejectsCheckOutThatIsNotAfterCheckIn() {
        assertThrows(RoomValidationException.class, () -> service.search(CHECK_IN, CHECK_IN, 2, 0, null, 0, 6));
    }

    @Test
    void rejectsInvalidGuestCounts() {
        assertThrows(RoomValidationException.class, () -> service.search(CHECK_IN, CHECK_OUT, 0, 0, null, 0, 6));
        assertThrows(RoomValidationException.class, () -> service.search(CHECK_IN, CHECK_OUT, 1, -1, null, 0, 6));
    }

    @Test
    void allowsCheckInToday() {
        LocalDate today = LocalDate.of(2026, 8, 8);
        RoomType suite = roomType("SUITE", "Suite", 4);
        when(roomTypeRepository.findAllByStatusOrderByNameAsc(RoomTypeStatus.ACTIVE))
                .thenReturn(List.of(suite));
        when(roomAvailabilityCalculator.countAvailableRooms(List.of(suite.getId()), today, CHECK_OUT))
                .thenReturn(Map.of(suite.getId(), 1L));

        PageResponse<AvailableRoomTypeResponse> responses = service.search(today, CHECK_OUT, 2, 0, null, 0, 6);

        assertEquals(1, responses.content().size());
    }

    @Test
    void excludesRoomTypesThatDoNotSupportTheAdultAndChildMix() {
        RoomType suite = roomType("SUITE", "Suite", 4);
        when(roomTypeRepository.findAllByStatusOrderByNameAsc(RoomTypeStatus.ACTIVE))
                .thenReturn(List.of(suite));

        PageResponse<AvailableRoomTypeResponse> responses = service.search(CHECK_IN, CHECK_OUT, 3, 2, null, 0, 6);

        assertEquals(0, responses.content().size());
        verify(roomAvailabilityCalculator, never()).countAvailableRooms(List.of(suite.getId()), CHECK_IN, CHECK_OUT);
    }

    @Test
    void paginatesAfterRemovingUnavailableRoomTypesAndPricesOnlyCurrentPage() {
        RoomType unavailable = roomType("ROOM-0", "A - Hết phòng", 4);
        RoomType firstAvailable = roomType("ROOM-1", "B - Deluxe", 4);
        RoomType secondAvailable = roomType("ROOM-2", "C - Suite", 4);
        when(roomTypeRepository.findAllByStatusOrderByNameAsc(RoomTypeStatus.ACTIVE))
                .thenReturn(List.of(unavailable, firstAvailable, secondAvailable));
        when(roomAvailabilityCalculator.countAvailableRooms(
                        List.of(unavailable.getId(), firstAvailable.getId(), secondAvailable.getId()),
                        CHECK_IN,
                        CHECK_OUT))
                .thenReturn(Map.of(
                        unavailable.getId(), 0L,
                        firstAvailable.getId(), 1L,
                        secondAvailable.getId(), 2L));

        PageResponse<AvailableRoomTypeResponse> response = service.search(CHECK_IN, CHECK_OUT, 2, 0, null, 1, 1);

        assertEquals(2, response.totalElements());
        assertEquals(2, response.totalPages());
        assertEquals("ROOM-2", response.content().get(0).code());
        verify(pricingService, never()).calculateRoomPrice(unavailable, CHECK_IN, CHECK_OUT);
        verify(pricingService, never()).calculateRoomPrice(firstAvailable, CHECK_IN, CHECK_OUT);
        verify(pricingService).calculateRoomPrice(secondAvailable, CHECK_IN, CHECK_OUT);
    }

    @Test
    void filtersAvailableSearchByRoomTypeIdWhenProvided() {
        RoomType deluxe = roomType("DELUXE", "Deluxe", 4);
        RoomType suite = roomType("SUITE", "Suite", 4);
        when(roomTypeRepository.findAllByStatusOrderByNameAsc(RoomTypeStatus.ACTIVE))
                .thenReturn(List.of(deluxe, suite));
        when(roomAvailabilityCalculator.countAvailableRooms(List.of(suite.getId()), CHECK_IN, CHECK_OUT))
                .thenReturn(Map.of(suite.getId(), 1L));

        PageResponse<AvailableRoomTypeResponse> response =
                service.search(CHECK_IN, CHECK_OUT, 2, 0, suite.getId(), 0, 6);

        assertEquals(1, response.totalElements());
        assertEquals("SUITE", response.content().get(0).code());
        verify(roomAvailabilityCalculator).countAvailableRooms(List.of(suite.getId()), CHECK_IN, CHECK_OUT);
    }

    private BookingNight night(LocalDate stayDate, long pricePerNight) {
        BookingNight night = BookingNight.builder().stayDate(stayDate).build();
        night.setPricePerNight(pricePerNight);
        return night;
    }

    private RoomType roomType(String code, String name, int maxGuests) {
        return RoomType.builder()
                .id(UUID.randomUUID())
                .code(code)
                .name(name)
                .description(name + " description")
                .basePrice(2_000_000)
                .maxGuests(maxGuests)
                .capacityAdults(2)
                .capacityChildren(2)
                .status(RoomTypeStatus.ACTIVE)
                .build();
    }
}
