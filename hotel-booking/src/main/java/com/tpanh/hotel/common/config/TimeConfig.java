package com.tpanh.hotel.common.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfig {

    @Bean
    public Clock clock(@Value("${booking.cancellation.hotel-zone:Asia/Ho_Chi_Minh}") String hotelTimeZone) {
        return Clock.system(ZoneId.of(hotelTimeZone));
    }
}
