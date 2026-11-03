package com.tpanh.hotel.authentication.controller;

import com.tpanh.hotel.authentication.dto.request.ForgotPasswordRequest;
import com.tpanh.hotel.authentication.dto.request.GuestRegisterRequest;
import com.tpanh.hotel.authentication.dto.request.LoginRequest;
import com.tpanh.hotel.authentication.dto.request.RefreshTokenRequest;
import com.tpanh.hotel.authentication.dto.request.ResetPasswordRequest;
import com.tpanh.hotel.authentication.dto.request.VerifyResetTokenRequest;
import com.tpanh.hotel.authentication.dto.response.ForgotPasswordResponse;
import com.tpanh.hotel.authentication.dto.response.GuestRegisterResponse;
import com.tpanh.hotel.authentication.dto.response.LoginResponse;
import com.tpanh.hotel.authentication.dto.response.RefreshTokenResponse;
import com.tpanh.hotel.authentication.dto.response.ResetPasswordResponse;
import com.tpanh.hotel.authentication.dto.response.ResetTokenVerificationResponse;
import com.tpanh.hotel.authentication.service.AuthService;
import com.tpanh.hotel.authentication.service.RefreshTokenService;
import com.tpanh.hotel.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.prefix}/auth")
@Tag(name = "Xác thực")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(AuthService authService, RefreshTokenService refreshTokenService) {
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/forgot-password")
    @Operation(
            summary = "Quên mật khẩu",
            description = "Gửi hướng dẫn đặt lại mật khẩu nếu tài khoản tồn tại",
            security = {},
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            required = true,
                            content = @Content(schema = @Schema(implementation = ForgotPasswordRequest.class))))
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Tiếp nhận yêu cầu thành công"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Email không hợp lệ",
                content = @Content)
    })
    public ApiResponse<ForgotPasswordResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ApiResponse.success(
                "Nếu email tồn tại, hướng dẫn đặt lại mật khẩu sẽ được gửi", authService.forgotPassword(request));
    }

    @PostMapping("/reset-password/verify")
    @Operation(
            summary = "Kiểm tra mã đặt lại mật khẩu",
            description = "Kiểm tra mã còn hiệu lực trước khi hiển thị biểu mẫu đặt lại mật khẩu",
            security = {})
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Mã đặt lại mật khẩu hợp lệ"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Mã đặt lại mật khẩu không hợp lệ hoặc đã hết hiệu lực",
                content = @Content)
    })
    public ApiResponse<ResetTokenVerificationResponse> verifyResetToken(
            @Valid @RequestBody VerifyResetTokenRequest request) {
        return ApiResponse.success("Mã đặt lại mật khẩu hợp lệ", authService.verifyResetToken(request.token()));
    }

    @PostMapping("/reset-password")
    @Operation(
            summary = "Đặt lại mật khẩu",
            description = "Đặt mật khẩu mới bằng mã còn hiệu lực và thu hồi các phiên đăng nhập cũ",
            security = {},
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            required = true,
                            content = @Content(schema = @Schema(implementation = ResetPasswordRequest.class))))
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Đặt lại mật khẩu thành công"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Dữ liệu hoặc mã đặt lại mật khẩu không hợp lệ",
                content = @Content)
    })
    public ApiResponse<ResetPasswordResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ApiResponse.success("Đặt lại mật khẩu thành công", authService.resetPassword(request));
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "Đăng nhập",
            description = "Xác thực bằng email hoặc số điện thoại",
            security = {},
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            required = true,
                            content = @Content(schema = @Schema(implementation = LoginRequest.class))))
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Đăng nhập thành công"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Dữ liệu không hợp lệ",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Thông tin đăng nhập không đúng",
                content = @Content)
    })
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.success("Đăng nhập thành công", authService.login(request, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/register")
    @Operation(
            summary = "Đăng ký tài khoản",
            description = "Tạo tài khoản khách mới",
            security = {},
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            required = true,
                            content = @Content(schema = @Schema(implementation = GuestRegisterRequest.class))))
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Đăng ký thành công"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Dữ liệu không hợp lệ",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "Email hoặc số điện thoại đã tồn tại",
                content = @Content)
    })
    public ResponseEntity<ApiResponse<GuestRegisterResponse>> registerGuest(
            @Valid @RequestBody GuestRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đăng ký khách hàng thành công", authService.registerGuest(request)));
    }

    @PostMapping("/refresh-token")
    @Operation(
            summary = "Làm mới mã truy cập",
            security = {},
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            required = true,
                            content = @Content(schema = @Schema(implementation = RefreshTokenRequest.class))))
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Làm mới token thành công"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Mã làm mới không hợp lệ hoặc đã hết hạn",
                content = @Content)
    })
    public ApiResponse<RefreshTokenResponse> refreshAccessToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success("Làm mới token thành công", refreshTokenService.refreshAccessToken(request));
    }
}
