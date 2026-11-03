package com.tpanh.hotel.ai.service;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class AiDateNormalizer {

    private static final Pattern DATE_RANGE =
            Pattern.compile("(?iu)(?<!\\d)(\\d{1,2})/(\\d{1,2})(?:/(\\d{4}))?\\s*(?:-|–|đến|tới)\\s*"
                    + "(\\d{1,2})/(\\d{1,2})(?:/(\\d{4}))?(?!\\d)");

    private final Clock clock;

    public AiDateNormalizer(Clock clock) {
        this.clock = clock;
    }

    public Optional<String> contextFor(String message) {
        Matcher matcher = DATE_RANGE.matcher(message);
        if (!matcher.find()) return Optional.empty();

        LocalDate today = LocalDate.now(clock);
        try {
            int checkInYear = matcher.group(3) == null ? today.getYear() : Integer.parseInt(matcher.group(3));
            LocalDate checkIn =
                    LocalDate.of(checkInYear, Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(1)));
            if (matcher.group(3) == null && checkIn.isBefore(today)) checkIn = checkIn.plusYears(1);

            int checkOutYear = matcher.group(6) == null ? checkIn.getYear() : Integer.parseInt(matcher.group(6));
            LocalDate checkOut =
                    LocalDate.of(checkOutYear, Integer.parseInt(matcher.group(5)), Integer.parseInt(matcher.group(4)));
            if (matcher.group(6) == null && !checkOut.isAfter(checkIn)) checkOut = checkOut.plusYears(1);

            String validity = checkIn.isBefore(today)
                    ? "Check-in NẰM TRONG QUÁ KHỨ so với " + today + "; không gọi tool và yêu cầu ngày mới."
                    : "Check-in KHÔNG nằm trong quá khứ so với "
                            + today
                            + "; bắt buộc dùng chính xác hai ngày ISO này khi gọi tool.";
            return Optional.of("Khoảng ngày trong tin nhắn đã được backend chuẩn hóa: check-in "
                    + checkIn
                    + ", check-out "
                    + checkOut
                    + ". "
                    + validity);
        } catch (DateTimeException exception) {
            return Optional.of("Khoảng ngày người dùng nhập không hợp lệ; hãy yêu cầu nhập lại ngày.");
        }
    }
}
