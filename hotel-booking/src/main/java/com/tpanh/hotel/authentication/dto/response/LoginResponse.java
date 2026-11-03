package com.tpanh.hotel.authentication.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "LoginResponse", description = "Dữ liệu trả về khi đăng nhập thành công")
public record LoginResponse(
        @Schema(description = "Mã truy cập JWT") String accessToken,
        @Schema(description = "Mã làm mới") String refreshToken,
        @Schema(description = "Loại mã xác thực", example = "Bearer") String tokenType,
        @Schema(description = "Thời gian hết hạn tính bằng giây", example = "3600") long expiresIn,
        LoginUserView user) {

    @Schema(name = "LoginUserView")
    public record LoginUserView(
            @Schema(description = "Mã người dùng") String id,
            @Schema(description = "Tên hiển thị") String fullName,
            @Schema(description = "Email") String email,
            @Schema(description = "Số điện thoại") String phone,
            @Schema(description = "Danh sách mã vai trò", example = "[\"ADMIN\"]") List<String> roles) {}
}
