package com.tpanh.hotel.addon.service.impl;

import com.tpanh.hotel.addon.domain.AddOnService;
import com.tpanh.hotel.addon.exception.AddOnCodeAlreadyExistsException;
import com.tpanh.hotel.addon.exception.AddOnNotFoundException;
import com.tpanh.hotel.addon.repository.AddOnServiceRepository;
import com.tpanh.hotel.addon.service.AddOnManagementService;
import com.tpanh.hotel.addon.service.model.AddOnManagementResult;
import com.tpanh.hotel.addon.service.model.AddOnSearchCriteria;
import com.tpanh.hotel.addon.service.model.CreateAddOnCommand;
import com.tpanh.hotel.addon.service.model.UpdateAddOnCommand;
import com.tpanh.hotel.addon.service.model.UpdateAddOnStatusCommand;
import com.tpanh.hotel.common.exception.RequestValidationException;
import com.tpanh.hotel.common.response.PageResponse;
import com.tpanh.hotel.common.validation.PaginationValidator;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AddOnManagementServiceImpl implements AddOnManagementService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_SEARCH_LENGTH = 150;
    private static final int MAX_DESCRIPTION_LENGTH = 2000;
    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]*$");
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("name", "code", "price", "active", "createdAt");

    private final AddOnServiceRepository addOnServiceRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AddOnManagementResult> findAll(AddOnSearchCriteria criteria) {
        if (criteria == null) {
            throw new RequestValidationException("criteria", "Tiêu chí tìm kiếm không được để trống");
        }
        PaginationValidator.validate(criteria.page(), criteria.size(), MAX_PAGE_SIZE);
        String keyword = escapeLikePattern(normalizeSearch(criteria.search()));
        String sortBy = normalizeSortBy(criteria.sortBy());
        Sort.Direction direction = normalizeDirection(criteria.direction());
        Sort sort = Sort.by(direction, sortBy).and(Sort.by(Sort.Direction.ASC, "id"));
        PageRequest pageable = PageRequest.of(criteria.page(), criteria.size(), sort);

        return PageResponse.from(addOnServiceRepository
                .findForManagement(keyword, criteria.active(), pageable)
                .map(this::toResult));
    }

    @Override
    @Transactional(readOnly = true)
    public AddOnManagementResult findById(UUID id) {
        return toResult(findAddOn(id));
    }

    @Override
    @Transactional
    public AddOnManagementResult create(CreateAddOnCommand command) {
        if (command == null) {
            throw new RequestValidationException("request", "Dữ liệu dịch vụ bổ sung không được để trống");
        }
        String code = normalizeCode(command.code());
        String name = normalizeName(command.name());
        String description = normalizeDescription(command.description());
        long price = validatePrice(command.price());

        if (addOnServiceRepository.existsByCodeIgnoreCase(code)) {
            throw new AddOnCodeAlreadyExistsException();
        }

        AddOnService addOn = AddOnService.create(code, name, description, price);
        try {
            return toResult(addOnServiceRepository.saveAndFlush(addOn));
        } catch (DataIntegrityViolationException exception) {
            if (isCodeConflict(exception)) {
                throw new AddOnCodeAlreadyExistsException();
            }
            throw exception;
        }
    }

    @Override
    @Transactional
    public AddOnManagementResult update(UUID id, UpdateAddOnCommand command) {
        if (command == null) {
            throw new RequestValidationException("request", "Dữ liệu dịch vụ bổ sung không được để trống");
        }
        AddOnService addOn = findAddOn(id);
        addOn.updateDetails(
                normalizeName(command.name()),
                normalizeDescription(command.description()),
                validatePrice(command.price()));
        addOnServiceRepository.saveAndFlush(addOn);
        return toResult(addOn);
    }

    @Override
    @Transactional
    public AddOnManagementResult updateStatus(UUID id, UpdateAddOnStatusCommand command) {
        if (command == null || command.active() == null) {
            throw new RequestValidationException("active", "Trạng thái dịch vụ bổ sung không được để trống");
        }
        AddOnService addOn = findAddOn(id);
        addOn.updateStatus(command.active());
        addOnServiceRepository.saveAndFlush(addOn);
        return toResult(addOn);
    }

    private AddOnService findAddOn(UUID id) {
        if (id == null) {
            throw new RequestValidationException("id", "ID dịch vụ bổ sung không được để trống");
        }
        return addOnServiceRepository.findById(id).orElseThrow(AddOnNotFoundException::new);
    }

    private String normalizeCode(String rawCode) {
        String code = rawCode == null ? "" : rawCode.trim().toUpperCase(Locale.ROOT);
        if (code.length() < 2
                || code.length() > 50
                || !CODE_PATTERN.matcher(code).matches()) {
            throw new RequestValidationException(
                    "code", "Mã dịch vụ phải có 2-50 ký tự và chỉ gồm chữ in hoa, số hoặc dấu gạch dưới");
        }
        return code;
    }

    private String normalizeName(String rawName) {
        String name = rawName == null ? "" : rawName.trim();
        if (name.length() < 2 || name.length() > 150) {
            throw new RequestValidationException("name", "Tên dịch vụ phải có từ 2 đến 150 ký tự");
        }
        return name;
    }

    private String normalizeDescription(String rawDescription) {
        if (rawDescription == null || rawDescription.isBlank()) {
            return null;
        }
        String description = rawDescription.trim();
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new RequestValidationException("description", "Mô tả dịch vụ không được vượt quá 2000 ký tự");
        }
        return description;
    }

    private long validatePrice(Long price) {
        if (price == null || price < 0) {
            throw new RequestValidationException("price", "Giá dịch vụ phải lớn hơn hoặc bằng 0");
        }
        return price;
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
        String sortBy = rawSortBy == null || rawSortBy.isBlank() ? "name" : rawSortBy.trim();
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new RequestValidationException(
                    "sort", "Trường sắp xếp phải là name, code, price, active hoặc createdAt");
        }
        return sortBy;
    }

    private Sort.Direction normalizeDirection(String rawDirection) {
        String direction = rawDirection == null || rawDirection.isBlank()
                ? "asc"
                : rawDirection.trim().toLowerCase(Locale.ROOT);
        return switch (direction) {
            case "asc" -> Sort.Direction.ASC;
            case "desc" -> Sort.Direction.DESC;
            default -> throw new RequestValidationException("sort", "Chiều sắp xếp phải là asc hoặc desc");
        };
    }

    private boolean isCodeConflict(DataIntegrityViolationException exception) {
        String message = exception.getMostSpecificCause().getMessage();
        if (message == null) {
            return false;
        }
        String normalizedMessage = message.toLowerCase(Locale.ROOT);
        return normalizedMessage.contains("add_on_services") && normalizedMessage.contains("code");
    }

    private AddOnManagementResult toResult(AddOnService addOn) {
        return new AddOnManagementResult(
                addOn.getId(),
                addOn.getCode(),
                addOn.getName(),
                addOn.getDescription(),
                addOn.getPrice(),
                addOn.isActive(),
                addOn.getCreatedAt(),
                addOn.getUpdatedAt());
    }
}
