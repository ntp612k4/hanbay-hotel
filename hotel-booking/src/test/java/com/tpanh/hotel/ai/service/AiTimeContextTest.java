package com.tpanh.hotel.ai.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class AiTimeContextTest {

    @Test
    void exposesCurrentHotelTimeToTheModel() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-25T07:15:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));

        String context = new AiTimeContext(clock).current();

        assertThat(context).contains("2026-08-25 14:15:00 +07:00").contains("Asia/Ho_Chi_Minh");
    }
}
