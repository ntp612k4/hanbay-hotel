package com.tpanh.hotel.authentication.service.impl;

import com.tpanh.hotel.authentication.config.PasswordResetProperties;
import com.tpanh.hotel.authentication.repository.PasswordResetTokenRepository;
import com.tpanh.hotel.authentication.security.TokenHashService;
import com.tpanh.hotel.authentication.service.EmailService;
import java.net.URI;
import java.time.Clock;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;
    private final PasswordResetProperties passwordResetProperties;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final TokenHashService tokenHashService;
    private final Clock clock;

    @Override
    @Async("passwordResetMailExecutor")
    @Transactional
    public CompletableFuture<Void> sendPasswordResetEmail(String email, String fullName, String rawToken) {
        URI resetLink = UriComponentsBuilder.fromUriString(passwordResetProperties.frontendUrl())
                .queryParam("token", rawToken)
                .build()
                .encode()
                .toUri();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(passwordResetProperties.fromAddress());
        message.setTo(email);
        message.setSubject("Đặt lại mật khẩu HanBay Hotel");
        message.setText(
                """
                Xin chào %s,

                Bạn có thể đặt lại mật khẩu tại liên kết sau:
                %s

                Liên kết có hiệu lực trong %d phút. Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này.
                Nếu cần hỗ trợ, vui lòng liên hệ %s.
                """
                        .formatted(
                                fullName,
                                resetLink,
                                passwordResetProperties.expirationMinutes(),
                                passwordResetProperties.supportEmail()));
        try {
            mailSender.send(message);
            return CompletableFuture.completedFuture(null);
        } catch (RuntimeException exception) {
            passwordResetTokenRepository.revokeByTokenHash(tokenHashService.hash(rawToken), clock.instant());
            LOGGER.error("Không thể gửi email đặt lại mật khẩu; mã xác thực đã được thu hồi", exception);
            return CompletableFuture.failedFuture(exception);
        }
    }
}
