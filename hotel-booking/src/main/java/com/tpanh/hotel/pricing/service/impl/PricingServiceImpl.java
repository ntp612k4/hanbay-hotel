package com.tpanh.hotel.pricing.service.impl;

import com.tpanh.hotel.booking.domain.Booking;
import com.tpanh.hotel.booking.domain.BookingNight;
import com.tpanh.hotel.booking.domain.addon.BookingAddOn;
import com.tpanh.hotel.booking.domain.pricing.BookingNightPriceRule;
import com.tpanh.hotel.pricing.domain.AdjustmentDirection;
import com.tpanh.hotel.pricing.domain.AdjustmentType;
import com.tpanh.hotel.pricing.domain.HolidayCalendar;
import com.tpanh.hotel.pricing.domain.PriceRule;
import com.tpanh.hotel.pricing.domain.PriceRulePolicy;
import com.tpanh.hotel.pricing.domain.RuleType;
import com.tpanh.hotel.pricing.dto.result.NightlyPriceResult;
import com.tpanh.hotel.pricing.dto.result.PricingResult;
import com.tpanh.hotel.pricing.dto.result.RoomPricingResult;
import com.tpanh.hotel.pricing.dto.result.RoomTypePricingResult;
import com.tpanh.hotel.pricing.dto.result.StayPriceResult;
import com.tpanh.hotel.pricing.exception.PricingValidationException;
import com.tpanh.hotel.pricing.repository.HolidayCalendarRepository;
import com.tpanh.hotel.pricing.repository.PriceRuleRepository;
import com.tpanh.hotel.pricing.service.PricingService;
import com.tpanh.hotel.room.domain.RoomType;
import com.tpanh.hotel.room.domain.RoomTypeStatus;
import com.tpanh.hotel.room.exception.RoomTypeNotFoundException;
import com.tpanh.hotel.room.repository.RoomTypeRepository;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PricingServiceImpl implements PricingService {

    private final PriceRuleRepository priceRuleRepository;
    private final HolidayCalendarRepository holidayCalendarRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final Clock clock;

    @Override
    public PricingResult calculatePrice(Booking booking) {

        RoomPricingResult roomPricingResult =
                calculateRoomPrice(booking.getRoomType(), booking.getCheckInDate(), booking.getCheckOutDate());
        long totalAddOnAmount = calculateAddOnPrice(booking);
        long grandTotal = roomPricingResult.finalRoomAmount() + totalAddOnAmount;

        return new PricingResult(roomPricingResult, totalAddOnAmount, grandTotal);
    }

    @Override
    public RoomPricingResult calculateRoomPrice(RoomType roomType, LocalDate checkIn, LocalDate checkOut) {
        validatePricingInput(roomType, checkIn, checkOut);

        try {
            return calculateValidatedRoomPrice(roomType, checkIn, checkOut);
        } catch (ArithmeticException exception) {
            throw new PricingValidationException("Giá trị tính toán vượt quá phạm vi được hỗ trợ.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public StayPriceResult calculateStayPrice(UUID roomTypeId, LocalDate checkIn, LocalDate checkOut) {
        if (roomTypeId == null) {
            throw new PricingValidationException("ID hạng phòng không được để trống.");
        }
        validateStayDates(checkIn, checkOut);

        RoomType roomType = roomTypeRepository
                .findByIdAndStatus(roomTypeId, RoomTypeStatus.ACTIVE)
                .orElseThrow(() -> new RoomTypeNotFoundException("Không tìm thấy hạng phòng đang kinh doanh."));

        RoomPricingResult pricing = calculateRoomPrice(roomType, checkIn, checkOut);
        List<NightlyPriceResult> nightlyPrices = pricing.nights().stream()
                .map(night -> new NightlyPriceResult(night.getStayDate(), night.getPricePerNight()))
                .toList();
        long totalPrice;
        try {
            totalPrice = nightlyPrices.stream()
                    .mapToLong(NightlyPriceResult::finalPrice)
                    .reduce(0L, Math::addExact);
        } catch (ArithmeticException exception) {
            throw new PricingValidationException("Tổng giá lưu trú vượt quá phạm vi được hỗ trợ.");
        }

        if (nightlyPrices.isEmpty() || totalPrice != pricing.finalRoomAmount()) {
            throw new PricingValidationException("Kết quả tính giá lưu trú không nhất quán.");
        }

        return new StayPriceResult(
                new RoomTypePricingResult(roomType.getId(), roomType.getCode(), roomType.getName()),
                checkIn,
                checkOut,
                nightlyPrices.size(),
                nightlyPrices,
                totalPrice);
    }

    private RoomPricingResult calculateValidatedRoomPrice(RoomType roomType, LocalDate checkIn, LocalDate checkOut) {

        LocalDate current = checkIn;

        long totalRoomAmount = 0; // Tổng giá trị phòng (Giá gốc + Phụ thu)
        long totalRoomDiscount = 0; // Tổng tiền khuyến mãi

        List<BookingNight> bookingNights = new ArrayList<>();
        final long basePrice = roomType.getBasePrice();
        List<PriceRule> applicableRules =
                priceRuleRepository.findActiveRulesForStay(roomType.getId(), checkIn, checkOut);
        applicableRules.forEach(this::validatePriceRule);
        List<HolidayCalendar> holidays = loadHolidaysIfRequired(applicableRules, checkIn, checkOut);

        while (current.isBefore(checkOut)) {
            LocalDate stayDate = current;
            List<PriceRule> priceRules = applicableRules.stream()
                    .filter(rule -> appliesToDate(rule, stayDate, holidays))
                    .toList();

            PriceRule baseModifier = selectBaseModifier(priceRules, basePrice);

            BookingNight currentBookingNight =
                    BookingNight.builder().stayDate(current).build();

            long currentNightSurcharge = 0;
            long currentNightSeasonalDiscount = 0;
            long currentNightPromotionDiscount = 0;

            // 1. Apply one period modifier (holiday > weekend > seasonal).
            if (baseModifier != null) {
                long adjustmentAmount = calculateAdjustment(basePrice, baseModifier);
                if (baseModifier.getAdjustmentDirection() == AdjustmentDirection.DISCOUNT) {
                    adjustmentAmount = Math.min(adjustmentAmount, basePrice);
                    currentNightSeasonalDiscount = adjustmentAmount;
                } else {
                    currentNightSurcharge = adjustmentAmount;
                }
                currentBookingNight.addAppliedRule(BookingNightPriceRule.builder()
                        .priceRule(baseModifier)
                        .adjustmentAmount(adjustmentAmount)
                        .build());
            }

            // 2. Apply the public promotion after the period modifier.
            long nightGrossPrice = Math.addExact(basePrice, currentNightSurcharge);
            long priceAfterPeriodModifier = Math.subtractExact(nightGrossPrice, currentNightSeasonalDiscount);
            PriceRule bestPromotion = selectBestPromotion(priceRules, priceAfterPeriodModifier);

            if (bestPromotion != null) {
                currentNightPromotionDiscount = Math.min(
                        calculateAdjustment(priceAfterPeriodModifier, bestPromotion), priceAfterPeriodModifier);
                currentBookingNight.addAppliedRule(BookingNightPriceRule.builder()
                        .priceRule(bestPromotion)
                        .adjustmentAmount(currentNightPromotionDiscount)
                        .build());
            }

            // 3. Final price and bill totals.
            long currentNightDiscount = Math.addExact(currentNightSeasonalDiscount, currentNightPromotionDiscount);
            long finalNightPrice = Math.subtractExact(priceAfterPeriodModifier, currentNightPromotionDiscount);
            if (finalNightPrice < 0) {
                throw new PricingValidationException("Giá cuối cùng của một đêm không được âm.");
            }

            currentBookingNight.setPricePerNight(finalNightPrice);
            bookingNights.add(currentBookingNight);

            // 4. CỘNG DỒN VÀO TỔNG BILL (RoomPricingResult)
            totalRoomAmount = Math.addExact(totalRoomAmount, nightGrossPrice);
            totalRoomDiscount = Math.addExact(totalRoomDiscount, currentNightDiscount);

            // Tiến tới ngày tiếp theo
            current = current.plusDays(1);
        }

        long finalTotalRoomAmount =
                bookingNights.stream().mapToLong(BookingNight::getPricePerNight).reduce(0L, Math::addExact);
        return new RoomPricingResult(bookingNights, totalRoomAmount, totalRoomDiscount, finalTotalRoomAmount);
    }

    private List<HolidayCalendar> loadHolidaysIfRequired(List<PriceRule> rules, LocalDate checkIn, LocalDate checkOut) {
        boolean holidayRuleConfigured = rules.stream().anyMatch(rule -> rule.getRuleType() == RuleType.HOLIDAY);
        return holidayRuleConfigured ? holidayCalendarRepository.findActiveForStay(checkIn, checkOut) : List.of();
    }

    private boolean appliesToDate(PriceRule rule, LocalDate stayDate, List<HolidayCalendar> holidays) {
        if (stayDate.isBefore(rule.getStartDate()) || stayDate.isAfter(rule.getEndDate())) {
            return false;
        }
        return switch (rule.getRuleType()) {
            case WEEKEND -> isWeekend(stayDate);
            case HOLIDAY -> holidays.stream().anyMatch(holiday -> holiday.includes(stayDate));
            case SEASONAL, PROMOTION -> true;
            case BASE -> false;
        };
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    private PriceRule selectBaseModifier(List<PriceRule> priceRules, long basePrice) {
        return priceRules.stream()
                .filter(rule -> rule.getRuleType() != RuleType.PROMOTION && rule.getRuleType() != RuleType.BASE)
                .max(Comparator.comparingInt(
                                (PriceRule rule) -> rule.getRuleType().priority())
                        .thenComparingInt(PriceRule::getPriority)
                        .thenComparingLong(rule -> calculateAdjustment(basePrice, rule))
                        .thenComparing(PriceRule::getId, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(PriceRule::getName, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
    }

    private PriceRule selectBestPromotion(List<PriceRule> priceRules, long basePrice) {
        return priceRules.stream()
                .filter(rule -> rule.getRuleType() == RuleType.PROMOTION)
                .max(Comparator.comparingLong((PriceRule rule) -> calculateAdjustment(basePrice, rule))
                        .thenComparingInt(PriceRule::getPriority)
                        .thenComparing(PriceRule::getId, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(PriceRule::getName, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
    }

    private void validatePricingInput(RoomType roomType, LocalDate checkIn, LocalDate checkOut) {
        if (roomType == null || roomType.getId() == null) {
            throw new PricingValidationException("Hạng phòng hợp lệ là bắt buộc để tính giá.");
        }
        validateDateRange(checkIn, checkOut);
        if (roomType.getBasePrice() < 0) {
            throw new PricingValidationException("Giá cơ bản của hạng phòng không được âm.");
        }
    }

    private void validateStayDates(LocalDate checkIn, LocalDate checkOut) {
        validateDateRange(checkIn, checkOut);
        if (checkIn.isBefore(LocalDate.now(clock))) {
            throw new PricingValidationException("Ngày nhận phòng không được ở quá khứ.");
        }
    }

    private void validateDateRange(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null) {
            throw new PricingValidationException("Ngày nhận phòng không được để trống.");
        }
        if (checkOut == null) {
            throw new PricingValidationException("Ngày trả phòng không được để trống.");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new PricingValidationException("Ngày trả phòng phải sau ngày nhận phòng.");
        }
    }

    private void validatePriceRule(PriceRule rule) {
        if (rule == null
                || rule.getRuleType() == null
                || rule.getAdjustmentType() == null
                || rule.getAdjustmentDirection() == null
                || rule.getStartDate() == null
                || rule.getEndDate() == null) {
            throw new PricingValidationException("Cấu hình quy tắc giá không đầy đủ.");
        }
        if (rule.getStartDate().isAfter(rule.getEndDate())) {
            throw new PricingValidationException("Khoảng hiệu lực của quy tắc giá không hợp lệ.");
        }
        if (rule.getAdjustmentValue() < 0) {
            throw new PricingValidationException("Giá trị điều chỉnh của quy tắc giá không được âm.");
        }
        if (!PriceRulePolicy.supportsDirection(rule.getRuleType(), rule.getAdjustmentDirection())) {
            throw new PricingValidationException(PriceRulePolicy.directionConstraintMessage(rule.getRuleType()));
        }
    }

    private long calculateAdjustment(long amount, PriceRule rule) {
        return rule.getAdjustmentType() == AdjustmentType.PERCENTAGE
                ? Math.multiplyExact(amount, rule.getAdjustmentValue()) / 100
                : rule.getAdjustmentValue();
    }

    private long calculateAddOnPrice(Booking booking) {
        // Nếu booking không có addon nào, trả về 0
        if (booking.getAddOns() == null || booking.getAddOns().isEmpty()) {
            return 0;
        }

        long totalAddOnAmount = 0;

        for (BookingAddOn bookingAddOn : booking.getAddOns()) {
            totalAddOnAmount += bookingAddOn.getTotalPrice();
        }

        return totalAddOnAmount;
    }
}
