package com.tpanh.hotel.booking;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tpanh.hotel.booking.controller.BookingController;
import com.tpanh.hotel.booking.domain.BookingStatus;
import com.tpanh.hotel.booking.dto.response.CreatePendingBookingResponse;
import com.tpanh.hotel.booking.exception.BookingValidationException;
import com.tpanh.hotel.booking.exception.CapacityExceededException;
import com.tpanh.hotel.booking.exception.InvalidBookingDateException;
import com.tpanh.hotel.booking.exception.RoomNotAvailableException;
import com.tpanh.hotel.booking.service.BookingService;
import com.tpanh.hotel.room.exception.RoomTypeNotFoundException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = BookingController.class)
@AutoConfigureMockMvc(addFilters = false)
class CreateBookingApiTest {

    private static final UUID ROOM_TYPE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMappingContext;

    @Test
    void returns201AndTimestampWhenBookingIsCreated() throws Exception {
        UUID bookingId = UUID.randomUUID();
        when(bookingService.createPendingBooking(any()))
                .thenReturn(new CreatePendingBookingResponse(
                        bookingId,
                        "BKG-001",
                        BookingStatus.PENDING,
                        2_000,
                        Instant.parse("2026-08-04T06:15:00Z"),
                        "booking-access-token"));

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đặt phòng thành công"))
                .andExpect(jsonPath("$.data.bookingId").value(bookingId.toString()))
                .andExpect(jsonPath("$.data.bookingCode").value("BKG-001"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.totalAmount").value(2_000))
                .andExpect(jsonPath("$.data.expiredAt").value("2026-08-04T06:15:00Z"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void returns400ForMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"adults\":1,\"children\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.path").value("/api/v1/bookings"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.errors.roomTypeId").exists())
                .andExpect(jsonPath("$.errors.checkInDate").exists())
                .andExpect(jsonPath("$.errors.checkOutDate").exists())
                .andExpect(jsonPath("$.errors.guestInfo").exists());
    }

    @Test
    void returns400ForInvalidGuestAndOccupancyFields() throws Exception {
        String json =
                """
                {
                  "roomTypeId": "%s",
                  "checkInDate": "2026-08-10",
                  "checkOutDate": "2026-08-12",
                  "adults": 0,
                  "children": -1,
                  "guestInfo": {
                    "fullName": " ",
                    "email": "invalid-email",
                    "phone": "123",
                    "identityNumber": " "
                  }
                }
                """
                        .formatted(ROOM_TYPE_ID);

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.adults").exists())
                .andExpect(jsonPath("$.errors.children").exists())
                .andExpect(jsonPath("$.errors['guestInfo.fullName']").exists())
                .andExpect(jsonPath("$.errors['guestInfo.email']").exists())
                .andExpect(jsonPath("$.errors['guestInfo.phone']").exists())
                .andExpect(jsonPath("$.errors['guestInfo.identityNumber']").exists());
    }

    @Test
    void returns400ForMalformedJson() throws Exception {
        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void mapsCapacityErrorTo400() throws Exception {
        when(bookingService.createPendingBooking(any())).thenThrow(new CapacityExceededException("Vượt quá sức chứa"));

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BOOKING_002"));
    }

    @Test
    void mapsInvalidStayDatesTo400() throws Exception {
        when(bookingService.createPendingBooking(any()))
                .thenThrow(new InvalidBookingDateException("Ngày nhận phòng phải sau ngày hiện tại"));

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BOOKING_001"));
    }

    @Test
    void mapsMissingAddOnTo400() throws Exception {
        when(bookingService.createPendingBooking(any()))
                .thenThrow(new BookingValidationException("Không tìm thấy dịch vụ tiện ích"));

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BOOKING_VALIDATION_FAILED"));
    }

    @Test
    void mapsUnknownRoomTypeTo404() throws Exception {
        when(bookingService.createPendingBooking(any()))
                .thenThrow(new RoomTypeNotFoundException("Không tìm thấy loại phòng"));

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROOM_001"));
    }

    @Test
    void mapsUnavailableRoomTo409() throws Exception {
        when(bookingService.createPendingBooking(any())).thenThrow(new RoomNotAvailableException("Hết phòng"));

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BOOKING_003"));
    }

    private String validJson() {
        return """
                {
                  "roomTypeId": "%s",
                  "checkInDate": "2026-08-10",
                  "checkOutDate": "2026-08-12",
                  "adults": 2,
                  "children": 1,
                  "guestInfo": {
                    "fullName": "Nguyễn Văn A",
                    "email": "guest@example.com",
                    "phone": "0912345678",
                    "identityNumber": "012345678901"
                  },
                  "addOns": []
                }
                """
                .formatted(ROOM_TYPE_ID);
    }
}
