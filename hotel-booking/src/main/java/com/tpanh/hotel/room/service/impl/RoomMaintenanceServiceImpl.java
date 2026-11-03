package com.tpanh.hotel.room.service.impl;

import com.tpanh.hotel.common.exception.RequestValidationException;
import com.tpanh.hotel.maintenance.domain.MaintenanceRequestStatus;
import com.tpanh.hotel.maintenance.repository.MaintenanceRequestRepository;
import com.tpanh.hotel.room.domain.Room;
import com.tpanh.hotel.room.domain.RoomMaintenanceAction;
import com.tpanh.hotel.room.dto.response.RoomMaintenanceResponse;
import com.tpanh.hotel.room.exception.RoomNotFoundException;
import com.tpanh.hotel.room.exception.RoomStateConflictException;
import com.tpanh.hotel.room.repository.RoomRepository;
import com.tpanh.hotel.room.service.RoomMaintenanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoomMaintenanceServiceImpl implements RoomMaintenanceService {

    private final RoomRepository roomRepository;
    private final MaintenanceRequestRepository maintenanceRequestRepository;

    @Override
    @Transactional
    public RoomMaintenanceResponse changeMaintenanceStatus(String roomNumber, RoomMaintenanceAction action) {
        if (action == null || action == RoomMaintenanceAction.UNKNOWN) {
            throw new RequestValidationException("action", "Hành động bảo trì không hợp lệ");
        }
        String normalizedRoomNumber = roomNumber == null ? "" : roomNumber.trim();
        if (normalizedRoomNumber.isEmpty() || normalizedRoomNumber.length() > 30) {
            throw new RequestValidationException("roomNumber", "Số phòng phải có từ 1 đến 30 ký tự");
        }
        Room room = roomRepository
                .findByRoomNumberForUpdate(normalizedRoomNumber)
                .orElseThrow(() -> new RoomNotFoundException("Không tìm thấy phòng " + normalizedRoomNumber));

        if (maintenanceRequestRepository.existsByRoomIdAndStatusNot(room.getId(), MaintenanceRequestStatus.COMPLETED)) {
            throw RoomStateConflictException.activeMaintenanceRequest();
        }
        if (action == RoomMaintenanceAction.START_MAINTENANCE) {
            room.startMaintenance();
        } else {
            room.endMaintenance();
        }
        return new RoomMaintenanceResponse(room.getId(), room.getRoomNumber(), room.getStatus());
    }
}
