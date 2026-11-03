package com.tpanh.hotel.media;

import com.tpanh.hotel.room.exception.RoomValidationException;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import java.io.*;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageStorageService {
    public static final String PREFIX = "/api/v1/media/";
    private final MinioClient client;
    private final String bucket;

    public ImageStorageService(
            @Value("${storage.minio.endpoint:http://localhost:9000}") String endpoint,
            @Value("${storage.minio.access-key:local-minio}") String access,
            @Value("${storage.minio.secret-key:local-minio-password}") String secret,
            @Value("${storage.minio.bucket:hotel-images}") String bucket) {
        this.client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(access, secret)
                .build();
        this.bucket = bucket;
    }

    public String upload(MultipartFile file) {
        if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024)
            throw new RoomValidationException("Ảnh phải có kích thước từ 1 byte đến 5 MB.");
        try {
            byte[] bytes = file.getBytes();
            String format;
            try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw new RoomValidationException("Chỉ hỗ trợ ảnh JPEG hoặc PNG hợp lệ.");
                var reader = readers.next();
                try {
                    reader.setInput(input);
                    format = reader.getFormatName().toLowerCase(java.util.Locale.ROOT);
                    if (!format.equals("jpeg") && !format.equals("png"))
                        throw new RoomValidationException("Chỉ hỗ trợ JPEG hoặc PNG.");
                    if ((long) reader.getWidth(0) * reader.getHeight(0) > 25000000)
                        throw new RoomValidationException("Ảnh vượt quá 25 megapixel.");
                    if (reader.read(0) == null) throw new RoomValidationException("Ảnh không hợp lệ.");
                } finally {
                    reader.dispose();
                }
            }
            String key = UUID.randomUUID() + "." + (format.equals("jpeg") ? "jpg" : "png");
            ensureBucket();
            client.putObject(PutObjectArgs.builder().bucket(bucket).object(key).contentType("image/" + format).stream(
                            new ByteArrayInputStream(bytes), bytes.length, -1)
                    .build());
            return PREFIX + key;
        } catch (RoomValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ImageStorageException("Không thể lưu ảnh. Vui lòng thử lại.", e);
        }
    }

    private synchronized void ensureBucket() throws Exception {
        if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build()))
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
    }

    public void requireImage(String url) {
        if (!url.startsWith(PREFIX)) throw new RoomValidationException("Vui lòng tải ảnh lên hệ thống.");
        String key = url.substring(PREFIX.length());
        validateKey(key);
        try {
            client.statObject(
                    StatObjectArgs.builder().bucket(bucket).object(key).build());
        } catch (ErrorResponseException e) {
            throw new RoomValidationException("Ảnh không tồn tại.");
        } catch (Exception e) {
            throw new ImageStorageException("Không thể kiểm tra ảnh.", e);
        }
    }

    public byte[] read(String key) {
        validateKey(key);
        try (var input = client.getObject(
                GetObjectArgs.builder().bucket(bucket).object(key).build())) {
            return input.readAllBytes();
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) throw new ImageNotFoundException();
            throw new ImageStorageException("Không thể đọc ảnh.", e);
        } catch (Exception e) {
            throw new ImageStorageException("Không thể đọc ảnh.", e);
        }
    }

    private void validateKey(String key) {
        if (!key.matches("[a-f0-9-]{36}\\.(jpg|png)")) throw new RoomValidationException("Đường dẫn ảnh không hợp lệ.");
    }
}
