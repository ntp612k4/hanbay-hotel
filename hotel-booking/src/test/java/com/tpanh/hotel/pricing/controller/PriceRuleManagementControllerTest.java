package com.tpanh.hotel.pricing.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tpanh.hotel.common.exception.GlobalExceptionHandler;
import com.tpanh.hotel.common.response.PageResponse;
import com.tpanh.hotel.pricing.domain.AdjustmentType;
import com.tpanh.hotel.pricing.domain.RuleType;
import com.tpanh.hotel.pricing.exception.PriceRuleConflictException;
import com.tpanh.hotel.pricing.exception.PriceRuleNotFoundException;
import com.tpanh.hotel.pricing.mapper.PricingMapper;
import com.tpanh.hotel.pricing.service.PriceRuleManagementService;
import com.tpanh.hotel.pricing.service.model.CreatePriceRuleCommand;
import com.tpanh.hotel.pricing.service.model.PriceRuleManagementResult;
import com.tpanh.hotel.pricing.service.model.PriceRuleSearchCriteria;
import com.tpanh.hotel.pricing.service.model.RoomTypeSummaryResult;
import com.tpanh.hotel.pricing.service.model.UpdatePriceRuleCommand;
import com.tpanh.hotel.pricing.service.model.UpdatePriceRuleStatusCommand;
import java.time.Instant;
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
class PriceRuleManagementControllerTest {

    private static final UUID ROOM_TYPE_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID PRICE_RULE_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final LocalDate START_DATE = LocalDate.of(2026, 9, 1);
    private static final LocalDate END_DATE = LocalDate.of(2026, 12, 31);

