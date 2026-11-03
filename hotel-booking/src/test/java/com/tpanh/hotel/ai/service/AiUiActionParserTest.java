package com.tpanh.hotel.ai.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.tpanh.hotel.ai.dto.AiUiAction;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class AiUiActionParserTest {

    private final AiUiActionParser parser = new AiUiActionParser(new ObjectMapper());

    @Test
    void extractsAndValidatesBookingAction() {
        String response =
                """
                Phòng Deluxe còn trống.
                @@UI_ACTION@@{"type":"CONTINUE_BOOKING","params":{"roomTypeId":"11111111-1111-4111-8111-111111111111","checkIn":"2026-08-20","checkOut":"2026-08-22","adults":2,"children":1}}
                """;

        AiUiActionParser.ParsedResponse parsed = parser.parse(response);

        assertThat(parsed.answer()).isEqualTo("Phòng Deluxe còn trống.");
        assertThat(parsed.action()).isPresent();
        assertThat(parsed.action().orElseThrow().type()).isEqualTo(AiUiAction.Type.CONTINUE_BOOKING);
        assertThat(parsed.action().orElseThrow().label()).isEqualTo("Tiếp tục đặt phòng");
    }

    @Test
    void rejectsActionWithMissingRequiredParameters() {
        AiUiActionParser.ParsedResponse parsed =
                parser.parse("Kết quả.\n@@UI_ACTION@@{\"type\":\"CONTINUE_BOOKING\",\"params\":{}}");

        assertThat(parsed.answer()).isEqualTo("Kết quả.");
        assertThat(parsed.action()).isEmpty();
    }

    @Test
    void acceptsFlatActionMetadataForModelTolerance() {
        String response =
                """
                Phòng đã sẵn sàng để tiếp tục.
                @@UI_ACTION@@{"type":"CONTINUE_BOOKING","roomTypeId":"11111111-1111-4111-8111-111111111111","checkIn":"2026-09-01","checkOut":"2026-09-05","adults":2,"children":1}
                """;

        AiUiActionParser.ParsedResponse parsed = parser.parse(response);

        assertThat(parsed.action()).isPresent();
        assertThat(parsed.action().orElseThrow().params().adults()).isEqualTo(2);
    }
}
