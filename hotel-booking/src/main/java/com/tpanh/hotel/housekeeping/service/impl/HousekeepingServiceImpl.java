package com.tpanh.hotel.housekeeping.service.impl;

import com.tpanh.hotel.common.response.PageResponse;
import com.tpanh.hotel.common.validation.PaginationValidator;
import com.tpanh.hotel.housekeeping.dto.response.DirtyRoomResponse;
import com.tpanh.hotel.housekeeping.mapper.DirtyRoomMapper;
import com.tpanh.hotel.housekeeping.service.HousekeepingService;
import com.tpanh.hotel.room.domain.RoomStatus;
import com.tpanh.hotel.room.exception.RoomNotFoundException;
import com.tpanh.hotel.room.repository.RoomRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HousekeepingServiceImpl implements HousekeepingService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Sort DIRTY_ROOM_SORT = Sort.by(Sort.Order.asc("floor"), Sort.Order.asc("roomNumber"));

    private final RoomRepository roomRepository;
    private final DirtyRoomMapper dirtyRoomMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DirtyRoomResponse> getDirtyRooms(int page, int size) {
        PaginationValidator.validate(page, size, MAX_PAGE_SIZE);

        PageRequest pageable = PageRequest.of(page, size, DIRTY_ROOM_SORT);
        return PageResponse.from(
                roomRepository.findAllByStatus(RoomStatus.DIRTY, pageable).map(dirtyRoomMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DirtyRoomResponse> getRoomsRequiringCleaning(int page, int size) {
        PaginationValidator.validate(page, size, MAX_PAGE_SIZE);
        PageRequest pageable = PageRequest.of(page, size, DIRTY_ROOM_SORT);
        return PageResponse.from(roomRepository
                .findAllByStatusIn(List.of(RoomStatus.DIRTY, RoomStatus.CLEANING), pageable)
                .map(dirtyRoomMapper::toResponse));
    }

    @Override
    @Transactional
    public DirtyRoomResponse startCleaning(UUID roomId) {
        var room = roomRepository
                .findByIdForUpdate(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Không tìm thấy phòng cần dọn"));
        room.startCleaning();
        return dirtyRoomMapper.toResponse(room);
    }

    @Override
    @Transactional
    public DirtyRoomResponse completeCleaning(UUID roomId) {
        var room = roomRepository
                .findByIdForUpdate(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Không tìm thấy phòng đang dọn"));
        room.completeCleaning();
        return dirtyRoomMapper.toResponse(room);
    }
}
