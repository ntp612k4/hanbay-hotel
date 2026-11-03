package com.tpanh.hotel.ai.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class AiDateNormalizerTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-08-25T07:15:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
    private final AiDateNormalizer normalizer = new AiDateNormalizer(clock);

    @Test
    void normalizesUpcomingDayMonthRangeToCurrentYear() {
        String context =
                normalizer.contextFor("Tìm phòng cho 2 người từ 1/9 đến 5/9").orElseThrow();

        assertThat(context)
                .contains("check-in 2026-09-01")
                .contains("check-out 2026-09-05")
                .contains("KHÔNG nằm trong quá khứ");
    }

    @Test
    void rollsPastDayMonthRangeToNextYear() {
        String context = normalizer.contextFor("Tìm phòng từ 1/8 - 5/8").orElseThrow();

        assertThat(context).contains("check-in 2027-08-01").contains("check-out 2027-08-05");
    }

    @Test
    void preservesExplicitYearAndMarksPastRange() {
        String context =
                normalizer.contextFor("Tìm phòng từ 1/8/2026 đến 5/8/2026").orElseThrow();

        assertThat(context).contains("check-in 2026-08-01").contains("NẰM TRONG QUÁ KHỨ");
    }
}
