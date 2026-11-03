package com.tpanh.hotel.maintenance.service.impl;

import com.tpanh.hotel.common.exception.RequestValidationException;
import com.tpanh.hotel.common.response.PageResponse;
import com.tpanh.hotel.common.security.CurrentUserProvider;
import com.tpanh.hotel.common.validation.PaginationValidator;
import com.tpanh.hotel.maintenance.domain.MaintenanceRequest;
import com.tpanh.hotel.maintenance.domain.MaintenanceRequestStatus;
import com.tpanh.hotel.maintenance.dto.request.CreateMaintenanceRequestRequest;
import com.tpanh.hotel.maintenance.dto.request.UpdateMaintenanceRequestStatusRequest;
import com.tpanh.hotel.maintenance.dto.response.MaintenanceRequestListItemResponse;
import com.tpanh.hotel.maintenance.dto.response.MaintenanceRequestResponse;
import com.tpanh.hotel.maintenance.exception.MaintenanceRequestNotFoundException;
import com.tpanh.hotel.maintenance.mapper.MaintenanceRequestMapper;
import com.tpanh.hotel.maintenance.repository.MaintenanceRequestRepository;
import com.tpanh.hotel.maintenance.service.MaintenanceRequestService;
import com.tpanh.hotel.room.domain.Room;
import com.tpanh.hotel.room.domain.RoomStatus;
import com.tpanh.hotel.room.exception.RoomNotFoundException;
import com.tpanh.hotel.room.exception.RoomStateConflictException;
import com.tpanh.hotel.room.repository.RoomRepository;
import com.tpanh.hotel.user.domain.User;
import com.tpanh.hotel.user.exception.UserNotFoundException;
import com.tpanh.hotel.user.repository.UserRepository;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MaintenanceRequestServiceImpl implements MaintenanceRequestService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_DESCRIPTION_LENGTH = 2000;
    private static final Sort DEFAULT_SORT =
            Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.ASC, "id"));

    private final MaintenanceRequestRepository maintenanceRequestRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final MaintenanceRequestMapper maintenanceRequestMapper;
    private final Clock clock;

    @Override
    @Transactional
    public MaintenanceRequestResponse create(CreateMaintenanceRequestRequest request) {
        if (request == null) {
            throw new RequestValidationException("request", "Dữ liệu yêu cầu bảo trì không được để trống");
        }
        if (request.roomId() == null) {
            throw new RequestValidationException("roomId", "Phòng không được để trống");
        }

        String title = normalizeTitle(request.title());
        String description = normalizeDescription(request.description());
        Room room = roomRepository
                .findByIdForUpdate(request.roomId())
                .orElseThrow(() -> new RoomNotFoundException("Không tìm thấy phòng"));
        User createdBy = currentUserProvider
                .currentUserId()
                .flatMap(userRepository::findById)
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy tài khoản quản lý đang đăng nhập"));

        if (maintenanceRequestRepository.existsByRoomIdAndStatusNot(room.getId(), MaintenanceRequestStatus.COMPLETED)) {
            throw RoomStateConflictException.activeMaintenanceRequest();
        }
        room.startMaintenance();
        MaintenanceRequest maintenanceRequest = MaintenanceRequest.create(room, title, description, createdBy);
        MaintenanceRequest saved = maintenanceRequestRepository.saveAndFlush(maintenanceRequest);
        return maintenanceRequestMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MaintenanceRequestListItemResponse> findAll(int page, int size) {
        PaginationValidator.validate(page, size, MAX_PAGE_SIZE);
        PageRequest pageable = PageRequest.of(page, size, DEFAULT_SORT);
        return PageResponse.from(
                maintenanceRequestRepository.findAll(pageable).map(maintenanceRequestMapper::toListItemResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public MaintenanceRequestResponse findById(UUID id) {
        return maintenanceRequestMapper.toResponse(maintenanceRequestRepository
                .findDetailsById(requireId(id))
                .orElseThrow(MaintenanceRequestNotFoundException::new));
    }

    @Override
    @Transactional
    public MaintenanceRequestResponse updateStatus(UUID id, UpdateMaintenanceRequestStatusRequest request) {
        if (request == null || request.status() == null) {
            throw new RequestValidationException("status", "Trạng thái yêu cầu bảo trì không được để trống");
        }

        MaintenanceRequest maintenanceRequest = maintenanceRequestRepository
                .findByIdForUpdate(requireId(id))
                .orElseThrow(MaintenanceRequestNotFoundException::new);
        Room room = roomRepository
                .findByIdForUpdate(maintenanceRequest.getRoom().getId())
                .orElseThrow(() -> new RoomNotFoundException("Không tìm thấy phòng của yêu cầu bảo trì"));

        if (room.getStatus() != RoomStatus.MAINTENANCE) {
            throw RoomStateConflictException.notUnderMaintenance(room.getStatus());
        }
        MaintenanceRequestStatus requestedStatus = request.status();
        maintenanceRequest.transitionTo(requestedStatus, clock.instant());
        if (requestedStatus == MaintenanceRequestStatus.COMPLETED) {
            room.endMaintenance();
        }

        MaintenanceRequest saved = maintenanceRequestRepository.saveAndFlush(maintenanceRequest);
        return maintenanceRequestMapper.toResponse(saved);
    }

    private UUID requireId(UUID id) {
        if (id == null) {
            throw new RequestValidationException("id", "ID yêu cầu bảo trì không được để trống");
        }
        return id;
    }

    private String normalizeTitle(String rawTitle) {
        String title = rawTitle == null ? "" : rawTitle.trim();
        if (title.isEmpty() || title.length() > 150) {
            throw new RequestValidationException("title", "Tiêu đề bảo trì phải có từ 1 đến 150 ký tự");
        }
        return title;
    }

    private String normalizeDescription(String rawDescription) {
        if (rawDescription == null || rawDescription.isBlank()) {
            return null;
        }
        String description = rawDescription.trim();
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new RequestValidationException("description", "Mô tả bảo trì không được vượt quá 2000 ký tự");
        }
        return description;
    }
}
