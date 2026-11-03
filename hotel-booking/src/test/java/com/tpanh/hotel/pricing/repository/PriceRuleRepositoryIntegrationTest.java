package com.tpanh.hotel.pricing.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.tpanh.hotel.pricing.domain.AdjustmentType;
import com.tpanh.hotel.pricing.domain.PriceRule;
import com.tpanh.hotel.pricing.domain.RuleType;
import com.tpanh.hotel.room.domain.RoomType;
import com.tpanh.hotel.room.domain.RoomTypeStatus;
import com.tpanh.hotel.room.repository.RoomTypeRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Transactional
@Testcontainers(disabledWithoutDocker = true)
class PriceRuleRepositoryIntegrationTest {

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
    private PriceRuleRepository priceRuleRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Test
    void rangeQueryReturnsEveryActiveOverlappingRuleForOnlyRequestedRoomType() {
        RoomType requestedRoomType = roomType("REQUESTED");
        RoomType otherRoomType = roomType("OTHER");
        roomTypeRepository.saveAllAndFlush(List.of(requestedRoomType, otherRoomType));

        PriceRule endsOnCheckIn = rule(requestedRoomType, "Ends on check-in", date(10), date(20), true);
        PriceRule startsOnLastNight = rule(requestedRoomType, "Starts on last night", date(22), date(30), true);
        PriceRule secondApplicableRule = rule(requestedRoomType, "Second applicable", date(21), date(22), true);
        PriceRule startsOnCheckOut = rule(requestedRoomType, "Starts on check-out", date(23), date(30), true);
        PriceRule expiredBeforeStay = rule(requestedRoomType, "Expired", date(1), date(19), true);
        PriceRule inactive = rule(requestedRoomType, "Inactive", date(20), date(22), false);
        PriceRule otherResource = rule(otherRoomType, "Other room type", date(20), date(22), true);
        priceRuleRepository.saveAllAndFlush(List.of(
                endsOnCheckIn,
                startsOnLastNight,
                secondApplicableRule,
                startsOnCheckOut,
                expiredBeforeStay,
                inactive,
                otherResource));

        List<PriceRule> result =
                priceRuleRepository.findActiveRulesForStay(requestedRoomType.getId(), date(20), date(23));

        assertThat(result)
                .extracting(PriceRule::getName)
                .containsExactlyInAnyOrder("Ends on check-in", "Starts on last night", "Second applicable");
    }

    private RoomType roomType(String prefix) {
        return RoomType.builder()
                .code(prefix + "_" + UUID.randomUUID().toString().replace("-", ""))
                .name(prefix + " room type")
                .basePrice(1_000_000)
                .maxGuests(2)
                .capacityAdults(2)
                .capacityChildren(0)
                .status(RoomTypeStatus.ACTIVE)
                .build();
    }

    private PriceRule rule(RoomType roomType, String name, LocalDate startDate, LocalDate endDate, boolean active) {
        return PriceRule.builder()
                .roomType(roomType)
                .name(name)
                .ruleType(RuleType.SEASONAL)
                .adjustmentType(AdjustmentType.FIXED)
                .adjustmentValue(100_000)
                .startDate(startDate)
                .endDate(endDate)
                .priority(1)
                .active(active)
                .build();
    }

    private LocalDate date(int day) {
        return LocalDate.of(2026, 8, day);
    }
}
