package com.tpanh.hotel.pricing.service.impl;

import com.tpanh.hotel.common.exception.RequestValidationException;
import com.tpanh.hotel.common.response.PageResponse;
import com.tpanh.hotel.common.validation.PaginationValidator;
import com.tpanh.hotel.pricing.domain.HolidayCalendar;
import com.tpanh.hotel.pricing.exception.HolidayCalendarNotFoundException;
import com.tpanh.hotel.pricing.repository.HolidayCalendarRepository;
import com.tpanh.hotel.pricing.service.HolidayCalendarManagementService;
import com.tpanh.hotel.pricing.service.model.CreateHolidayCalendarCommand;
import com.tpanh.hotel.pricing.service.model.HolidayCalendarManagementResult;
import com.tpanh.hotel.pricing.service.model.HolidayCalendarSearchCriteria;
import com.tpanh.hotel.pricing.service.model.UpdateHolidayCalendarCommand;
import com.tpanh.hotel.pricing.service.model.UpdateHolidayCalendarStatusCommand;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HolidayCalendarManagementServiceImpl implements HolidayCalendarManagementService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_SEARCH_LENGTH = 150;
    private static final int MAX_NAME_LENGTH = 150;
    private static final int MIN_NAME_LENGTH = 2;
    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("name", "startDate", "endDate", "active", "createdAt");

    private final HolidayCalendarRepository holidayCalendarRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<HolidayCalendarManagementResult> findAll(HolidayCalendarSearchCriteria criteria) {
        if (criteria == null) {
            throw new RequestValidationException("criteria", "Tiêu chí tìm kiếm không được để trống");
        }
        PaginationValidator.validate(criteria.page(), criteria.size(), MAX_PAGE_SIZE);
        String keyword = escapeLikePattern(normalizeSearch(criteria.search()));
        String sortBy = normalizeSortBy(criteria.sortBy());
        Sort.Direction direction = normalizeDirection(criteria.direction());
        PageRequest pageable = PageRequest.of(
                criteria.page(), criteria.size(), Sort.by(direction, sortBy).and(Sort.by(Sort.Direction.ASC, "id")));
        return PageResponse.from(holidayCalendarRepository
                .findForManagement(keyword, criteria.active(), pageable)
                .map(this::toResult));
    }

    @Override
    @Transactional(readOnly = true)
    public HolidayCalendarManagementResult findById(UUID id) {
        return toResult(findHoliday(id));
    }

    @Override
    @Transactional
    public HolidayCalendarManagementResult create(CreateHolidayCalendarCommand command) {
        if (command == null) {
            throw new RequestValidationException("request", "Dữ liệu ngày lễ không được để trống");
        }
        HolidayValues values = validateValues(command.name(), command.startDate(), command.endDate());
        return toResult(holidayCalendarRepository.saveAndFlush(
                HolidayCalendar.create(values.name(), values.startDate(), values.endDate())));
    }

    @Override
    @Transactional
    public HolidayCalendarManagementResult update(UUID id, UpdateHolidayCalendarCommand command) {
        if (command == null) {
            throw new RequestValidationException("request", "Dữ liệu ngày lễ không được để trống");
        }
        HolidayCalendar holiday = findHolidayForUpdate(id);
        HolidayValues values = validateValues(command.name(), command.startDate(), command.endDate());
        holiday.updateDetails(values.name(), values.startDate(), values.endDate());
        return toResult(holidayCalendarRepository.saveAndFlush(holiday));
    }

    @Override
    @Transactional
    public HolidayCalendarManagementResult updateStatus(UUID id, UpdateHolidayCalendarStatusCommand command) {
        if (command == null || command.active() == null) {
            throw new RequestValidationException("active", "Trạng thái ngày lễ không được để trống");
        }
        HolidayCalendar holiday = findHolidayForUpdate(id);
        holiday.updateStatus(command.active());
        return toResult(holidayCalendarRepository.saveAndFlush(holiday));
    }

    private HolidayCalendar findHoliday(UUID id) {
        if (id == null) {
            throw new RequestValidationException("id", "ID ngày lễ không được để trống");
        }
        return holidayCalendarRepository.findById(id).orElseThrow(HolidayCalendarNotFoundException::new);
    }

    private HolidayCalendar findHolidayForUpdate(UUID id) {
        if (id == null) {
            throw new RequestValidationException("id", "ID ngày lễ không được để trống");
        }
        return holidayCalendarRepository.findByIdForUpdate(id).orElseThrow(HolidayCalendarNotFoundException::new);
    }

    private HolidayValues validateValues(String rawName, LocalDate startDate, LocalDate endDate) {
        String name = rawName == null ? "" : rawName.trim();
        if (name.length() < MIN_NAME_LENGTH || name.length() > MAX_NAME_LENGTH) {
            throw new RequestValidationException("name", "Tên ngày lễ phải có từ 2 đến 150 ký tự");
        }
        if (startDate == null) {
            throw new RequestValidationException("startDate", "Ngày bắt đầu không được để trống");
        }
        if (endDate == null) {
            throw new RequestValidationException("endDate", "Ngày kết thúc không được để trống");
        }
        if (startDate.isAfter(endDate)) {
            throw new RequestValidationException("endDate", "Ngày kết thúc phải sau hoặc bằng ngày bắt đầu");
        }
        return new HolidayValues(name, startDate, endDate);
    }

    private String normalizeSearch(String rawSearch) {
        String search = rawSearch == null ? "" : rawSearch.trim();
        if (search.length() > MAX_SEARCH_LENGTH) {
            throw new RequestValidationException("search", "Từ khóa tìm kiếm không được vượt quá 150 ký tự");
        }
        return search;
    }

    private String escapeLikePattern(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private String normalizeSortBy(String rawSortBy) {
        String sortBy = rawSortBy == null || rawSortBy.isBlank() ? "startDate" : rawSortBy.trim();
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new RequestValidationException(
                    "sort", "Trường sắp xếp phải là name, startDate, endDate, active hoặc createdAt");
        }
        return sortBy;
    }

    private Sort.Direction normalizeDirection(String rawDirection) {
        String direction = rawDirection == null || rawDirection.isBlank()
                ? "desc"
                : rawDirection.trim().toLowerCase(Locale.ROOT);
        return switch (direction) {
            case "asc" -> Sort.Direction.ASC;
            case "desc" -> Sort.Direction.DESC;
            default -> throw new RequestValidationException("sort", "Chiều sắp xếp phải là asc hoặc desc");
        };
    }

    private HolidayCalendarManagementResult toResult(HolidayCalendar holiday) {
        return new HolidayCalendarManagementResult(
                holiday.getId(),
                holiday.getName(),
                holiday.getStartDate(),
                holiday.getEndDate(),
                holiday.isActive(),
                holiday.getCreatedAt(),
                holiday.getUpdatedAt());
    }

    private record HolidayValues(String name, LocalDate startDate, LocalDate endDate) {}
}
