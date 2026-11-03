package com.tpanh.hotel.pricing.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tpanh.hotel.common.exception.GlobalExceptionHandler;
import com.tpanh.hotel.pricing.dto.result.NightlyPriceResult;
import com.tpanh.hotel.pricing.dto.result.RoomTypePricingResult;
import com.tpanh.hotel.pricing.dto.result.StayPriceResult;
import com.tpanh.hotel.pricing.exception.PricingValidationException;
import com.tpanh.hotel.pricing.mapper.PricingMapper;
import com.tpanh.hotel.pricing.service.PricingService;
import com.tpanh.hotel.room.exception.RoomTypeNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class PricingControllerTest {

    private static final UUID ROOM_TYPE_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final LocalDate CHECK_IN = LocalDate.of(2026, 8, 20);
    private static final LocalDate CHECK_OUT = LocalDate.of(2026, 8, 23);

    @Mock
    private PricingService pricingService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PricingMapper pricingMapper = Mappers.getMapper(PricingMapper.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new PricingController(pricingService, pricingMapper))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addPlaceholderValue("api.prefix", "/api/v1")
                .build();
    }

    @Test
    void validRequestReturnsNightlyBreakdownInApiResponse() throws Exception {
        when(pricingService.calculateStayPrice(ROOM_TYPE_ID, CHECK_IN, CHECK_OUT))
                .thenReturn(result());

        mockMvc.perform(post("/api/v1/pricing/stay-price-calculations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Tính giá lưu trú thành công"))
                .andExpect(jsonPath("$.data.roomType.id").value(ROOM_TYPE_ID.toString()))
                .andExpect(jsonPath("$.data.roomType.code").value("DLX"))
                .andExpect(jsonPath("$.data.checkInDate").value("2026-08-20"))
                .andExpect(jsonPath("$.data.checkOutDate").value("2026-08-23"))
                .andExpect(jsonPath("$.data.numberOfNights").value(3))
                .andExpect(jsonPath("$.data.nightlyPrices[0].date").value("2026-08-20"))
                .andExpect(jsonPath("$.data.nightlyPrices[0].finalPrice").value(1_000_000))
                .andExpect(jsonPath("$.data.totalPrice").value(3_300_000))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(pricingService).calculateStayPrice(ROOM_TYPE_ID, CHECK_IN, CHECK_OUT);
    }

    @Test
    void missingRequiredFieldReturnsValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/pricing/stay-price-calculations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomTypeId\":\"" + ROOM_TYPE_ID + "\",\"checkInDate\":\"2026-08-20\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.checkOutDate").value("Ngày trả phòng không được để trống"))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }

    @Test
    void malformedIdentifierReturnsSafeBadRequestInsteadOfInternalError() throws Exception {
        mockMvc.perform(
                        post("/api/v1/pricing/stay-price-calculations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "roomTypeId": "not-a-uuid",
                                  "checkInDate": "2026-08-20",
                                  "checkOutDate": "2026-08-23"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }

    @Test
    void malformedDateReturnsSafeBadRequestInsteadOfInternalError() throws Exception {
        mockMvc.perform(
                        post("/api/v1/pricing/stay-price-calculations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "roomTypeId": "10000000-0000-0000-0000-000000000001",
                                  "checkInDate": "20/08/2026",
                                  "checkOutDate": "2026-08-23"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }

    @Test
    void invalidDateRangeFromServiceReturnsPricingValidationError() throws Exception {
        when(pricingService.calculateStayPrice(ROOM_TYPE_ID, CHECK_IN, CHECK_OUT))
                .thenThrow(new PricingValidationException("Ngày trả phòng phải sau ngày nhận phòng."));

        mockMvc.perform(post("/api/v1/pricing/stay-price-calculations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PRICING_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Ngày trả phòng phải sau ngày nhận phòng."));
    }

    @Test
    void missingRoomTypeFromServiceReturnsNotFound() throws Exception {
        when(pricingService.calculateStayPrice(ROOM_TYPE_ID, CHECK_IN, CHECK_OUT))
                .thenThrow(new RoomTypeNotFoundException("Không tìm thấy hạng phòng đang kinh doanh."));

        mockMvc.perform(post("/api/v1/pricing/stay-price-calculations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROOM_001"));
    }

    private String validRequest() {
        return """
                {
                  "roomTypeId": "10000000-0000-0000-0000-000000000001",
                  "checkInDate": "2026-08-20",
                  "checkOutDate": "2026-08-23"
                }
                """;
    }

    private StayPriceResult result() {
        return new StayPriceResult(
                new RoomTypePricingResult(ROOM_TYPE_ID, "DLX", "Deluxe"),
                CHECK_IN,
                CHECK_OUT,
                3,
                List.of(
                        new NightlyPriceResult(LocalDate.of(2026, 8, 20), 1_000_000),
                        new NightlyPriceResult(LocalDate.of(2026, 8, 21), 1_100_000),
                        new NightlyPriceResult(LocalDate.of(2026, 8, 22), 1_200_000)),
                3_300_000);
    }
}
