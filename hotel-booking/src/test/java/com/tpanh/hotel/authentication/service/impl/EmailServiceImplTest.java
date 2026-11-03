package com.tpanh.hotel.authentication.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tpanh.hotel.authentication.config.PasswordResetProperties;
import com.tpanh.hotel.authentication.repository.PasswordResetTokenRepository;
import com.tpanh.hotel.authentication.security.TokenHashService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

class EmailServiceImplTest {

    @Test
    void shouldRevokeResetTokenWhenSmtpDeliveryFails() {
        JavaMailSender mailSender = org.mockito.Mockito.mock(JavaMailSender.class);
        PasswordResetTokenRepository repository = org.mockito.Mockito.mock(PasswordResetTokenRepository.class);
        TokenHashService hashService = org.mockito.Mockito.mock(TokenHashService.class);
        Instant now = Instant.parse("2026-08-10T10:00:00Z");
        PasswordResetProperties properties = new PasswordResetProperties(
                15,
                "http://localhost:5173/reset-password",
                "no-reply@example.com",
                "support@example.com",
                60,
                5,
                20,
                10,
                7);
        EmailServiceImpl service =
                new EmailServiceImpl(mailSender, properties, repository, hashService, Clock.fixed(now, ZoneOffset.UTC));
        org.mockito.Mockito.doThrow(new MailSendException("SMTP unavailable"))
                .when(mailSender)
                .send(org.mockito.ArgumentMatchers.any(org.springframework.mail.SimpleMailMessage.class));
        when(hashService.hash("raw-token")).thenReturn("hashed-token");

        var future = service.sendPasswordResetEmail("guest@example.com", "Khách hàng", "raw-token");

        assertThatThrownBy(future::join).hasCauseInstanceOf(MailSendException.class);
        verify(repository).revokeByTokenHash("hashed-token", now);
    }
}
