package com.tpanh.hotel.booking.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Service;

@Service
public class BookingAccessTokenService {

    private static final int TOKEN_BYTES = 32;
    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hash(String token) {
        try {
            return java.util.HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Máy chủ không hỗ trợ SHA-256", exception);
        }
    }

    public boolean matches(String rawToken, String expectedHash) {
        if (rawToken == null || rawToken.isBlank() || expectedHash == null) {
            return false;
        }
        return MessageDigest.isEqual(
                hash(rawToken).getBytes(StandardCharsets.US_ASCII), expectedHash.getBytes(StandardCharsets.US_ASCII));
    }
}
