package com.tpanh.hotel.authentication.scheduler;

import com.tpanh.hotel.authentication.config.PasswordResetProperties;
import com.tpanh.hotel.authentication.repository.PasswordResetTokenRepository;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PasswordResetTokenCleanupScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordResetTokenCleanupScheduler.class);

    private final PasswordResetTokenRepository repository;
    private final PasswordResetProperties properties;
    private final Clock clock;

    @Scheduled(cron = "${auth.password-reset.cleanup-cron:0 0 3 * * *}")
    @Transactional
    public void deleteExpiredTokens() {
        int deleted =
                repository.deleteExpiredBefore(clock.instant().minus(properties.retentionDays(), ChronoUnit.DAYS));
        if (deleted > 0) {
            LOGGER.info("Đã xóa {} mã đặt lại mật khẩu hết thời gian lưu trữ", deleted);
        }
    }
}
