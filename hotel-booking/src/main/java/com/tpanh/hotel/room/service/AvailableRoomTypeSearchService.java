package com.tpanh.hotel.room.service;

import com.tpanh.hotel.common.response.PageResponse;
import com.tpanh.hotel.common.validation.PaginationValidator;
import com.tpanh.hotel.pricing.service.PricingService;
import com.tpanh.hotel.room.domain.RoomType;
import com.tpanh.hotel.room.domain.RoomTypeStatus;
import com.tpanh.hotel.room.dto.response.AvailableRoomTypeResponse;
import com.tpanh.hotel.room.exception.RoomValidationException;
import com.tpanh.hotel.room.mapper.RoomTypeMapper;
import com.tpanh.hotel.room.repository.RoomTypeRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AvailableRoomTypeSearchService {

    private static final int MAX_PAGE_SIZE = 100;

    private final RoomTypeRepository roomTypeRepository;
    private final RoomAvailabilityCalculator roomAvailabilityCalculator;
    private final Clock clock;
    private final PricingService pricingService;
    private final RoomTypeMapper roomTypeMapper;

    @Transactional(readOnly = true)
    public PageResponse<AvailableRoomTypeResponse> search(
            LocalDate checkIn, LocalDate checkOut, int adults, int children, UUID roomTypeId, int page, int size) {
        validateSearchCriteria(checkIn, checkOut, adults, children);
        PaginationValidator.validate(page, size, MAX_PAGE_SIZE);

        List<RoomType> candidateRoomTypes =
                roomTypeRepository.findAllByStatusOrderByNameAsc(RoomTypeStatus.ACTIVE).stream()
                        .filter(roomType -> roomType.canAccommodate(adults, children))
                        .filter(roomType ->
                                roomTypeId == null || roomType.getId().equals(roomTypeId))
                        .toList();

        Map<UUID, Long> availableRoomCounts = roomAvailabilityCalculator.countAvailableRooms(
                candidateRoomTypes.stream().map(RoomType::getId).toList(), checkIn, checkOut);

        List<RoomType> availableRoomTypes = candidateRoomTypes.stream()
                .filter(roomType -> availableRoomCounts.getOrDefault(roomType.getId(), 0L) > 0)
                .toList();

        int totalElements = availableRoomTypes.size();
        int fromIndex = (int) Math.min((long) page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<AvailableRoomTypeResponse> content = availableRoomTypes.subList(fromIndex, toIndex).stream()
                .map(roomType -> roomTypeMapper.toAvailableResponse(
                        roomType,
                        availableRoomCounts.getOrDefault(roomType.getId(), 0L),
                        pricingService.calculateRoomPrice(roomType, checkIn, checkOut)))
                .toList();
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);

        return new PageResponse<>(
                content, page, size, totalElements, totalPages, page == 0, totalPages == 0 || page >= totalPages - 1);
    }

    private void validateSearchCriteria(LocalDate checkIn, LocalDate checkOut, int adults, int children) {
        if (checkIn == null) {
            throw new RoomValidationException("Ngày nhận phòng là bắt buộc.");
        }
        if (checkOut == null) {
            throw new RoomValidationException("Ngày trả phòng là bắt buộc.");
        }
        if (checkIn.isBefore(LocalDate.now(clock))) {
            throw new RoomValidationException("Ngày nhận phòng không được ở quá khứ.");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new RoomValidationException("Ngày trả phòng phải sau ngày nhận phòng.");
        }
        if (adults < 1) {
            throw new RoomValidationException("Số người lớn phải lớn hơn hoặc bằng 1.");
        }
        if (children < 0) {
            throw new RoomValidationException("Số trẻ em không được âm.");
        }
    }
}
