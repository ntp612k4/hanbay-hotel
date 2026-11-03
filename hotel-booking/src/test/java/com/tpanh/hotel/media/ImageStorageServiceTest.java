package com.tpanh.hotel.media;

import static org.assertj.core.api.Assertions.*;

import com.tpanh.hotel.room.exception.RoomValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class ImageStorageServiceTest {
    private final ImageStorageService storage =
            new ImageStorageService("http://localhost:1", "test-access", "test-secret", "test-images");

    @Test
    void rejectsDisguisedHtmlWithoutContactingStorage() {
        assertThatThrownBy(() -> storage.upload(new MockMultipartFile(
                        "file", "room.jpg", "image/jpeg", "<script>alert(1)</script>".getBytes())))
                .isInstanceOf(RoomValidationException.class);
    }

    @Test
    void rejectsEmptyAndOversizedImages() {
        for (byte[] bytes : new byte[][] {new byte[0], new byte[5 * 1024 * 1024 + 1]})
            assertThatThrownBy(() -> storage.upload(new MockMultipartFile("file", "room.png", "image/png", bytes)))
                    .isInstanceOf(RoomValidationException.class);
    }

    @Test
    void rejectsTraversalAndExternalUrls() {
        for (String url :
                new String[] {"http://localhost:9000/private", "/api/v1/media/../secret", "/api/v1/media/file.svg"})
            assertThatThrownBy(() -> storage.requireImage(url)).isInstanceOf(RoomValidationException.class);
    }
}
