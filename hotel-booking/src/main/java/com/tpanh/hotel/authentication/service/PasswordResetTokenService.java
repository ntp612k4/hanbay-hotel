package com.tpanh.hotel.authentication.service;

import com.tpanh.hotel.authentication.dto.request.ResetPasswordRequest;
import com.tpanh.hotel.authentication.dto.response.ResetPasswordResponse;
import com.tpanh.hotel.authentication.dto.response.ResetTokenVerificationResponse;
import com.tpanh.hotel.user.domain.User;

public interface PasswordResetTokenService {

    String issueToken(User user);

    void revokeToken(String rawToken);

    ResetTokenVerificationResponse verifyToken(String rawToken);

    ResetPasswordResponse resetPassword(ResetPasswordRequest request);
}
