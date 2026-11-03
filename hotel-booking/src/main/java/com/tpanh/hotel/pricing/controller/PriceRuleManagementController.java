package com.tpanh.hotel.pricing.controller;

import com.tpanh.hotel.common.exception.RequestValidationException;
import com.tpanh.hotel.common.response.ApiResponse;
import com.tpanh.hotel.common.response.PageResponse;
import com.tpanh.hotel.pricing.domain.RuleType;
import com.tpanh.hotel.pricing.dto.request.CreatePriceRuleRequest;
import com.tpanh.hotel.pricing.dto.request.UpdatePriceRuleRequest;
import com.tpanh.hotel.pricing.dto.request.UpdatePriceRuleStatusRequest;
import com.tpanh.hotel.pricing.dto.response.PriceRuleManagementResponse;
import com.tpanh.hotel.pricing.mapper.PricingMapper;
import com.tpanh.hotel.pricing.service.PriceRuleManagementService;
import com.tpanh.hotel.pricing.service.model.PriceRuleManagementResult;
import com.tpanh.hotel.pricing.service.model.PriceRuleSearchCriteria;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("${api.prefix}/management/price-rules")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
@Tag(name = "Quan ly quy tac gia", description = "API quan ly quy tac gia dung trong dynamic pricing")
public class PriceRuleManagementController {

    private final PriceRuleManagementService priceRuleManagementService;
    private final PricingMapper pricingMapper;

    @GetMapping
    @Operation(summary = "Tra cuu danh muc quy tac gia")
    public ApiResponse<PageResponse<PriceRuleManagementResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID roomTypeId,
            @RequestParam(required = false) RuleType ruleType,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "startDate,desc") String sort) {
        String[] sortParts = parseSort(sort);
        PageResponse<PriceRuleManagementResult> result = priceRuleManagementService.findAll(new PriceRuleSearchCriteria(
                page, size, search, roomTypeId, ruleType, active, sortParts[0], sortParts[1]));
        return ApiResponse.success("Lay danh sach quy tac gia thanh cong", mapPage(result));
    }

    @PostMapping
    @Operation(summary = "Tao quy tac gia")
    public ResponseEntity<ApiResponse<PriceRuleManagementResponse>> create(
            @Valid @RequestBody CreatePriceRuleRequest request) {
        PriceRuleManagementResponse response =
                pricingMapper.toManagementResponse(priceRuleManagementService.create(request.toCommand()));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(ApiResponse.success("Tao quy tac gia thanh cong", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiet quy tac gia")
    public ApiResponse<PriceRuleManagementResponse> findById(@PathVariable UUID id) {
        return ApiResponse.success(
                "Lay chi tiet quy tac gia thanh cong",
                pricingMapper.toManagementResponse(priceRuleManagementService.findById(id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cap nhat quy tac gia")
    public ApiResponse<PriceRuleManagementResponse> update(
            @PathVariable UUID id, @Valid @RequestBody UpdatePriceRuleRequest request) {
        return ApiResponse.success(
                "Cap nhat quy tac gia thanh cong",
                pricingMapper.toManagementResponse(priceRuleManagementService.update(id, request.toCommand())));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Kich hoat hoac ngung kich hoat quy tac gia")
    public ApiResponse<PriceRuleManagementResponse> updateStatus(
            @PathVariable UUID id, @Valid @RequestBody UpdatePriceRuleStatusRequest request) {
        return ApiResponse.success(
                "Cap nhat trang thai quy tac gia thanh cong",
                pricingMapper.toManagementResponse(priceRuleManagementService.updateStatus(id, request.toCommand())));
    }

    private String[] parseSort(String sort) {
        String[] parts = sort == null ? new String[0] : sort.trim().split(",", -1);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new RequestValidationException("sort", "Sap xep phai co dinh dang field,asc hoac field,desc");
        }
        return new String[] {parts[0].trim(), parts[1].trim()};
    }

    private PageResponse<PriceRuleManagementResponse> mapPage(PageResponse<PriceRuleManagementResult> source) {
        return new PageResponse<>(
                source.content().stream()
                        .map(pricingMapper::toManagementResponse)
                        .toList(),
                source.page(),
                source.size(),
                source.totalElements(),
                source.totalPages(),
                source.first(),
                source.last());
    }
}
