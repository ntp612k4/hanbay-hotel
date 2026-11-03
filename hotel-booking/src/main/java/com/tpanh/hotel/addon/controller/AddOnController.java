package com.tpanh.hotel.addon.controller;

import com.tpanh.hotel.addon.dto.response.AddOnCatalogResponse;
import com.tpanh.hotel.addon.service.AddOnCatalogService;
import com.tpanh.hotel.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.prefix}/add-ons")
@RequiredArgsConstructor
@Tag(name = "Dịch vụ bổ sung", description = "API danh mục dịch vụ bổ sung dành cho khách đặt phòng")
public class AddOnController {

    private final AddOnCatalogService addOnCatalogService;

    @GetMapping
    @Operation(
            summary = "Lấy danh sách dịch vụ bổ sung",
            description = "Trả về các dịch vụ bổ sung đang hoạt động để khách lựa chọn khi đặt phòng",
            security = {})
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Lấy danh sách dịch vụ bổ sung thành công")
    })
    public ResponseEntity<ApiResponse<List<AddOnCatalogResponse>>> getActiveAddOns() {
        return ResponseEntity.ok(ApiResponse.success(addOnCatalogService.getActiveAddOns()));
    }
}
