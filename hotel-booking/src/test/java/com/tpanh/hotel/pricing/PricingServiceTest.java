package com.tpanh.hotel.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tpanh.hotel.addon.domain.AddOnService;
import com.tpanh.hotel.booking.domain.Booking;
import com.tpanh.hotel.booking.domain.addon.BookingAddOn;
import com.tpanh.hotel.pricing.domain.AdjustmentDirection;
import com.tpanh.hotel.pricing.domain.AdjustmentType;
import com.tpanh.hotel.pricing.domain.HolidayCalendar;
import com.tpanh.hotel.pricing.domain.PriceRule;
import com.tpanh.hotel.pricing.domain.RuleType;
import com.tpanh.hotel.pricing.dto.result.PricingResult;
import com.tpanh.hotel.pricing.dto.result.StayPriceResult;
import com.tpanh.hotel.pricing.exception.PricingValidationException;
import com.tpanh.hotel.pricing.repository.HolidayCalendarRepository;
import com.tpanh.hotel.pricing.repository.PriceRuleRepository;
import com.tpanh.hotel.pricing.service.impl.PricingServiceImpl;
import com.tpanh.hotel.room.domain.RoomType;
import com.tpanh.hotel.room.domain.RoomTypeStatus;
import com.tpanh.hotel.room.exception.RoomTypeNotFoundException;
import com.tpanh.hotel.room.repository.RoomTypeRepository;
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

@ExtendWith(MockitoExtension.class)
class PricingServiceTest {

    @Mock
    private PriceRuleRepository priceRuleRepository;

    @Mock
    private HolidayCalendarRepository holidayCalendarRepository;

    @Mock
    private RoomTypeRepository roomTypeRepository;

