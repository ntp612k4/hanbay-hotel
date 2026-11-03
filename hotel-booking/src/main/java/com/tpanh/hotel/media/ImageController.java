package com.tpanh.hotel.media;

import com.tpanh.hotel.common.response.ApiResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/media")
public class ImageController {
    private final ImageStorageService storage;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Map<String, String>> upload(@RequestParam MultipartFile file) {
        return ApiResponse.success(Map.of("url", storage.upload(file)));
    }

    @GetMapping("/{key}")
    public ResponseEntity<byte[]> read(@PathVariable String key) {
        return ResponseEntity.ok()
                .contentType(key.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG)
                .cacheControl(
                        CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable())
                .header("X-Content-Type-Options", "nosniff")
                .body(storage.read(key));
    }
}
