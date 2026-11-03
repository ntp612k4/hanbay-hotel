package com.tpanh.hotel;

import com.tpanh.hotel.booking.config.BookingCancellationProperties;
import jakarta.annotation.PostConstruct;
import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableConfigurationProperties(BookingCancellationProperties.class)
public class HotelBookingApplication {

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+7"));
    }

    public static void main(String[] args) {
        SpringApplication.run(HotelBookingApplication.class, args);
    }
}