    private PricingServiceImpl pricingService;

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-18T00:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        pricingService =
                new PricingServiceImpl(priceRuleRepository, holidayCalendarRepository, roomTypeRepository, CLOCK);
    }

    @Test
    void calculatesOneNightStayFromRoomTypeId() {
        UUID roomTypeId = UUID.randomUUID();
        RoomType roomType = activeRoomType(roomTypeId, 1_000);
        when(roomTypeRepository.findByIdAndStatus(roomTypeId, RoomTypeStatus.ACTIVE))
                .thenReturn(java.util.Optional.of(roomType));
        when(priceRuleRepository.findActiveRulesForStay(
                        roomTypeId, LocalDate.of(2026, 8, 20), LocalDate.of(2026, 8, 21)))
                .thenReturn(List.of());

        StayPriceResult result =
                pricingService.calculateStayPrice(roomTypeId, LocalDate.of(2026, 8, 20), LocalDate.of(2026, 8, 21));

        assertEquals(1, result.numberOfNights());
        assertEquals(
                List.of(LocalDate.of(2026, 8, 20)),
                result.nightlyPrices().stream().map(night -> night.date()).toList());
        assertEquals(1_000, result.totalPrice());
    }

    @Test
    void calculatesStayAcrossMonthBoundary() {
        StayPriceResult result = calculateBasePriceStay(LocalDate.of(2026, 8, 30), LocalDate.of(2026, 9, 2), 700);

        assertEquals(3, result.numberOfNights());
        assertEquals(
                List.of(LocalDate.of(2026, 8, 30), LocalDate.of(2026, 8, 31), LocalDate.of(2026, 9, 1)),
                result.nightlyPrices().stream().map(night -> night.date()).toList());
        assertEquals(2_100, result.totalPrice());
    }

    @Test
    void calculatesStayAcrossYearBoundary() {
        StayPriceResult result = calculateBasePriceStay(LocalDate.of(2026, 12, 30), LocalDate.of(2027, 1, 2), 900);

        assertEquals(3, result.numberOfNights());
        assertEquals(
                List.of(LocalDate.of(2026, 12, 30), LocalDate.of(2026, 12, 31), LocalDate.of(2027, 1, 1)),
                result.nightlyPrices().stream().map(night -> night.date()).toList());
        assertEquals(2_700, result.totalPrice());
    }

    @Test
    void acceptsZeroBasePriceWhenAllowedByTheDomain() {
        StayPriceResult result = calculateBasePriceStay(LocalDate.of(2026, 8, 20), LocalDate.of(2026, 8, 21), 0);

        assertEquals(1, result.numberOfNights());
        assertEquals(0, result.nightlyPrices().get(0).finalPrice());
        assertEquals(0, result.totalPrice());
    }

    @Test
    void rejectsEqualCheckInAndCheckOut() {
        LocalDate stayDate = LocalDate.of(2026, 8, 20);

        assertThrows(
                PricingValidationException.class,
                () -> pricingService.calculateStayPrice(UUID.randomUUID(), stayDate, stayDate));
    }

    @Test
    void rejectsCheckInAfterCheckOut() {
        assertThrows(
                PricingValidationException.class,
                () -> pricingService.calculateStayPrice(
                        UUID.randomUUID(), LocalDate.of(2026, 8, 21), LocalDate.of(2026, 8, 20)));
    }

    @Test
    void rejectsMissingRequiredInput() {
        assertThrows(
                PricingValidationException.class,
                () -> pricingService.calculateStayPrice(null, LocalDate.of(2026, 8, 20), LocalDate.of(2026, 8, 21)));
        assertThrows(
                PricingValidationException.class,
                () -> pricingService.calculateStayPrice(UUID.randomUUID(), null, LocalDate.of(2026, 8, 21)));
        assertThrows(
                PricingValidationException.class,
                () -> pricingService.calculateStayPrice(UUID.randomUUID(), LocalDate.of(2026, 8, 20), null));
    }

    @Test
    void rejectsPastCheckIn() {
        assertThrows(
                PricingValidationException.class,
                () -> pricingService.calculateStayPrice(
                        UUID.randomUUID(), LocalDate.of(2026, 8, 17), LocalDate.of(2026, 8, 18)));
    }

    @Test
    void rejectsUnknownOrInactiveRoomType() {
        UUID roomTypeId = UUID.randomUUID();
        when(roomTypeRepository.findByIdAndStatus(roomTypeId, RoomTypeStatus.ACTIVE))
                .thenReturn(java.util.Optional.empty());

        assertThrows(
                RoomTypeNotFoundException.class,
                () -> pricingService.calculateStayPrice(
                        roomTypeId, LocalDate.of(2026, 8, 20), LocalDate.of(2026, 8, 21)));
    }

    @Test
    void appliesRuleOnlyToMatchingNightAndTotalsNightlyPrices() {
        UUID roomTypeId = UUID.randomUUID();
        RoomType roomType = activeRoomType(roomTypeId, 1_000);
        LocalDate checkIn = LocalDate.of(2026, 8, 21);
        LocalDate checkOut = LocalDate.of(2026, 8, 24);
        when(roomTypeRepository.findByIdAndStatus(roomTypeId, RoomTypeStatus.ACTIVE))
                .thenReturn(java.util.Optional.of(roomType));
        when(priceRuleRepository.findActiveRulesForStay(roomTypeId, checkIn, checkOut))
                .thenReturn(List.of(percentageRule(RuleType.WEEKEND, 20, 1, checkIn, checkIn.plusDays(1))));

        StayPriceResult result = pricingService.calculateStayPrice(roomTypeId, checkIn, checkOut);

        assertEquals(
                List.of(1_000L, 1_200L, 1_000L),
                result.nightlyPrices().stream().map(night -> night.finalPrice()).toList());
        assertEquals(
                result.nightlyPrices().stream()
                        .mapToLong(night -> night.finalPrice())
                        .sum(),
                result.totalPrice());
        verify(priceRuleRepository).findActiveRulesForStay(roomTypeId, checkIn, checkOut);
    }

    @Test
    void rejectsInvalidBasePriceInsteadOfUsingItSilently() {
        RoomType roomType = RoomType.builder()
                .id(UUID.randomUUID())
                .basePrice(-1)
                .maxGuests(1)
                .build();

        assertThrows(
                PricingValidationException.class, () -> pricingService.calculateRoomPrice(roomType, date(1), date(2)));
    }

    @Test
    void rejectsIncompletePriceRuleConfiguration() {
        RoomType roomType = RoomType.builder()
                .id(UUID.randomUUID())
                .basePrice(1_000)
                .maxGuests(1)
                .build();
        when(priceRuleRepository.findActiveRulesForStay(roomType.getId(), date(1), date(2)))
                .thenReturn(
                        List.of(PriceRule.builder().ruleType(RuleType.WEEKEND).build()));

        assertThrows(
                PricingValidationException.class, () -> pricingService.calculateRoomPrice(roomType, date(1), date(2)));
    }

    @Test
    void calculatesBasePriceForEveryNightWhenThereAreNoRules() {
        Booking booking = booking(1_000_000, date(1), date(4), 2, 1);
        when(priceRuleRepository.findActiveRulesForStay(any(), any(), any())).thenReturn(List.of());

        PricingResult result = pricingService.calculatePrice(booking);

        assertEquals(3_000_000, result.roomPricingResult().roomAmount());
        assertEquals(0, result.roomPricingResult().roomDiscount());
        assertEquals(3_000_000, result.roomPricingResult().finalRoomAmount());
        assertEquals(3, result.roomPricingResult().nights().size());
        assertEquals(
                List.of(date(1), date(2), date(3)),
                result.roomPricingResult().nights().stream()
                        .map(night -> night.getStayDate())
                        .toList());
    }

    @Test
    void selectsOneBaseModifierAndBestPromotion() {
        Booking booking = booking(1_000, date(1), date(2), 2, 0);
        when(priceRuleRepository.findActiveRulesForStay(any(), any(), any()))
                .thenReturn(List.of(
                        fixedRule(RuleType.SEASONAL, 300, 1),
                        fixedRule(RuleType.HOLIDAY, 500, 2),
                        percentageRule(RuleType.PROMOTION, 10, 3),
                        fixedRule(RuleType.PROMOTION, 50, 4)));
        when(holidayCalendarRepository.findActiveForStay(any(), any())).thenReturn(List.of());

        PricingResult result = pricingService.calculatePrice(booking);

        assertEquals(1_300, result.roomPricingResult().roomAmount());
        assertEquals(130, result.roomPricingResult().roomDiscount());
        assertEquals(1_170, result.roomPricingResult().finalRoomAmount());
        assertEquals(
                2, result.roomPricingResult().nights().get(0).getAppliedRules().size());
    }

    @Test
    void holidayModifierOverridesWeekendModifier() {
        Booking booking = booking(1_000, LocalDate.of(2026, 9, 4), LocalDate.of(2026, 9, 7), 1, 0);
        PriceRule weekend = percentageRule(RuleType.WEEKEND, 20, 1, LocalDate.of(2026, 9, 4), LocalDate.of(2026, 9, 6));
        PriceRule holiday = fixedRule(RuleType.HOLIDAY, 500, 1, LocalDate.of(2026, 9, 5), LocalDate.of(2026, 9, 5));
        when(priceRuleRepository.findActiveRulesForStay(any(), any(), any())).thenReturn(List.of(weekend, holiday));
        when(holidayCalendarRepository.findActiveForStay(any(), any()))
                .thenReturn(List.of(HolidayCalendar.builder()
                        .name("Hotel event")
                        .startDate(LocalDate.of(2026, 9, 5))
                        .endDate(LocalDate.of(2026, 9, 5))
                        .active(true)
                        .build()));

        PricingResult result = pricingService.calculatePrice(booking);

        assertEquals(
                List.of(1_000L, 1_500L, 1_200L),
                result.roomPricingResult().nights().stream()
                        .map(night -> night.getPricePerNight())
                        .toList());
    }

    @Test
    void neverProducesNegativeNightPriceWhenDiscountExceedsGrossPrice() {
        Booking booking = booking(100, date(1), date(2), 1, 0);
        when(priceRuleRepository.findActiveRulesForStay(any(), any(), any()))
                .thenReturn(List.of(fixedRule(RuleType.PROMOTION, 500, 1)));

        PricingResult result = pricingService.calculatePrice(booking);

        assertEquals(100, result.roomPricingResult().roomDiscount());
        assertEquals(0, result.roomPricingResult().finalRoomAmount());
        assertEquals(0, result.roomPricingResult().nights().get(0).getPricePerNight());
        assertEquals(
                100,
                result.roomPricingResult()
                        .nights()
                        .get(0)
                        .getAppliedRules()
                        .get(0)
                        .getAdjustmentAmount());
    }

    @Test
    void appliesSeasonalDiscountBeforePublicPromotion() {
        Booking booking = booking(1_000, date(1), date(2), 1, 0);
        when(priceRuleRepository.findActiveRulesForStay(any(), any(), any()))
                .thenReturn(List.of(
                        percentageRule(RuleType.SEASONAL, AdjustmentDirection.DISCOUNT, 20, 1),
                        percentageRule(RuleType.PROMOTION, 10, 1)));

        PricingResult result = pricingService.calculatePrice(booking);

        assertEquals(1_000, result.roomPricingResult().roomAmount());
        assertEquals(280, result.roomPricingResult().roomDiscount());
        assertEquals(720, result.roomPricingResult().finalRoomAmount());
        assertEquals(
                2, result.roomPricingResult().nights().get(0).getAppliedRules().size());
    }

    @Test
    void selectsTheSameBaseModifierWhenEqualPriorityRulesArriveInDifferentOrders() {
        Booking booking = booking(1_000, date(1), date(2), 1, 0);
        PriceRule smallerSurcharge = fixedRule(RuleType.SEASONAL, 100, 10);
        PriceRule largerSurcharge = fixedRule(RuleType.SEASONAL, 200, 10);

        when(priceRuleRepository.findActiveRulesForStay(any(), any(), any()))
                .thenReturn(List.of(smallerSurcharge, largerSurcharge))
                .thenReturn(List.of(largerSurcharge, smallerSurcharge));

        PricingResult firstResult = pricingService.calculatePrice(booking);
        PricingResult secondResult = pricingService.calculatePrice(booking);

        assertEquals(1_200, firstResult.roomPricingResult().finalRoomAmount());
        assertEquals(
                firstResult.roomPricingResult().finalRoomAmount(),
                secondResult.roomPricingResult().finalRoomAmount());
    }

    @Test
    void appliesDifferentRulesToTheirMatchingNights() {
        Booking booking = booking(1_000, date(1), date(3), 1, 0);
        when(priceRuleRepository.findActiveRulesForStay(any(), any(), any()))
                .thenReturn(List.of(
                        percentageRule(RuleType.SEASONAL, 20, 1, date(1), date(1)),
                        fixedRule(RuleType.PROMOTION, 100, 1, date(2), date(2))));

        PricingResult result = pricingService.calculatePrice(booking);

        assertEquals(1_200, result.roomPricingResult().nights().get(0).getPricePerNight());
        assertEquals(900, result.roomPricingResult().nights().get(1).getPricePerNight());
        assertEquals(2_100, result.totalAmount());
    }

    @Test
    void calculatesAddOnsFromSnapshottedUnitPriceAndQuantity() {
        Booking booking = booking(100, date(1), date(4), 2, 1);
        BookingAddOn breakfast = addOn(200, 2);
        BookingAddOn extraBed = addOn(50, 2);
        BookingAddOn pickup = addOn(30, 2);
        booking.addAddOn(breakfast);
        booking.addAddOn(extraBed);
        booking.addAddOn(pickup);
        when(priceRuleRepository.findActiveRulesForStay(any(), any(), any())).thenReturn(List.of());

        PricingResult result = pricingService.calculatePrice(booking);

        assertEquals(400, breakfast.getTotalPrice());
        assertEquals(100, extraBed.getTotalPrice());
        assertEquals(60, pickup.getTotalPrice());
        assertEquals(560, result.addOnAmount());
        assertEquals(860, result.totalAmount());
        assertEquals(200, breakfast.getUnitPrice());
    }

    @Test
    void returnsZeroAddOnAmountWhenNoAddOnsWereSelected() {
        Booking booking = booking(500, date(1), date(2), 1, 0);
        when(priceRuleRepository.findActiveRulesForStay(any(), any(), any())).thenReturn(List.of());

        PricingResult result = pricingService.calculatePrice(booking);

        assertEquals(0, result.addOnAmount());
        assertEquals(result.roomPricingResult().finalRoomAmount(), result.totalAmount());
    }

    @Test
    void capsPersistedSeasonalDiscountAtRoomPrice() {
        var rule = PriceRule.builder()
                .ruleType(RuleType.SEASONAL)
                .adjustmentDirection(AdjustmentDirection.DISCOUNT)
                .adjustmentType(AdjustmentType.FIXED)
                .adjustmentValue(800000)
                .startDate(date(1))
                .endDate(date(30))
                .build();
        when(priceRuleRepository.findActiveRulesForStay(any(), any(), any())).thenReturn(List.of(rule));
        var result = pricingService
                .calculatePrice(booking(500000, date(1), date(2), 1, 0))
                .roomPricingResult();
        assertEquals(500000, result.roomDiscount());
        assertEquals(0, result.finalRoomAmount());
        var applied = result.nights().get(0).getAppliedRules().iterator().next();
        assertEquals(500000, applied.getAdjustmentAmount());
        var response = org.mapstruct.factory.Mappers.getMapper(com.tpanh.hotel.room.mapper.RoomTypeMapper.class)
                .toNightlyPriceAdjustmentResponse(applied);
        assertEquals(AdjustmentDirection.DISCOUNT, response.adjustmentDirection());
        assertEquals(500000, response.amount());
    }

    private Booking booking(long basePrice, LocalDate checkIn, LocalDate checkOut, int adults, int children) {
        RoomType roomType = RoomType.builder()
                .id(UUID.randomUUID())
                .basePrice(basePrice)
                .maxGuests(10)
                .build();
        return Booking.builder()
                .roomType(roomType)
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .adultCount(adults)
                .childCount(children)
                .build();
    }

    private StayPriceResult calculateBasePriceStay(LocalDate checkIn, LocalDate checkOut, long basePrice) {
        UUID roomTypeId = UUID.randomUUID();
        RoomType roomType = activeRoomType(roomTypeId, basePrice);
        when(roomTypeRepository.findByIdAndStatus(roomTypeId, RoomTypeStatus.ACTIVE))
                .thenReturn(java.util.Optional.of(roomType));
        when(priceRuleRepository.findActiveRulesForStay(any(), any(), any())).thenReturn(List.of());
        return pricingService.calculateStayPrice(roomTypeId, checkIn, checkOut);
    }

    private RoomType activeRoomType(UUID id, long basePrice) {
        return RoomType.builder()
                .id(id)
                .code("DLX")
                .name("Deluxe")
                .basePrice(basePrice)
                .maxGuests(2)
                .capacityAdults(2)
                .status(RoomTypeStatus.ACTIVE)
                .build();
    }

    private BookingAddOn addOn(long price, int quantity) {
        AddOnService service =
                AddOnService.builder().id(UUID.randomUUID()).price(price).build();
        BookingAddOn addOn =
                BookingAddOn.builder().addOnService(service).quantity(quantity).build();
        addOn.updateSnapshot(quantity, price);
        return addOn;
    }

    private PriceRule fixedRule(RuleType type, long amount, int priority) {
        return fixedRule(type, amount, priority, date(1), date(30));
    }

    private PriceRule fixedRule(RuleType type, long amount, int priority, LocalDate startDate, LocalDate endDate) {
        return PriceRule.builder()
                .ruleType(type)
                .adjustmentType(AdjustmentType.FIXED)
                .adjustmentValue(amount)
                .adjustmentDirection(defaultDirection(type))
                .priority(priority)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }

    private PriceRule percentageRule(RuleType type, long percent, int priority) {
        return percentageRule(type, percent, priority, date(1), date(30));
    }

    private PriceRule percentageRule(
            RuleType type, long percent, int priority, LocalDate startDate, LocalDate endDate) {
        return percentageRule(type, defaultDirection(type), percent, priority, startDate, endDate);
    }

    private PriceRule percentageRule(RuleType type, AdjustmentDirection direction, long percent, int priority) {
        return percentageRule(type, direction, percent, priority, date(1), date(30));
    }

    private PriceRule percentageRule(
            RuleType type,
            AdjustmentDirection direction,
            long percent,
            int priority,
            LocalDate startDate,
            LocalDate endDate) {
        return PriceRule.builder()
                .ruleType(type)
                .adjustmentType(AdjustmentType.PERCENTAGE)
                .adjustmentValue(percent)
                .adjustmentDirection(direction)
                .priority(priority)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }

    private AdjustmentDirection defaultDirection(RuleType ruleType) {
        return ruleType == RuleType.PROMOTION ? AdjustmentDirection.DISCOUNT : AdjustmentDirection.INCREASE;
    }

    private LocalDate date(int day) {
        return LocalDate.of(2026, 9, day);
    }
}
