package com.tpanh.hotel.room.service;

import com.tpanh.hotel.common.response.PageResponse;
import com.tpanh.hotel.room.domain.*;
import com.tpanh.hotel.room.dto.request.RoomWriteRequest;
import com.tpanh.hotel.room.dto.response.RoomManagementResponse;
import com.tpanh.hotel.room.exception.*;
import com.tpanh.hotel.room.repository.*;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomManagementService {
    private final RoomRepository rooms;
    private final RoomTypeRepository types;
    private final JdbcTemplate jdbc;

    public PageResponse<RoomManagementResponse> list(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new RoomValidationException("Phân trang không hợp lệ.");
        return PageResponse.from(rooms.findAllByOrderByFloorAscRoomNumberAsc(PageRequest.of(page, size))
                .map(RoomManagementResponse::from));
    }

    public RoomManagementResponse detail(UUID id) {
        return RoomManagementResponse.from(find(id));
    }

    @Transactional
    public RoomManagementResponse create(RoomWriteRequest r) {
        RoomType type = lockType(r.roomTypeId());
        requireActive(type);
        return RoomManagementResponse.from(rooms.saveAndFlush(Room.builder()
                .roomType(type)
                .roomNumber(r.roomNumber())
                .floor(r.floor())
                .note(r.note())
                .status(RoomStatus.AVAILABLE)
                .build()));
    }

    @Transactional
    public RoomManagementResponse update(UUID id, RoomWriteRequest r) {
        UUID oldType = rooms.findRoomTypeId(id).orElseThrow(() -> new RoomNotFoundException("Không tìm thấy phòng."));
        // Follow booking lock order: room type before physical room; sort both types to avoid deadlocks.
        java.util.stream.Stream.of(oldType, r.roomTypeId()).distinct().sorted().forEach(this::lockType);
        Room room = lockRoom(id);
        if (!room.getRoomType().getId().equals(oldType))
            throw new RoomValidationException("Phòng vừa thay đổi. Vui lòng tải lại.");
        RoomType type = lockType(r.roomTypeId());
        if (!oldType.equals(r.roomTypeId())) {
            requireNoDemand(oldType);
            requireActive(type);
        }
        room.updateDetails(type, r.roomNumber(), r.floor(), r.note());
        return RoomManagementResponse.from(rooms.saveAndFlush(room));
    }

    @Transactional
    public void delete(UUID id) {
        UUID oldType = rooms.findRoomTypeId(id).orElseThrow(() -> new RoomNotFoundException("Không tìm thấy phòng."));
        lockType(oldType);
        Room room = lockRoom(id);
        if (!room.getRoomType().getId().equals(oldType))
            throw new RoomValidationException("Phòng vừa thay đổi. Vui lòng tải lại.");
        if (room.getStatus() != RoomStatus.AVAILABLE) throw new RoomValidationException("Chỉ xóa phòng đang sẵn sàng.");
        requireNoDemand(oldType);
        rooms.delete(room);
        rooms.flush();
    }

    private void requireNoDemand(UUID typeId) {
        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM bookings WHERE room_type_id=? AND status IN ('PENDING','CONFIRMED','CHECKED_IN') AND check_out_date>=CURRENT_DATE",
                Integer.class,
                typeId);
        if (count != null && count > 0)
            throw new RoomValidationException(
                    "Loại phòng còn đặt phòng chưa hoàn tất. Không thể giảm số phòng hoặc đổi loại lúc này.");
    }

    private void requireActive(RoomType type) {
        if (type.getStatus() != RoomTypeStatus.ACTIVE)
            throw new RoomValidationException("Loại phòng đã ngừng kinh doanh.");
    }

    private Room find(UUID id) {
        return rooms.findById(id).orElseThrow(() -> new RoomNotFoundException("Không tìm thấy phòng."));
    }

    private Room lockRoom(UUID id) {
        return rooms.findByIdForUpdate(id).orElseThrow(() -> new RoomNotFoundException("Không tìm thấy phòng."));
    }

    private RoomType lockType(UUID id) {
        return types.findByIdForUpdate(id)
                .orElseThrow(() -> new RoomTypeNotFoundException("Không tìm thấy loại phòng."));
    }
}
