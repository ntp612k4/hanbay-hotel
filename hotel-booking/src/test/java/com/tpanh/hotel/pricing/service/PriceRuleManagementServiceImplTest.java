package com.tpanh.hotel.pricing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tpanh.hotel.common.exception.RequestValidationException;
import com.tpanh.hotel.pricing.domain.AdjustmentDirection;
import com.tpanh.hotel.pricing.domain.AdjustmentType;
import com.tpanh.hotel.pricing.domain.PriceRule;
import com.tpanh.hotel.pricing.domain.RuleType;
import com.tpanh.hotel.pricing.exception.PriceRuleConflictException;
import com.tpanh.hotel.pricing.exception.PriceRuleNotFoundException;
import com.tpanh.hotel.pricing.repository.PriceRuleRepository;
import com.tpanh.hotel.pricing.service.impl.PriceRuleManagementServiceImpl;
import com.tpanh.hotel.pricing.service.model.CreatePriceRuleCommand;
import com.tpanh.hotel.pricing.service.model.PriceRuleSearchCriteria;
import com.tpanh.hotel.pricing.service.model.UpdatePriceRuleCommand;
import com.tpanh.hotel.pricing.service.model.UpdatePriceRuleStatusCommand;
import com.tpanh.hotel.room.domain.RoomType;
import com.tpanh.hotel.room.domain.RoomTypeStatus;
import com.tpanh.hotel.room.exception.RoomTypeNotFoundException;
import com.tpanh.hotel.room.repository.RoomTypeRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class PriceRuleManagementServiceImplTest {

    private static final UUID ROOM_TYPE_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID PRICE_RULE_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final LocalDate START_DATE = LocalDate.of(2026, 9, 1);
    private static final LocalDate END_DATE = LocalDate.of(2026, 12, 31);

    @Mock
    private PriceRuleRepository priceRuleRepository;

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @Test
    void findAllShouldNormalizeCriteriaAndMapPage() {
        RoomType deluxe = activeRoomType();
        PriceRule priceRule = priceRule(deluxe, true);
        when(priceRuleRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(priceRule), PageRequest.of(1, 10, Sort.by(Sort.Direction.ASC, "name")), 21));

        var result = service()
                .findAll(new PriceRuleSearchCriteria(
                        1, 10, "  weekend%_vip\\  ", ROOM_TYPE_ID, RuleType.WEEKEND, true, "name", "ASC"));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(priceRuleRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("name").getDirection())
                .isEqualTo(Sort.Direction.ASC);
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("id").getDirection())
                .isEqualTo(Sort.Direction.ASC);
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.totalElements()).isEqualTo(21);
        assertThat(result.content()).extracting(item -> item.name()).containsExactly("Weekend surcharge");
        assertThat(result.content().get(0).roomType().code()).isEqualTo("DLX");
    }

    @Test
    void findAllShouldUseContractDefaults() {
        when(priceRuleRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service().findAll(new PriceRuleSearchCriteria(0, 20, null, null, null, null, null, null));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(priceRuleRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("startDate").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void findAllShouldRejectUnsupportedSortAndBaseRuleType() {
        assertThatThrownBy(() ->
                        service().findAll(new PriceRuleSearchCriteria(0, 20, null, null, null, null, "id", "asc")))
                .isInstanceOf(RequestValidationException.class)
                .extracting("field")
                .isEqualTo("sort");

        assertThatThrownBy(() -> service()
                        .findAll(new PriceRuleSearchCriteria(0, 20, null, null, RuleType.BASE, null, "name", "asc")))
                .isInstanceOf(RequestValidationException.class)
                .extracting("field")
                .isEqualTo("ruleType");

        verify(priceRuleRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void findByIdShouldReturnInactiveRuleForManagement() {
        PriceRule inactive = priceRule(activeRoomType(), false);
        when(priceRuleRepository.findById(PRICE_RULE_ID)).thenReturn(Optional.of(inactive));

        var result = service().findById(PRICE_RULE_ID);

        assertThat(result.active()).isFalse();
        assertThat(result.roomType().id()).isEqualTo(ROOM_TYPE_ID);
    }

    @Test
    void findByIdShouldThrowContractNotFoundError() {
        when(priceRuleRepository.findById(PRICE_RULE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().findById(PRICE_RULE_ID))
                .isInstanceOf(PriceRuleNotFoundException.class)
                .extracting("errorCode")
                .isEqualTo("PRICE_RULE_NOT_FOUND");
    }

    @Test
    void createShouldNormalizeInputAndActivateRule() {
        RoomType roomType = activeRoomType();
        when(roomTypeRepository.findByIdAndStatus(ROOM_TYPE_ID, RoomTypeStatus.ACTIVE))
                .thenReturn(Optional.of(roomType));
        when(priceRuleRepository.saveAndFlush(any(PriceRule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service()
                .create(new CreatePriceRuleCommand(
                        ROOM_TYPE_ID,
                        "  Weekend surcharge  ",
                        RuleType.WEEKEND,
                        AdjustmentType.PERCENTAGE,
                        20L,
                        START_DATE,
                        END_DATE,
                        10));

        ArgumentCaptor<PriceRule> entityCaptor = ArgumentCaptor.forClass(PriceRule.class);
        verify(priceRuleRepository).saveAndFlush(entityCaptor.capture());
        PriceRule saved = entityCaptor.getValue();
        assertThat(saved.getRoomType()).isSameAs(roomType);
        assertThat(saved.getName()).isEqualTo("Weekend surcharge");
        assertThat(saved.getRuleType()).isEqualTo(RuleType.WEEKEND);
        assertThat(saved.getAdjustmentType()).isEqualTo(AdjustmentType.PERCENTAGE);
        assertThat(saved.getAdjustmentValue()).isEqualTo(20);
        assertThat(saved.getAdjustmentDirection()).isEqualTo(AdjustmentDirection.INCREASE);
        assertThat(saved.getStartDate()).isEqualTo(START_DATE);
        assertThat(saved.getEndDate()).isEqualTo(END_DATE);
        assertThat(saved.getPriority()).isEqualTo(10);
        assertThat(saved.isActive()).isTrue();
        assertThat(result.active()).isTrue();
    }

    @Test
    void createShouldAcceptHolidayFixedAmountAboveOneHundred() {
        when(roomTypeRepository.findByIdAndStatus(ROOM_TYPE_ID, RoomTypeStatus.ACTIVE))
                .thenReturn(Optional.of(activeRoomType()));
        when(priceRuleRepository.saveAndFlush(any(PriceRule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service()
                .create(new CreatePriceRuleCommand(
                        ROOM_TYPE_ID,
                        "Holiday surcharge",
                        RuleType.HOLIDAY,
                        AdjustmentType.FIXED,
                        250_000L,
                        START_DATE,
                        END_DATE,
                        20));

        assertThat(result.adjustmentValue()).isEqualTo(250_000);
        assertThat(result.adjustmentType()).isEqualTo(AdjustmentType.FIXED);
    }

    @Test
    void createShouldAllowSeasonalDiscountAndRejectInvalidDirections() {
        when(roomTypeRepository.findByIdAndStatus(ROOM_TYPE_ID, RoomTypeStatus.ACTIVE))
                .thenReturn(Optional.of(activeRoomType()));
        when(priceRuleRepository.saveAndFlush(any(PriceRule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var seasonalDiscount = service()
                .create(new CreatePriceRuleCommand(
                        ROOM_TYPE_ID,
                        "Low season discount",
                        RuleType.SEASONAL,
                        AdjustmentType.PERCENTAGE,
                        20L,
                        AdjustmentDirection.DISCOUNT,
                        START_DATE,
                        END_DATE,
                        10));

        assertThat(seasonalDiscount.adjustmentDirection()).isEqualTo(AdjustmentDirection.DISCOUNT);

        assertThatThrownBy(() -> service()
                        .create(new CreatePriceRuleCommand(
                                ROOM_TYPE_ID,
                                "Invalid promotion",
                                RuleType.PROMOTION,
                                AdjustmentType.PERCENTAGE,
                                10L,
                                AdjustmentDirection.INCREASE,
                                START_DATE,
                                END_DATE,
                                10)))
                .isInstanceOf(RequestValidationException.class)
                .extracting("field")
                .isEqualTo("adjustmentDirection");
    }

    @Test
    void createShouldRejectInvalidBusinessValues() {
        assertThatThrownBy(() -> service().create(null))
                .isInstanceOf(RequestValidationException.class)
                .extracting("field")
                .isEqualTo("request");

        assertThatThrownBy(() -> service()
                        .create(new CreatePriceRuleCommand(
                                null,
                                "Name",
                                RuleType.WEEKEND,
                                AdjustmentType.PERCENTAGE,
                                20L,
                                START_DATE,
                                END_DATE,
                                1)))
                .isInstanceOf(RequestValidationException.class)
                .extracting("field")
                .isEqualTo("roomTypeId");

        verify(priceRuleRepository, never()).saveAndFlush(any());
    }

    @Test
    void createShouldRejectMissingOrInactiveRoomType() {
        when(roomTypeRepository.findByIdAndStatus(ROOM_TYPE_ID, RoomTypeStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().create(validCreateCommand()))
                .isInstanceOf(RoomTypeNotFoundException.class)
                .extracting("errorCode")
                .isEqualTo("ROOM_001");
    }

    @Test
    void createShouldRejectFieldValidationFailures() {
        RoomType roomType = activeRoomType();
        when(roomTypeRepository.findByIdAndStatus(ROOM_TYPE_ID, RoomTypeStatus.ACTIVE))
                .thenReturn(Optional.of(roomType));

        assertFieldError(validCreateCommandWithName(" "), "name");
        assertFieldError(validCreateCommandWithName("A"), "name");
        assertFieldError(validCreateCommandWithName("A".repeat(151)), "name");
        assertFieldError(commandWithRuleType(null), "ruleType");
        assertFieldError(commandWithRuleType(RuleType.BASE), "ruleType");
        assertFieldError(commandWithAdjustmentType(null), "adjustmentType");
        assertFieldError(commandWithAdjustmentValue(-1L), "adjustmentValue");
        assertFieldError(commandWithAdjustmentValue(101L), "adjustmentValue");
        assertFieldError(commandWithStartDate(null), "startDate");
        assertFieldError(commandWithEndDate(null), "endDate");
        assertFieldError(commandWithDateRange(END_DATE, START_DATE), "endDate");
        assertFieldError(commandWithPriority(null), "priority");
        assertFieldError(commandWithPriority(-1), "priority");
        assertFieldError(commandWithPriority(1001), "priority");

        verify(priceRuleRepository, never()).saveAndFlush(any());
    }

    @Test
    void createShouldTranslateDatabaseConflict() {
        when(roomTypeRepository.findByIdAndStatus(ROOM_TYPE_ID, RoomTypeStatus.ACTIVE))
                .thenReturn(Optional.of(activeRoomType()));
        when(priceRuleRepository.saveAndFlush(any(PriceRule.class)))
                .thenThrow(new DataIntegrityViolationException("price rule constraint"));

        assertThatThrownBy(() -> service().create(validCreateCommand()))
                .isInstanceOf(PriceRuleConflictException.class)
                .extracting("errorCode")
                .isEqualTo("PRICE_RULE_CONFLICT");
    }

    @Test
    void updateShouldChangeBusinessFieldsWithoutChangingStatus() {
        RoomType oldRoomType = activeRoomType();
        RoomType newRoomType = RoomType.builder()
                .id(UUID.fromString("10000000-0000-0000-0000-000000000002"))
                .code("STE")
                .name("Suite")
                .basePrice(2_000_000)
                .maxGuests(4)
                .capacityAdults(2)
                .capacityChildren(2)
                .status(RoomTypeStatus.ACTIVE)
                .build();
        PriceRule priceRule = priceRule(oldRoomType, false);
        when(priceRuleRepository.findByIdForUpdate(PRICE_RULE_ID)).thenReturn(Optional.of(priceRule));
        when(roomTypeRepository.findByIdAndStatus(newRoomType.getId(), RoomTypeStatus.ACTIVE))
                .thenReturn(Optional.of(newRoomType));
        when(priceRuleRepository.saveAndFlush(priceRule)).thenReturn(priceRule);

        var result = service()
                .update(
                        PRICE_RULE_ID,
                        new UpdatePriceRuleCommand(
                                newRoomType.getId(),
                                "  Holiday surcharge  ",
                                RuleType.HOLIDAY,
                                AdjustmentType.FIXED,
                                300_000L,
                                LocalDate.of(2026, 12, 24),
                                LocalDate.of(2026, 12, 26),
                                50));

        assertThat(result.roomType().code()).isEqualTo("STE");
        assertThat(result.name()).isEqualTo("Holiday surcharge");
        assertThat(result.ruleType()).isEqualTo(RuleType.HOLIDAY);
        assertThat(result.adjustmentValue()).isEqualTo(300_000);
        assertThat(result.active()).isFalse();
        verify(priceRuleRepository).saveAndFlush(priceRule);
        verify(priceRuleRepository).findByIdForUpdate(PRICE_RULE_ID);
    }

    @Test
    void updateShouldRejectUnknownId() {
        when(priceRuleRepository.findByIdForUpdate(PRICE_RULE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().update(PRICE_RULE_ID, validUpdateCommand()))
                .isInstanceOf(PriceRuleNotFoundException.class)
                .extracting("errorCode")
                .isEqualTo("PRICE_RULE_NOT_FOUND");
    }

    @Test
    void updateStatusShouldOnlyChangeActive() {
        PriceRule priceRule = priceRule(activeRoomType(), true);
        when(priceRuleRepository.findByIdForUpdate(PRICE_RULE_ID)).thenReturn(Optional.of(priceRule));
        when(priceRuleRepository.saveAndFlush(priceRule)).thenReturn(priceRule);

        var result = service().updateStatus(PRICE_RULE_ID, new UpdatePriceRuleStatusCommand(false));

        assertThat(result.active()).isFalse();
        assertThat(result.name()).isEqualTo("Weekend surcharge");
        assertThat(result.adjustmentValue()).isEqualTo(20);
        verify(priceRuleRepository).saveAndFlush(priceRule);
        verify(priceRuleRepository).findByIdForUpdate(PRICE_RULE_ID);
    }

    @Test
    void updateStatusShouldRejectNullActive() {
        assertThatThrownBy(() -> service().updateStatus(PRICE_RULE_ID, new UpdatePriceRuleStatusCommand(null)))
                .isInstanceOf(RequestValidationException.class)
                .extracting("field")
                .isEqualTo("active");

        verify(priceRuleRepository, never()).saveAndFlush(any());
    }

    private void assertFieldError(CreatePriceRuleCommand command, String field) {
        assertThatThrownBy(() -> service().create(command))
                .isInstanceOf(RequestValidationException.class)
                .extracting("field")
                .isEqualTo(field);
    }

    private CreatePriceRuleCommand validCreateCommand() {
        return new CreatePriceRuleCommand(
                ROOM_TYPE_ID,
                "Weekend surcharge",
                RuleType.WEEKEND,
                AdjustmentType.PERCENTAGE,
                20L,
                START_DATE,
                END_DATE,
                10);
    }

    private UpdatePriceRuleCommand validUpdateCommand() {
        return new UpdatePriceRuleCommand(
                ROOM_TYPE_ID,
                "Weekend surcharge",
                RuleType.WEEKEND,
                AdjustmentType.PERCENTAGE,
                20L,
                START_DATE,
                END_DATE,
                10);
    }

    private CreatePriceRuleCommand validCreateCommandWithName(String name) {
        return new CreatePriceRuleCommand(
                ROOM_TYPE_ID, name, RuleType.WEEKEND, AdjustmentType.PERCENTAGE, 20L, START_DATE, END_DATE, 10);
    }

    private CreatePriceRuleCommand commandWithRuleType(RuleType ruleType) {
        return new CreatePriceRuleCommand(
                ROOM_TYPE_ID, "Weekend surcharge", ruleType, AdjustmentType.PERCENTAGE, 20L, START_DATE, END_DATE, 10);
    }

    private CreatePriceRuleCommand commandWithAdjustmentType(AdjustmentType adjustmentType) {
        return new CreatePriceRuleCommand(
                ROOM_TYPE_ID, "Weekend surcharge", RuleType.WEEKEND, adjustmentType, 20L, START_DATE, END_DATE, 10);
    }

    private CreatePriceRuleCommand commandWithAdjustmentValue(Long adjustmentValue) {
        return new CreatePriceRuleCommand(
                ROOM_TYPE_ID,
                "Weekend surcharge",
                RuleType.WEEKEND,
                AdjustmentType.PERCENTAGE,
                adjustmentValue,
                START_DATE,
                END_DATE,
                10);
    }

    private CreatePriceRuleCommand commandWithStartDate(LocalDate startDate) {
        return new CreatePriceRuleCommand(
                ROOM_TYPE_ID,
                "Weekend surcharge",
                RuleType.WEEKEND,
                AdjustmentType.PERCENTAGE,
                20L,
                startDate,
                END_DATE,
                10);
    }

    private CreatePriceRuleCommand commandWithEndDate(LocalDate endDate) {
        return new CreatePriceRuleCommand(
                ROOM_TYPE_ID,
                "Weekend surcharge",
                RuleType.WEEKEND,
                AdjustmentType.PERCENTAGE,
                20L,
                START_DATE,
                endDate,
                10);
    }

    private CreatePriceRuleCommand commandWithDateRange(LocalDate startDate, LocalDate endDate) {
        return new CreatePriceRuleCommand(
                ROOM_TYPE_ID,
                "Weekend surcharge",
                RuleType.WEEKEND,
                AdjustmentType.PERCENTAGE,
                20L,
                startDate,
                endDate,
                10);
    }

    private CreatePriceRuleCommand commandWithPriority(Integer priority) {
        return new CreatePriceRuleCommand(
                ROOM_TYPE_ID,
                "Weekend surcharge",
                RuleType.WEEKEND,
                AdjustmentType.PERCENTAGE,
                20L,
                START_DATE,
                END_DATE,
                priority);
    }

    private RoomType activeRoomType() {
        return RoomType.builder()
                .id(ROOM_TYPE_ID)
                .code("DLX")
                .name("Deluxe")
                .basePrice(1_000_000)
                .maxGuests(3)
                .capacityAdults(2)
                .capacityChildren(1)
                .status(RoomTypeStatus.ACTIVE)
                .build();
    }

    private PriceRule priceRule(RoomType roomType, boolean active) {
        return PriceRule.builder()
                .id(PRICE_RULE_ID)
                .roomType(roomType)
                .name("Weekend surcharge")
                .ruleType(RuleType.WEEKEND)
                .adjustmentType(AdjustmentType.PERCENTAGE)
                .adjustmentValue(20)
                .adjustmentDirection(AdjustmentDirection.INCREASE)
                .startDate(START_DATE)
                .endDate(END_DATE)
                .priority(10)
                .active(active)
                .build();
    }

    private PriceRuleManagementService service() {
        return new PriceRuleManagementServiceImpl(priceRuleRepository, roomTypeRepository);
    }
}
