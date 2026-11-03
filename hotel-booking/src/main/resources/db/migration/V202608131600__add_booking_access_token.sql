ALTER TABLE bookings
    ADD COLUMN access_token_hash VARCHAR(64);

COMMENT ON COLUMN bookings.access_token_hash IS
    'SHA-256 của mã truy cập dùng để bảo vệ thao tác checkout cho khách chưa đăng nhập';
