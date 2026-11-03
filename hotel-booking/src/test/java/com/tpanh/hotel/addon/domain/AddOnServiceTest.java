package com.tpanh.hotel.addon.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.tpanh.hotel.booking.domain.addon.BookingAddOn;
import org.junit.jupiter.api.Test;

class AddOnServiceTest {

    @Test
    void createShouldAlwaysActivateNewAddOn() {
        AddOnService addOn = AddOnService.create("AIRPORT_PICKUP", "Đón sân bay", null, 350_000);

        assertThat(addOn.getCode()).isEqualTo("AIRPORT_PICKUP");
        assertThat(addOn.isActive()).isTrue();
    }

    @Test
    void updateDetailsShouldNotChangeCodeOrStatus() {
        AddOnService addOn = AddOnService.create("AIRPORT_PICKUP", "Đón sân bay", null, 350_000);

        addOn.updateDetails("Đón sân bay bằng xe riêng", "Tối đa 4 khách", 400_000);

        assertThat(addOn.getCode()).isEqualTo("AIRPORT_PICKUP");
        assertThat(addOn.getName()).isEqualTo("Đón sân bay bằng xe riêng");
        assertThat(addOn.getDescription()).isEqualTo("Tối đa 4 khách");
        assertThat(addOn.getPrice()).isEqualTo(400_000);
        assertThat(addOn.isActive()).isTrue();
    }

    @Test
    void catalogPriceChangeShouldNotChangeBookingSnapshot() {
        AddOnService addOn = AddOnService.create("CITY_TOUR", "City tour", null, 650_000);
        BookingAddOn bookingAddOn = BookingAddOn.builder().addOnService(addOn).build();
        bookingAddOn.updateSnapshot(2, addOn.getPrice());

        addOn.updateDetails("City tour mới", null, 700_000);
        addOn.updateStatus(false);

        assertThat(bookingAddOn.getUnitPrice()).isEqualTo(650_000);
        assertThat(bookingAddOn.getTotalPrice()).isEqualTo(1_300_000);
    }
}