    @Mock
    private PriceRuleManagementService priceRuleManagementService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PricingMapper mapper = Mappers.getMapper(PricingMapper.class);
        PriceRuleManagementController controller =
                new PriceRuleManagementController(priceRuleManagementService, mapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addPlaceholderValue("api.prefix", "/api/v1")
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void findAllShouldBindFiltersSortAndReturnPageEnvelope() throws Exception {
        PageResponse<PriceRuleManagementResult> page =
                new PageResponse<>(List.of(result(true)), 0, 20, 1, 1, true, true);
        when(priceRuleManagementService.findAll(new PriceRuleSearchCriteria(
                        0, 20, "weekend", ROOM_TYPE_ID, RuleType.WEEKEND, true, "startDate", "desc")))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/management/price-rules")
                        .param("search", "weekend")
                        .param("roomTypeId", ROOM_TYPE_ID.toString())
                        .param("ruleType", "WEEKEND")
                        .param("active", "true")
                        .param("sort", "startDate,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Lay danh sach quy tac gia thanh cong"))
                .andExpect(jsonPath("$.data.content[0].id").value(PRICE_RULE_ID.toString()))
                .andExpect(jsonPath("$.data.content[0].roomType.code").value("DLX"))
                .andExpect(jsonPath("$.data.content[0].ruleType").value("WEEKEND"))
                .andExpect(jsonPath("$.data.content[0].active").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void createShouldReturnCreatedResourceAndLocation() throws Exception {
        CreatePriceRuleCommand command = new CreatePriceRuleCommand(
                ROOM_TYPE_ID,
                "Weekend surcharge Deluxe",
                RuleType.WEEKEND,
                AdjustmentType.PERCENTAGE,
                20L,
                START_DATE,
                END_DATE,
                10);
        when(priceRuleManagementService.create(command)).thenReturn(result(true));

        mockMvc.perform(post("/api/v1/management/price-rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateJson()))
                .andExpect(status().isCreated())
                .andExpect(
                        header().string("Location", "http://localhost/api/v1/management/price-rules/" + PRICE_RULE_ID))
                .andExpect(jsonPath("$.data.id").value(PRICE_RULE_ID.toString()))
                .andExpect(jsonPath("$.data.adjustmentValue").value(20));
    }

    @Test
    void findByIdShouldReturnResource() throws Exception {
        when(priceRuleManagementService.findById(PRICE_RULE_ID)).thenReturn(result(true));

        mockMvc.perform(get("/api/v1/management/price-rules/{id}", PRICE_RULE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Lay chi tiet quy tac gia thanh cong"))
                .andExpect(jsonPath("$.data.id").value(PRICE_RULE_ID.toString()));
    }

    @Test
    void updateAndStatusShouldMapRequestsToSeparateCommands() throws Exception {
        UpdatePriceRuleCommand updateCommand = new UpdatePriceRuleCommand(
                ROOM_TYPE_ID,
                "Weekend surcharge Deluxe",
                RuleType.WEEKEND,
                AdjustmentType.PERCENTAGE,
                25L,
                START_DATE,
                END_DATE,
                10);
        when(priceRuleManagementService.update(eq(PRICE_RULE_ID), eq(updateCommand)))
                .thenReturn(resultWithAdjustmentValue(true, 25));
        when(priceRuleManagementService.updateStatus(eq(PRICE_RULE_ID), eq(new UpdatePriceRuleStatusCommand(false))))
                .thenReturn(result(false));

        mockMvc.perform(put("/api/v1/management/price-rules/{id}", PRICE_RULE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cap nhat quy tac gia thanh cong"))
                .andExpect(jsonPath("$.data.adjustmentValue").value(25));

        mockMvc.perform(patch("/api/v1/management/price-rules/{id}/status", PRICE_RULE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));

        verify(priceRuleManagementService).update(PRICE_RULE_ID, updateCommand);
        verify(priceRuleManagementService).updateStatus(PRICE_RULE_ID, new UpdatePriceRuleStatusCommand(false));
    }

    @Test
    void findByIdShouldExposeContractNotFoundError() throws Exception {
        when(priceRuleManagementService.findById(PRICE_RULE_ID)).thenThrow(new PriceRuleNotFoundException());

        mockMvc.perform(get("/api/v1/management/price-rules/{id}", PRICE_RULE_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRICE_RULE_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/v1/management/price-rules/" + PRICE_RULE_ID));
    }

    @Test
    void conflictShouldExposeContractConflictError() throws Exception {
        CreatePriceRuleCommand command = new CreatePriceRuleCommand(
                ROOM_TYPE_ID,
                "Weekend surcharge Deluxe",
                RuleType.WEEKEND,
                AdjustmentType.PERCENTAGE,
                20L,
                START_DATE,
                END_DATE,
                10);
        when(priceRuleManagementService.create(command)).thenThrow(new PriceRuleConflictException());

        mockMvc.perform(post("/api/v1/management/price-rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRICE_RULE_CONFLICT"));
    }

    @Test
    void invalidBodyShouldReturnFieldValidationErrors() throws Exception {
        mockMvc.perform(
                        post("/api/v1/management/price-rules")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"roomTypeId":null,"name":" ","adjustmentValue":-1,"priority":1001}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.roomTypeId").exists())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.ruleType").exists())
                .andExpect(jsonPath("$.errors.adjustmentType").exists())
                .andExpect(jsonPath("$.errors.adjustmentValue").exists())
                .andExpect(jsonPath("$.errors.startDate").exists())
                .andExpect(jsonPath("$.errors.endDate").exists())
                .andExpect(jsonPath("$.errors.priority").exists());
    }

    @Test
    void readOnlyOrUnknownFieldShouldBeRejected() throws Exception {
        mockMvc.perform(
                        put("/api/v1/management/price-rules/{id}", PRICE_RULE_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "roomTypeId":"10000000-0000-0000-0000-000000000001",
                                  "name":"Weekend surcharge Deluxe",
                                  "ruleType":"WEEKEND",
                                  "adjustmentType":"PERCENTAGE",
                                  "adjustmentValue":20,
                                  "startDate":"2026-09-01",
                                  "endDate":"2026-12-31",
                                  "priority":10,
                                  "active":false
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.active").value("Truong du lieu khong duoc ho tro"));
    }

    @Test
    void malformedJsonInvalidEnumAndInvalidUuidShouldReturnSafeErrors() throws Exception {
        mockMvc.perform(post("/api/v1/management/price-rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));

        mockMvc.perform(post("/api/v1/management/price-rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("null"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));

        mockMvc.perform(
                        post("/api/v1/management/price-rules")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "roomTypeId":"10000000-0000-0000-0000-000000000001",
                                  "name":"Weekend surcharge Deluxe",
                                  "ruleType":"NOT_A_RULE",
                                  "adjustmentType":"PERCENTAGE",
                                  "adjustmentValue":20,
                                  "startDate":"2026-09-01",
                                  "endDate":"2026-12-31",
                                  "priority":10
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));

        mockMvc.perform(get("/api/v1/management/price-rules/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.id").exists());
    }

    @Test
    void invalidSortShapeShouldReturnValidationErrorBeforeServiceCall() throws Exception {
        mockMvc.perform(get("/api/v1/management/price-rules").param("sort", "startDate"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.sort").exists());
    }

    private String validCreateJson() {
        return """
                {
                  "roomTypeId":"10000000-0000-0000-0000-000000000001",
                  "name":"Weekend surcharge Deluxe",
                  "ruleType":"WEEKEND",
                  "adjustmentType":"PERCENTAGE",
                  "adjustmentValue":20,
                  "startDate":"2026-09-01",
                  "endDate":"2026-12-31",
                  "priority":10
                }
                """;
    }

    private String validUpdateJson() {
        return """
                {
                  "roomTypeId":"10000000-0000-0000-0000-000000000001",
                  "name":"Weekend surcharge Deluxe",
                  "ruleType":"WEEKEND",
                  "adjustmentType":"PERCENTAGE",
                  "adjustmentValue":25,
                  "startDate":"2026-09-01",
                  "endDate":"2026-12-31",
                  "priority":10
                }
                """;
    }

    private PriceRuleManagementResult result(boolean active) {
        return resultWithAdjustmentValue(active, 20);
    }

    private PriceRuleManagementResult resultWithAdjustmentValue(boolean active, long adjustmentValue) {
        return new PriceRuleManagementResult(
                PRICE_RULE_ID,
                new RoomTypeSummaryResult(ROOM_TYPE_ID, "DLX", "Deluxe"),
                "Weekend surcharge Deluxe",
                RuleType.WEEKEND,
                AdjustmentType.PERCENTAGE,
                adjustmentValue,
                START_DATE,
                END_DATE,
                10,
                active,
                Instant.parse("2026-08-29T01:00:00Z"),
                Instant.parse("2026-08-29T02:00:00Z"));
    }
}
