package com.tpanh.hotel.maintenance.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tpanh.hotel.common.exception.GlobalExceptionHandler;
import com.tpanh.hotel.common.response.PageResponse;
import com.tpanh.hotel.maintenance.domain.MaintenanceRequestStatus;
import com.tpanh.hotel.maintenance.dto.request.CreateMaintenanceRequestRequest;
import com.tpanh.hotel.maintenance.dto.request.UpdateMaintenanceRequestStatusRequest;
import com.tpanh.hotel.maintenance.dto.response.MaintenanceRequestListItemResponse;
import com.tpanh.hotel.maintenance.dto.response.MaintenanceRequestResponse;
import com.tpanh.hotel.maintenance.exception.InvalidMaintenanceRequestTransitionException;
import com.tpanh.hotel.maintenance.exception.MaintenanceRequestNotFoundException;
import com.tpanh.hotel.maintenance.service.MaintenanceRequestService;
import com.tpanh.hotel.room.domain.RoomStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class MaintenanceRequestControllerTest {

    private static final UUID REQUEST_ID = UUID.fromString("31000000-0000-0000-0000-000000000001");
    private static final UUID ROOM_ID = UUID.fromString("32000000-0000-0000-0000-000000000001");
    private static final UUID MANAGER_ID = UUID.fromString("33000000-0000-0000-0000-000000000001");

    @Mock
    private MaintenanceRequestService maintenanceRequestService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new MaintenanceRequestController(maintenanceRequestService))
                .addPlaceholderValue("api.prefix", "/api/v1")
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createReturnsCreatedEnvelopeAndLocation() throws Exception {
        CreateMaintenanceRequestRequest request =
                new CreateMaintenanceRequestRequest(ROOM_ID, "Điều hòa hỏng", "Không làm lạnh");
        when(maintenanceRequestService.create(request))
                .thenReturn(response(MaintenanceRequestStatus.PENDING, RoomStatus.MAINTENANCE));

        mockMvc.perform(
                        post("/api/v1/maintenance-requests")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "roomId":"32000000-0000-0000-0000-000000000001",
                                  "title":"Điều hòa hỏng",
                                  "description":"Không làm lạnh"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/maintenance-requests/" + REQUEST_ID))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Tạo yêu cầu bảo trì thành công"))
                .andExpect(jsonPath("$.data.id").value(REQUEST_ID.toString()))
                .andExpect(jsonPath("$.data.roomId").value(ROOM_ID.toString()))
                .andExpect(jsonPath("$.data.roomNumber").value("0501"))
                .andExpect(jsonPath("$.data.roomStatus").value("MAINTENANCE"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.createdById").value(MANAGER_ID.toString()))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(maintenanceRequestService).create(request);
    }

    @Test
    void listUsesDefaultPaginationAndReturnsPageEnvelope() throws Exception {
        MaintenanceRequestListItemResponse item = listItem();
        when(maintenanceRequestService.findAll(0, 20))
                .thenReturn(new PageResponse<>(List.of(item), 0, 20, 1, 1, true, true));

        mockMvc.perform(get("/api/v1/maintenance-requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Lấy danh sách yêu cầu bảo trì thành công"))
                .andExpect(jsonPath("$.data.content[0].id").value(REQUEST_ID.toString()))
                .andExpect(jsonPath("$.data.content[0].title").value("Điều hòa hỏng"))
                .andExpect(jsonPath("$.data.content[0].description").doesNotExist())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.first").value(true))
                .andExpect(jsonPath("$.data.last").value(true));

        verify(maintenanceRequestService).findAll(0, 20);
    }

    @Test
    void listBindsCustomPaginationAndDetailReturnsFullResponse() throws Exception {
        when(maintenanceRequestService.findAll(2, 5))
                .thenReturn(new PageResponse<>(List.of(), 2, 5, 0, 0, false, true));
        when(maintenanceRequestService.findById(REQUEST_ID))
                .thenReturn(response(MaintenanceRequestStatus.PENDING, RoomStatus.MAINTENANCE));

        mockMvc.perform(get("/api/v1/maintenance-requests").param("page", "2").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.size").value(5));

        mockMvc.perform(get("/api/v1/maintenance-requests/{id}", REQUEST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Lấy chi tiết yêu cầu bảo trì thành công"))
                .andExpect(jsonPath("$.data.description").value("Không làm lạnh"));
    }

    @Test
    void updateStatusReturnsUpdatedRequestAndRoomState() throws Exception {
        UpdateMaintenanceRequestStatusRequest request =
                new UpdateMaintenanceRequestStatusRequest(MaintenanceRequestStatus.COMPLETED);
        when(maintenanceRequestService.updateStatus(REQUEST_ID, request))
                .thenReturn(response(MaintenanceRequestStatus.COMPLETED, RoomStatus.AVAILABLE));

        mockMvc.perform(patch("/api/v1/maintenance-requests/{id}/status", REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cập nhật trạng thái yêu cầu bảo trì thành công"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.roomStatus").value("AVAILABLE"))
                .andExpect(jsonPath("$.data.completedAt").value("2026-09-04T04:00:00Z"));

        verify(maintenanceRequestService).updateStatus(REQUEST_ID, request);
    }

    @Test
    void createAndUpdateValidateRequiredFieldsBeforeCallingService() throws Exception {
        mockMvc.perform(post("/api/v1/maintenance-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Điều hòa hỏng\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.roomId").exists());

        mockMvc.perform(post("/api/v1/maintenance-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomId\":\"" + ROOM_ID + "\",\"title\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists());

        mockMvc.perform(patch("/api/v1/maintenance-requests/{id}/status", REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.status").exists());

        verifyNoInteractions(maintenanceRequestService);
    }

    @Test
    void malformedEnumAndUuidUseGlobalErrorContract() throws Exception {
        mockMvc.perform(patch("/api/v1/maintenance-requests/{id}/status", REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));

        mockMvc.perform(get("/api/v1/maintenance-requests/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.id").exists());

        verifyNoInteractions(maintenanceRequestService);
    }

    @Test
    void mapsNotFoundAndInvalidTransitionToDocumentedStatuses() throws Exception {
        when(maintenanceRequestService.findById(REQUEST_ID)).thenThrow(new MaintenanceRequestNotFoundException());
        UpdateMaintenanceRequestStatusRequest request =
                new UpdateMaintenanceRequestStatusRequest(MaintenanceRequestStatus.COMPLETED);
        when(maintenanceRequestService.updateStatus(REQUEST_ID, request))
                .thenThrow(new InvalidMaintenanceRequestTransitionException(
                        MaintenanceRequestStatus.PENDING, MaintenanceRequestStatus.COMPLETED));

        mockMvc.perform(get("/api/v1/maintenance-requests/{id}", REQUEST_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MAINTENANCE_REQUEST_NOT_FOUND"));

        mockMvc.perform(patch("/api/v1/maintenance-requests/{id}/status", REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MAINTENANCE_REQUEST_STATUS_CONFLICT"))
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    private MaintenanceRequestResponse response(MaintenanceRequestStatus status, RoomStatus roomStatus) {
        return new MaintenanceRequestResponse(
                REQUEST_ID,
                ROOM_ID,
                "0501",
                roomStatus,
                "Điều hòa hỏng",
                "Không làm lạnh",
                status,
                MANAGER_ID,
                "Quản lý HANBAY",
                Instant.parse("2026-09-04T01:00:00Z"),
                Instant.parse("2026-09-04T04:00:00Z"),
                status == MaintenanceRequestStatus.PENDING ? null : Instant.parse("2026-09-04T02:00:00Z"),
                status == MaintenanceRequestStatus.COMPLETED ? Instant.parse("2026-09-04T04:00:00Z") : null);
    }

    private MaintenanceRequestListItemResponse listItem() {
        return new MaintenanceRequestListItemResponse(
                REQUEST_ID,
                ROOM_ID,
                "0501",
                RoomStatus.MAINTENANCE,
                "Điều hòa hỏng",
                MaintenanceRequestStatus.PENDING,
                MANAGER_ID,
                "Quản lý HANBAY",
                Instant.parse("2026-09-04T01:00:00Z"),
                null,
                null);
    }
}
