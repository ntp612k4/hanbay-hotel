package com.tpanh.hotel.authentication.service;

import com.tpanh.hotel.authentication.dto.request.RefreshTokenRequest;
import com.tpanh.hotel.authentication.dto.response.RefreshTokenResponse;
import com.tpanh.hotel.user.domain.User;

public interface RefreshTokenService {

    RefreshTokenResponse refreshAccessToken(RefreshTokenRequest request);

    String issueRefreshToken(User user);
}
