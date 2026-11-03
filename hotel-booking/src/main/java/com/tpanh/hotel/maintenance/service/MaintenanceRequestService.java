package com.tpanh.hotel.maintenance.service;

import com.tpanh.hotel.common.response.PageResponse;
import com.tpanh.hotel.maintenance.dto.request.CreateMaintenanceRequestRequest;
import com.tpanh.hotel.maintenance.dto.request.UpdateMaintenanceRequestStatusRequest;
import com.tpanh.hotel.maintenance.dto.response.MaintenanceRequestListItemResponse;
import com.tpanh.hotel.maintenance.dto.response.MaintenanceRequestResponse;
import java.util.UUID;

public interface MaintenanceRequestService {

    MaintenanceRequestResponse create(CreateMaintenanceRequestRequest request);

    PageResponse<MaintenanceRequestListItemResponse> findAll(int page, int size);

    MaintenanceRequestResponse findById(UUID id);

    MaintenanceRequestResponse updateStatus(UUID id, UpdateMaintenanceRequestStatusRequest request);
}
