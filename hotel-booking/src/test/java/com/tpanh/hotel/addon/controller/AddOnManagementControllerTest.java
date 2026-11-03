package com.tpanh.hotel.addon.controller;

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

import com.tpanh.hotel.addon.exception.AddOnCodeAlreadyExistsException;
import com.tpanh.hotel.addon.exception.AddOnNotFoundException;
import com.tpanh.hotel.addon.mapper.AddOnMapper;
import com.tpanh.hotel.addon.service.AddOnManagementService;
import com.tpanh.hotel.addon.service.model.AddOnManagementResult;
import com.tpanh.hotel.addon.service.model.AddOnSearchCriteria;
import com.tpanh.hotel.addon.service.model.CreateAddOnCommand;
import com.tpanh.hotel.addon.service.model.UpdateAddOnCommand;
import com.tpanh.hotel.addon.service.model.UpdateAddOnStatusCommand;
import com.tpanh.hotel.common.exception.GlobalExceptionHandler;
import com.tpanh.hotel.common.response.PageResponse;
import java.time.Instant;
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
class AddOnManagementControllerTest {

    private static final UUID ADD_ON_ID = UUID.fromString("4c2ff15b-c288-4722-b4b8-8e73d3f55337");

    @Mock
    private AddOnManagementService addOnManagementService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AddOnMapper mapper = Mappers.getMapper(AddOnMapper.class);
        AddOnManagementController controller = new AddOnManagementController(addOnManagementService, mapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addPlaceholderValue("api.prefix", "/api/v1")
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void findAllShouldBindFiltersSortAndReturnPageEnvelope() throws Exception {
        PageResponse<AddOnManagementResult> page = new PageResponse<>(List.of(result(true)), 0, 20, 1, 1, true, true);
        when(addOnManagementService.findAll(new AddOnSearchCriteria(0, 20, "airport", true, "price", "desc")))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/management/add-ons")
                        .param("search", "airport")
                        .param("active", "true")
                        .param("sort", "price,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Lấy danh sách dịch vụ bổ sung thành công"))
                .andExpect(jsonPath("$.data.content[0].id").value(ADD_ON_ID.toString()))
                .andExpect(jsonPath("$.data.content[0].code").value("AIRPORT_PICKUP"))
                .andExpect(jsonPath("$.data.content[0].active").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void createShouldReturnCreatedResourceAndLocation() throws Exception {
        when(addOnManagementService.create(
                        new CreateAddOnCommand("AIRPORT_PICKUP", "Đón sân bay", "Xe riêng", 350_000L)))
                .thenReturn(result(true));

        mockMvc.perform(
                        post("/api/v1/management/add-ons")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "code":"AIRPORT_PICKUP",
                                  "name":"Đón sân bay",
                                  "description":"Xe riêng",
                                  "price":350000
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/management/add-ons/" + ADD_ON_ID))
                .andExpect(jsonPath("$.data.id").value(ADD_ON_ID.toString()))
                .andExpect(jsonPath("$.data.price").value(350000));
    }

    @Test
    void updateAndStatusShouldMapRequestsToSeparateCommands() throws Exception {
        when(addOnManagementService.update(
                        eq(ADD_ON_ID), eq(new UpdateAddOnCommand("Đón sân bay mới", null, 400_000L))))
                .thenReturn(result(true));
        when(addOnManagementService.updateStatus(eq(ADD_ON_ID), eq(new UpdateAddOnStatusCommand(false))))
                .thenReturn(result(false));

        mockMvc.perform(
                        put("/api/v1/management/add-ons/{id}", ADD_ON_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"name":"Đón sân bay mới","description":null,"price":400000}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cập nhật dịch vụ bổ sung thành công"));

        mockMvc.perform(
                        patch("/api/v1/management/add-ons/{id}/status", ADD_ON_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"active":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));

        verify(addOnManagementService).update(ADD_ON_ID, new UpdateAddOnCommand("Đón sân bay mới", null, 400_000L));
        verify(addOnManagementService).updateStatus(ADD_ON_ID, new UpdateAddOnStatusCommand(false));
    }

    @Test
    void findByIdShouldExposeContractNotFoundError() throws Exception {
        when(addOnManagementService.findById(ADD_ON_ID)).thenThrow(new AddOnNotFoundException());

        mockMvc.perform(get("/api/v1/management/add-ons/{id}", ADD_ON_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ADD_ON_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/v1/management/add-ons/" + ADD_ON_ID));
    }

    @Test
    void duplicateCodeShouldExposeContractConflictError() throws Exception {
        CreateAddOnCommand command = new CreateAddOnCommand("AIRPORT_PICKUP", "Đón sân bay", null, 350_000L);
        when(addOnManagementService.create(command)).thenThrow(new AddOnCodeAlreadyExistsException());

        mockMvc.perform(
                        post("/api/v1/management/add-ons")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"code":"AIRPORT_PICKUP","name":"Đón sân bay","price":350000}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ADD_ON_CODE_ALREADY_EXISTS"));
    }

    @Test
    void invalidBodyShouldReturnFieldValidationErrors() throws Exception {
        mockMvc.perform(
                        post("/api/v1/management/add-ons")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"code":"invalid code","name":" ","price":-1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.code").exists())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.price").exists());
    }

    @Test
    void readOnlyOrUnknownFieldShouldBeRejected() throws Exception {
        mockMvc.perform(
                        put("/api/v1/management/add-ons/{id}", ADD_ON_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"code":"CANNOT_CHANGE","name":"Tên mới","price":1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.code").value("Trường dữ liệu không được hỗ trợ"));
    }

    @Test
    void malformedJsonAndInvalidUuidShouldReturnJsonErrors() throws Exception {
        mockMvc.perform(post("/api/v1/management/add-ons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));

        mockMvc.perform(post("/api/v1/management/add-ons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("null"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));

        mockMvc.perform(get("/api/v1/management/add-ons/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.id").exists());
    }

    @Test
    void invalidSortShapeShouldReturnValidationErrorBeforeServiceCall() throws Exception {
        mockMvc.perform(get("/api/v1/management/add-ons").param("sort", "name"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.sort").exists());
    }

    private AddOnManagementResult result(boolean active) {
        return new AddOnManagementResult(
                ADD_ON_ID,
                "AIRPORT_PICKUP",
                "Đón sân bay",
                "Xe riêng",
                350_000,
                active,
                Instant.parse("2026-08-20T01:00:00Z"),
                Instant.parse("2026-08-20T02:00:00Z"));
    }
}
