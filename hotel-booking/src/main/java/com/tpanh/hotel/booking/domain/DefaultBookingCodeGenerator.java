package com.tpanh.hotel.booking.domain;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultBookingCodeGenerator implements BookingCodeGenerator {

    private static final String BOOKING_CODE_PREFIX = "BK";
    private static final String BOOKING_CODE_SEQUENCE_SQL = "select nextval('booking_code_seq')";
    private static final int SEQUENCE_PADDING_LENGTH = 6;

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    @Override
    public String generate() {
        Long sequenceValue = jdbcTemplate.queryForObject(BOOKING_CODE_SEQUENCE_SQL, Long.class);
        String bookingDate = LocalDate.now(clock).format(DateTimeFormatter.BASIC_ISO_DATE);

        return BOOKING_CODE_PREFIX + bookingDate + String.format("%0" + SEQUENCE_PADDING_LENGTH + "d", sequenceValue);
    }
}
