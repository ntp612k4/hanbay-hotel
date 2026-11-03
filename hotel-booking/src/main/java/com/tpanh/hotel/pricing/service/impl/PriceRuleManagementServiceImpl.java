package com.tpanh.hotel.pricing.service.impl;

import com.tpanh.hotel.common.exception.RequestValidationException;
import com.tpanh.hotel.common.response.PageResponse;
import com.tpanh.hotel.common.validation.PaginationValidator;
import com.tpanh.hotel.pricing.domain.AdjustmentDirection;
import com.tpanh.hotel.pricing.domain.AdjustmentType;
import com.tpanh.hotel.pricing.domain.PriceRule;
import com.tpanh.hotel.pricing.domain.PriceRulePolicy;
import com.tpanh.hotel.pricing.domain.RuleType;
import com.tpanh.hotel.pricing.exception.PriceRuleConflictException;
import com.tpanh.hotel.pricing.exception.PriceRuleNotFoundException;
import com.tpanh.hotel.pricing.repository.PriceRuleRepository;
import com.tpanh.hotel.pricing.service.PriceRuleManagementService;
import com.tpanh.hotel.pricing.service.model.CreatePriceRuleCommand;
import com.tpanh.hotel.pricing.service.model.PriceRuleManagementResult;
import com.tpanh.hotel.pricing.service.model.PriceRuleSearchCriteria;
import com.tpanh.hotel.pricing.service.model.RoomTypeSummaryResult;
import com.tpanh.hotel.pricing.service.model.UpdatePriceRuleCommand;
import com.tpanh.hotel.pricing.service.model.UpdatePriceRuleStatusCommand;
import com.tpanh.hotel.room.domain.RoomType;
import com.tpanh.hotel.room.domain.RoomTypeStatus;
import com.tpanh.hotel.room.exception.RoomTypeNotFoundException;
import com.tpanh.hotel.room.repository.RoomTypeRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PriceRuleManagementServiceImpl implements PriceRuleManagementService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_SEARCH_LENGTH = 150;
    private static final int MAX_NAME_LENGTH = 150;
    private static final int MIN_NAME_LENGTH = 2;
    private static final int MAX_PERCENTAGE_ADJUSTMENT = 100;
    private static final int MIN_PRIORITY = 0;
    private static final int MAX_PRIORITY = 1000;
    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("name", "ruleType", "adjustmentValue", "startDate", "endDate", "priority", "active", "createdAt");

    private final PriceRuleRepository priceRuleRepository;
    private final RoomTypeRepository roomTypeRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PriceRuleManagementResult> findAll(PriceRuleSearchCriteria criteria) {
        if (criteria == null) {
            throw new RequestValidationException("criteria", "Tieu chi tim kiem khong duoc de trong");
        }
        PaginationValidator.validate(criteria.page(), criteria.size(), MAX_PAGE_SIZE);
        String keyword = normalizeSearch(criteria.search());
        RuleType ruleType = validateOptionalManageableRuleType(criteria.ruleType());
        String sortBy = normalizeSortBy(criteria.sortBy());
        Sort.Direction direction = normalizeDirection(criteria.direction());
        Sort sort = Sort.by(direction, sortBy).and(Sort.by(Sort.Direction.ASC, "id"));
        PageRequest pageable = PageRequest.of(criteria.page(), criteria.size(), sort);
        Specification<PriceRule> specification =
                buildManagementSpecification(keyword, criteria.roomTypeId(), ruleType, criteria.active());

        return PageResponse.from(
                priceRuleRepository.findAll(specification, pageable).map(this::toResult));
    }

    @Override
    @Transactional(readOnly = true)
    public PriceRuleManagementResult findById(UUID id) {
        return toResult(findPriceRule(id));
    }

    @Override
    @Transactional
    public PriceRuleManagementResult create(CreatePriceRuleCommand command) {
        if (command == null) {
            throw new RequestValidationException("request", "Du lieu quy tac gia khong duoc de trong");
        }
        PriceRuleValues values = validateCreateCommand(command);
        PriceRule priceRule = PriceRule.create(
                values.roomType(),
                values.name(),
                values.ruleType(),
                values.adjustmentType(),
                values.adjustmentValue(),
                values.adjustmentDirection(),
                values.startDate(),
                values.endDate(),
                values.priority());

        try {
            return toResult(priceRuleRepository.saveAndFlush(priceRule));
        } catch (DataIntegrityViolationException exception) {
            throw new PriceRuleConflictException();
        }
    }

    @Override
    @Transactional
    public PriceRuleManagementResult update(UUID id, UpdatePriceRuleCommand command) {
        if (command == null) {
            throw new RequestValidationException("request", "Du lieu quy tac gia khong duoc de trong");
        }
        PriceRule priceRule = findPriceRuleForUpdate(id);
        PriceRuleValues values = validateUpdateCommand(command);
        priceRule.updateDetails(
                values.roomType(),
                values.name(),
                values.ruleType(),
                values.adjustmentType(),
                values.adjustmentValue(),
                values.adjustmentDirection(),
                values.startDate(),
                values.endDate(),
                values.priority());

        try {
            return toResult(priceRuleRepository.saveAndFlush(priceRule));
        } catch (DataIntegrityViolationException exception) {
            throw new PriceRuleConflictException();
        }
    }

    @Override
    @Transactional
    public PriceRuleManagementResult updateStatus(UUID id, UpdatePriceRuleStatusCommand command) {
        if (command == null || command.active() == null) {
            throw new RequestValidationException("active", "Trang thai quy tac gia khong duoc de trong");
        }
        PriceRule priceRule = findPriceRuleForUpdate(id);
        priceRule.updateStatus(command.active());

        try {
            return toResult(priceRuleRepository.saveAndFlush(priceRule));
        } catch (DataIntegrityViolationException exception) {
            throw new PriceRuleConflictException();
        }
    }

    private PriceRule findPriceRule(UUID id) {
        if (id == null) {
            throw new RequestValidationException("id", "ID quy tac gia khong duoc de trong");
        }
        return priceRuleRepository.findById(id).orElseThrow(PriceRuleNotFoundException::new);
    }

    private PriceRule findPriceRuleForUpdate(UUID id) {
        if (id == null) {
            throw new RequestValidationException("id", "ID quy tac gia khong duoc de trong");
        }
        return priceRuleRepository.findByIdForUpdate(id).orElseThrow(PriceRuleNotFoundException::new);
    }

    private PriceRuleValues validateCreateCommand(CreatePriceRuleCommand command) {
        return validateValues(
                command.roomTypeId(),
                command.name(),
                command.ruleType(),
                command.adjustmentType(),
                command.adjustmentValue(),
                command.adjustmentDirection(),
                command.startDate(),
                command.endDate(),
                command.priority());
    }

    private PriceRuleValues validateUpdateCommand(UpdatePriceRuleCommand command) {
        return validateValues(
                command.roomTypeId(),
                command.name(),
                command.ruleType(),
                command.adjustmentType(),
                command.adjustmentValue(),
                command.adjustmentDirection(),
                command.startDate(),
                command.endDate(),
                command.priority());
    }

    private PriceRuleValues validateValues(
            UUID roomTypeId,
            String rawName,
            RuleType rawRuleType,
            AdjustmentType adjustmentType,
            Long rawAdjustmentValue,
            AdjustmentDirection rawAdjustmentDirection,
            LocalDate startDate,
            LocalDate endDate,
            Integer rawPriority) {
        RoomType roomType = findActiveRoomType(roomTypeId);
        String name = normalizeName(rawName);
        RuleType ruleType = validateRequiredManageableRuleType(rawRuleType);
        if (adjustmentType == null) {
            throw new RequestValidationException("adjustmentType", "Loai dieu chinh gia khong duoc de trong");
        }
        long adjustmentValue = validateAdjustmentValue(adjustmentType, rawAdjustmentValue);
        AdjustmentDirection adjustmentDirection = validateAdjustmentDirection(ruleType, rawAdjustmentDirection);
        validateDateRange(startDate, endDate);
        int priority = validatePriority(rawPriority);
        return new PriceRuleValues(
                roomType,
                name,
                ruleType,
                adjustmentType,
                adjustmentValue,
                adjustmentDirection,
                startDate,
                endDate,
                priority);
    }

    private RoomType findActiveRoomType(UUID roomTypeId) {
        if (roomTypeId == null) {
            throw new RequestValidationException("roomTypeId", "ID hang phong khong duoc de trong");
        }
        return roomTypeRepository
                .findByIdAndStatus(roomTypeId, RoomTypeStatus.ACTIVE)
                .orElseThrow(() -> new RoomTypeNotFoundException("Khong tim thay hang phong dang kinh doanh"));
    }

    private String normalizeName(String rawName) {
        String name = rawName == null ? "" : rawName.trim();
        if (name.length() < MIN_NAME_LENGTH || name.length() > MAX_NAME_LENGTH) {
            throw new RequestValidationException("name", "Ten quy tac gia phai co tu 2 den 150 ky tu");
        }
        return name;
    }

    private RuleType validateOptionalManageableRuleType(RuleType ruleType) {
        if (ruleType == null) {
            return null;
        }
        return validateBaseIsNotManaged(ruleType);
    }

    private RuleType validateRequiredManageableRuleType(RuleType ruleType) {
        if (ruleType == null) {
            throw new RequestValidationException("ruleType", "Loai quy tac gia khong duoc de trong");
        }
        return validateBaseIsNotManaged(ruleType);
    }

    private RuleType validateBaseIsNotManaged(RuleType ruleType) {
        if (ruleType == RuleType.BASE) {
            throw new RequestValidationException("ruleType", "BASE khong duoc quan ly qua API quy tac gia");
        }
        return ruleType;
    }

    private long validateAdjustmentValue(AdjustmentType adjustmentType, Long rawAdjustmentValue) {
        if (rawAdjustmentValue == null || rawAdjustmentValue < 0) {
            throw new RequestValidationException("adjustmentValue", "Gia tri dieu chinh phai lon hon hoac bang 0");
        }
        if (adjustmentType == AdjustmentType.PERCENTAGE && rawAdjustmentValue > MAX_PERCENTAGE_ADJUSTMENT) {
            throw new RequestValidationException("adjustmentValue", "Phan tram dieu chinh khong duoc vuot qua 100");
        }
        return rawAdjustmentValue;
    }

    private AdjustmentDirection validateAdjustmentDirection(
            RuleType ruleType, AdjustmentDirection rawAdjustmentDirection) {
        AdjustmentDirection adjustmentDirection =
                rawAdjustmentDirection == null ? PriceRulePolicy.defaultDirection(ruleType) : rawAdjustmentDirection;
        if (!PriceRulePolicy.supportsDirection(ruleType, adjustmentDirection)) {
            throw new RequestValidationException(
                    "adjustmentDirection", PriceRulePolicy.directionConstraintMessage(ruleType));
        }
        return adjustmentDirection;
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            throw new RequestValidationException("startDate", "Ngay bat dau khong duoc de trong");
        }
        if (endDate == null) {
            throw new RequestValidationException("endDate", "Ngay ket thuc khong duoc de trong");
        }
        if (startDate.isAfter(endDate)) {
            throw new RequestValidationException("endDate", "Ngay ket thuc phai lon hon hoac bang ngay bat dau");
        }
    }

    private int validatePriority(Integer rawPriority) {
        if (rawPriority == null || rawPriority < MIN_PRIORITY || rawPriority > MAX_PRIORITY) {
            throw new RequestValidationException("priority", "Do uu tien phai tu 0 den 1000");
        }
        return rawPriority;
    }

    private String normalizeSearch(String rawSearch) {
        String search = rawSearch == null ? "" : rawSearch.trim();
        if (search.length() > MAX_SEARCH_LENGTH) {
            throw new RequestValidationException("search", "Tu khoa tim kiem khong duoc vuot qua 150 ky tu");
        }
        return search;
    }

    private Specification<PriceRule> buildManagementSpecification(
            String keyword, UUID roomTypeId, RuleType ruleType, Boolean active) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (roomTypeId != null) {
                predicates.add(criteriaBuilder.equal(root.get("roomType").get("id"), roomTypeId));
            }
            if (ruleType != null) {
                predicates.add(criteriaBuilder.equal(root.get("ruleType"), ruleType));
            }
            if (active != null) {
                predicates.add(criteriaBuilder.equal(root.get("active"), active));
            }
            if (!keyword.isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("name")),
                        "%" + escapeLikePattern(keyword.toLowerCase(Locale.ROOT)) + "%",
                        '\\'));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private String escapeLikePattern(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private String normalizeSortBy(String rawSortBy) {
        String sortBy = rawSortBy == null || rawSortBy.isBlank() ? "startDate" : rawSortBy.trim();
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new RequestValidationException(
                    "sort",
                    "Truong sap xep phai la name, ruleType, adjustmentValue, startDate, endDate, priority, active hoac createdAt");
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
            default -> throw new RequestValidationException("sort", "Chieu sap xep phai la asc hoac desc");
        };
    }

    private PriceRuleManagementResult toResult(PriceRule priceRule) {
        RoomType roomType = priceRule.getRoomType();
        return new PriceRuleManagementResult(
                priceRule.getId(),
                new RoomTypeSummaryResult(roomType.getId(), roomType.getCode(), roomType.getName()),
                priceRule.getName(),
                priceRule.getRuleType(),
                priceRule.getAdjustmentType(),
                priceRule.getAdjustmentValue(),
                priceRule.getAdjustmentDirection(),
                priceRule.getStartDate(),
                priceRule.getEndDate(),
                priceRule.getPriority(),
                priceRule.isActive(),
                priceRule.getCreatedAt(),
                priceRule.getUpdatedAt());
    }

    private record PriceRuleValues(
            RoomType roomType,
            String name,
            RuleType ruleType,
            AdjustmentType adjustmentType,
            long adjustmentValue,
            AdjustmentDirection adjustmentDirection,
            LocalDate startDate,
            LocalDate endDate,
            int priority) {}
}
