package com.tpanh.hotel.room.service.impl;

import com.tpanh.hotel.common.exception.RequestValidationException;
import com.tpanh.hotel.common.exception.ResourceNotFoundException;
import com.tpanh.hotel.common.response.PageResponse;
import com.tpanh.hotel.common.validation.PaginationValidator;
import com.tpanh.hotel.room.domain.RoomType;
import com.tpanh.hotel.room.domain.RoomTypeStatus;
import com.tpanh.hotel.room.dto.RoomTypeCatalogItemResponse;
import com.tpanh.hotel.room.dto.RoomTypeDetailResponse;
import com.tpanh.hotel.room.mapper.RoomTypeMapper;
import com.tpanh.hotel.room.repository.RoomTypeRepository;
import com.tpanh.hotel.room.service.RoomTypeService;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoomTypeServiceImpl implements RoomTypeService {

    private static final int MAX_PAGE_SIZE = 100;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("name", "basePrice");

    private final RoomTypeRepository roomTypeRepository;
    private final RoomTypeMapper roomTypeMapper;

    /*
     * =========================================================
     * DEVELOP - HAN-21 / HAN-23
     * Danh sách loại phòng
     * =========================================================
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<RoomTypeCatalogItemResponse> getCatalog(
            int page, int size, String keyword, String sortBy, String direction) {

        PaginationValidator.validate(page, size, MAX_PAGE_SIZE);

        String normalizedKeyword = keyword == null ? "" : keyword.trim();

        String normalizedSortBy = normalizeSortBy(sortBy);

        Sort.Direction sortDirection = normalizeDirection(direction);

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(sortDirection, normalizedSortBy));

        Page<RoomTypeCatalogItemResponse> result = roomTypeRepository
                .findByStatusAndNameContainingIgnoreCase(RoomTypeStatus.ACTIVE, normalizedKeyword, pageRequest)
                .map(roomTypeMapper::toCatalogResponse);

        return PageResponse.from(result);
    }

    /*
     * =========================================================
     * HAN-22
     * Chi tiết loại phòng
     * =========================================================
     */
    @Override
    @Transactional(readOnly = true)
    public RoomTypeDetailResponse getDetail(UUID id) {
        RoomType roomType = roomTypeRepository
                .findDetailByIdAndStatus(id, RoomTypeStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy loại phòng đang kinh doanh"));

        return roomTypeMapper.toDetailResponse(roomType);
    }

    @Override
    @Transactional(readOnly = true)
    public RoomTypeDetailResponse getDetailByName(String name) {
        RoomType roomType = roomTypeRepository
                .findDetailByNameIgnoreCaseAndStatus(name.trim(), RoomTypeStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy loại phòng đang kinh doanh"));

        return roomTypeMapper.toDetailResponse(roomType);
    }

    /*
     * =========================================================
     * Helpers của getCatalog()
     * =========================================================
     */

    private String normalizeSortBy(String sortBy) {

        String value = sortBy == null || sortBy.isBlank() ? "name" : sortBy.trim();

        if (!ALLOWED_SORT_FIELDS.contains(value)) {
            throw new RequestValidationException("sortBy", "Trường sắp xếp phải là name hoặc basePrice");
        }

        return value;
    }

    private Sort.Direction normalizeDirection(String direction) {

        String value = direction == null || direction.isBlank()
                ? "asc"
                : direction.trim().toLowerCase(Locale.ROOT);

        return switch (value) {
            case "asc" -> Sort.Direction.ASC;
            case "desc" -> Sort.Direction.DESC;
            default -> throw new RequestValidationException("direction", "Chiều sắp xếp phải là asc hoặc desc");
        };
    }
}
