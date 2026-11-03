package com.tpanh.hotel.ai.service;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
public class AiTimeContext {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss XXX");
    private final Clock clock;

    public AiTimeContext(Clock clock) {
        this.clock = clock;
    }

    public String current() {
        ZonedDateTime now = ZonedDateTime.now(clock);
        return "Thời gian hiện tại của khách sạn: " + FORMATTER.format(now) + " (" + now.getZone() + ").";
    }
}
