package com.tpanh.hotel.booking;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tpanh.hotel.booking.controller.CheckOutController;
import com.tpanh.hotel.booking.domain.BookingStatus;
import com.tpanh.hotel.booking.dto.request.CheckOutGuestRequest;
import com.tpanh.hotel.booking.dto.response.AssignedRoomResponse;
import com.tpanh.hotel.booking.dto.response.CheckOutGuestResponse;
import com.tpanh.hotel.booking.exception.CheckOutConflictException;
import com.tpanh.hotel.booking.service.CheckOutService;
import com.tpanh.hotel.room.domain.RoomStatus;
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

@WebMvcTest(controllers = CheckOutController.class)
@AutoConfigureMockMvc(addFilters = false)
class CheckOutControllerTest {

    private static final UUID BOOKING_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID ROOM_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CheckOutService checkOutService;

    @MockitoBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMappingContext;

    @Test
    void returnsCheckOutResult() throws Exception {
        when(checkOutService.checkOut(
                        BOOKING_ID,
                        new CheckOutGuestRequest("Khách đã trả đủ chìa khóa", 100_000L, "Mất thẻ phòng", 100_000L)))
                .thenReturn(response());

        mockMvc.perform(
                        patch("/api/v1/bookings/{bookingId}/check-out", BOOKING_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "note": "Khách đã trả đủ chìa khóa",
                                  "additionalChargeAmount": 100000,
                                  "additionalChargeReason": "Mất thẻ phòng",
                                  "collectedAmount": 100000
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Trả phòng thành công"))
                .andExpect(jsonPath("$.data.bookingId").value(BOOKING_ID.toString()))
                .andExpect(jsonPath("$.data.bookingCode").value("BK20260824000001"))
                .andExpect(jsonPath("$.data.bookingStatus").value("CHECKED_OUT"))
                .andExpect(jsonPath("$.data.room.roomNumber").value("0501"))
                .andExpect(jsonPath("$.data.room.status").value("DIRTY"))
                .andExpect(jsonPath("$.data.outstandingAmount").value(0))
                .andExpect(jsonPath("$.data.additionalChargeAmount").value(100_000))
                .andExpect(jsonPath("$.data.additionalChargeReason").value("Mất thẻ phòng"))
                .andExpect(jsonPath("$.data.collectedAmount").value(100_000));
    }

    @Test
    void acceptsMissingRequestBody() throws Exception {
        when(checkOutService.checkOut(BOOKING_ID, null)).thenReturn(response());

        mockMvc.perform(patch("/api/v1/bookings/{bookingId}/check-out", BOOKING_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookingStatus").value("CHECKED_OUT"));

        verify(checkOutService).checkOut(BOOKING_ID, null);
    }

    @Test
    void forwardsInspectionChargeRequest() throws Exception {
        CheckOutGuestRequest request =
                new CheckOutGuestRequest("Khách xác nhận biên bản", 100_000L, "Mất thẻ phòng", 100_000L);
        when(checkOutService.checkOut(BOOKING_ID, request)).thenReturn(response());

        mockMvc.perform(
                        patch("/api/v1/bookings/{bookingId}/check-out", BOOKING_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "note": "Khách xác nhận biên bản",
                                  "additionalChargeAmount": 100000,
                                  "additionalChargeReason": "Mất thẻ phòng",
                                  "collectedAmount": 100000
                                }
                                """))
                .andExpect(status().isOk());

        verify(checkOutService).checkOut(BOOKING_ID, request);
    }

    @Test
    void returns400WhenNoteIsTooLong() throws Exception {
        String note = "a".repeat(501);

        mockMvc.perform(patch("/api/v1/bookings/{bookingId}/check-out", BOOKING_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"" + note + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.note").value("Ghi chú không được vượt quá 500 ký tự"));
    }

    @Test
    void returns400WhenAdditionalChargeIsNegative() throws Exception {
        mockMvc.perform(patch("/api/v1/bookings/{bookingId}/check-out", BOOKING_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"additionalChargeAmount\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.additionalChargeAmount").value("Phí phát sinh không được âm"));
    }

    @Test
    void returns400WhenAdditionalChargeReasonIsMissing() throws Exception {
        mockMvc.perform(patch("/api/v1/bookings/{bookingId}/check-out", BOOKING_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"additionalChargeAmount\":100000,\"collectedAmount\":100000}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.additionalChargeReasonValid")
                        .value("Lý do phát sinh là bắt buộc khi có phí phát sinh"));
    }

    @Test
    void mapsBusinessConflictTo409() throws Exception {
        when(checkOutService.checkOut(eq(BOOKING_ID), eq(null)))
                .thenThrow(CheckOutConflictException.paymentRequired(100_000L));

        mockMvc.perform(patch("/api/v1/bookings/{bookingId}/check-out", BOOKING_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PAYMENT_REQUIRED"));
    }

    @Test
    void returns400WhenBookingIdIsInvalid() throws Exception {
        mockMvc.perform(patch("/api/v1/bookings/{bookingId}/check-out", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.bookingId").exists());
    }

    @Test
    void rejectsAmountsAboveSupportedLimit() throws Exception {
        mockMvc.perform(
                        patch("/api/v1/bookings/{bookingId}/check-out", BOOKING_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"additionalChargeAmount\":10000000000,\"additionalChargeReason\":\"Vỡ ly\",\"collectedAmount\":10000000000}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.additionalChargeAmount").exists())
                .andExpect(jsonPath("$.errors.collectedAmount").exists());
        org.mockito.Mockito.verifyNoInteractions(checkOutService);
    }

    @Test
    void rejectsFractionalMoneyInsteadOfTruncatingIt() throws Exception {
        for (String field : java.util.List.of("collectedAmount", "additionalChargeAmount")) {
            mockMvc.perform(patch("/api/v1/bookings/{bookingId}/check-out", BOOKING_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"" + field + "\":1.5}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
        }
        org.mockito.Mockito.verifyNoInteractions(checkOutService);
    }

    private CheckOutGuestResponse response() {
        return new CheckOutGuestResponse(
                BOOKING_ID,
                "BK20260824000001",
                BookingStatus.CHECKED_OUT,
                new AssignedRoomResponse(ROOM_ID, "0501", 5, RoomStatus.DIRTY),
                Instant.parse("2026-08-24T03:30:00Z"),
                5_635_000L,
                5_635_000L,
                0L,
                100_000L,
                "Mất thẻ phòng",
                100_000L);
    }
}
