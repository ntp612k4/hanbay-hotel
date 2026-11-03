package com.tpanh.hotel.authentication.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(name = "GuestRegisterRequest", description = "Dữ liệu đăng ký tài khoản khách")
public record GuestRegisterRequest(
        @NotBlank(message = "Họ tên không được để trống") @Size(min = 2, max = 150, message = "Họ tên phải có từ 2 đến 150 ký tự") @Schema(example = "Nguyễn Văn A")
                String fullName,
        @NotBlank(message = "Email không được để trống") @Email(message = "Email không đúng định dạng") @Size(max = 255, message = "Email không được vượt quá 255 ký tự") @Schema(example = "guest@example.com")
                String email,
        @NotBlank(message = "Số điện thoại không được để trống") @Size(min = 9, max = 15, message = "Số điện thoại phải có từ 9 đến 15 chữ số") @Pattern(regexp = "^[0-9]+$", message = "Số điện thoại chỉ được chứa chữ số") @Schema(example = "0901234567")
                String phone,
        @NotBlank(message = "Mật khẩu không được để trống") @Size(min = 8, max = 72, message = "Mật khẩu phải có từ 8 đến 72 ký tự") @Schema(example = "Password123")
                String password,
        @NotBlank(message = "Xác nhận mật khẩu không được để trống") @Size(min = 8, max = 72, message = "Xác nhận mật khẩu phải có từ 8 đến 72 ký tự") @Schema(example = "Password123")
                String confirmPassword) {}
