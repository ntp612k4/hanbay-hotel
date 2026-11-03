package com.tpanh.hotel.media;

import com.tpanh.hotel.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class ImageNotFoundException extends DomainException {
    public ImageNotFoundException() {
        super("IMAGE_NOT_FOUND", "Không tìm thấy ảnh.", HttpStatus.NOT_FOUND);
    }
}
