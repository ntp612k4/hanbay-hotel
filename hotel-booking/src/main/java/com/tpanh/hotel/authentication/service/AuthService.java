package com.tpanh.hotel.authentication.service;

import com.tpanh.hotel.authentication.dto.request.ForgotPasswordRequest;
import com.tpanh.hotel.authentication.dto.request.GuestRegisterRequest;
import com.tpanh.hotel.authentication.dto.request.LoginRequest;
import com.tpanh.hotel.authentication.dto.request.ResetPasswordRequest;
import com.tpanh.hotel.authentication.dto.response.ForgotPasswordResponse;
import com.tpanh.hotel.authentication.dto.response.GuestRegisterResponse;
import com.tpanh.hotel.authentication.dto.response.LoginResponse;
import com.tpanh.hotel.authentication.dto.response.ResetPasswordResponse;
import com.tpanh.hotel.authentication.dto.response.ResetTokenVerificationResponse;

public interface AuthService {

    ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request);

    ResetTokenVerificationResponse verifyResetToken(String rawToken);

    ResetPasswordResponse resetPassword(ResetPasswordRequest request);

    LoginResponse login(LoginRequest request, String clientIp);

    GuestRegisterResponse registerGuest(GuestRegisterRequest request);
}
