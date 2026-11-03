package com.tpanh.hotel.room.service.impl;

import com.tpanh.hotel.common.response.PageResponse;
import com.tpanh.hotel.common.validation.PaginationValidator;
import com.tpanh.hotel.room.dto.response.PhysicalRoomListItemResponse;
import com.tpanh.hotel.room.repository.RoomRepository;
import com.tpanh.hotel.room.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private static final int MAX_PAGE_SIZE = 100;

    private final RoomRepository roomRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PhysicalRoomListItemResponse> findAll(int page, int size) {
        PaginationValidator.validate(page, size, MAX_PAGE_SIZE);

        return PageResponse.from(roomRepository
                .findAllByOrderByFloorAscRoomNumberAsc(PageRequest.of(page, size))
                .map(room -> new PhysicalRoomListItemResponse(
                        room.getId(), room.getRoomNumber(), room.getFloor(), room.getStatus())));
    }
}
