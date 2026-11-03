package com.tpanh.hotel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HotelBookingApplicationTests {

    @Test
    void applicationEntryPointIsAvailable() {
        assertEquals("com.tpanh.hotel.HotelBookingApplication", HotelBookingApplication.class.getName());
    }
}
