# Database design as-built

Flyway trong `hotel-booking/src/main/resources/db/migration` là nguồn sự thật của database.

## Identity

- `users`: email, phone, password hash, full name, status và audit fields.
- `roles`: `ADMIN`, `MANAGER`, `RECEPTIONIST`, `HOUSEKEEPER`.
- `user_roles`: quan hệ staff role; tài khoản khách không có bản ghi tại đây.
- `refresh_tokens`: user, token hash, created/expired/revoked timestamps.

## Room và pricing

- `room_types`: code, base price, `capacity_adults`, `capacity_children`, `max_guests`, status.
- `room_type_images`: ảnh theo thứ tự.
- `rooms`: phòng vật lý và trạng thái.
- `price_rules`: rule phụ thu/khuyến mãi theo khoảng ngày và priority.

## Booking

- `bookings`: code, booked_by nullable, room type, ngày, adult/child count, status, total và expired_at.
- `booking_guests`: thông tin khách snapshot.
- `booking_nights`: giá snapshot từng đêm.
- `booking_night_price_rules`: các rule đã áp dụng cho từng đêm.
- `booking_status_histories`: lịch sử trạng thái.
- `add_on_services`: catalog dịch vụ active và đơn giá hiện tại.
- `booking_add_ons`: `service_id`, `quantity`, `unit_price`, `total_price`; đây là snapshot không thay đổi khi catalog đổi giá.

Booking status hiện có: `PENDING`, `CONFIRMED`, `CANCELLED`, `CHECKED_IN`, `CHECKED_OUT`, `EXPIRED`.

## Business constraints

- `check_out_date > check_in_date`.
- Adults, children và tổng khách phải nằm trong ba capacity của room type.
- Child `<= 12` miễn phí tiền phòng trong MVP nhưng vẫn chiếm occupancy.
- Add-on quantity từ 1 đến 20 trên API; tổng add-on là `unit_price × quantity`.
- Một service chỉ xuất hiện một lần trong một booking.
- `add_on_services.code` là mã nghiệp vụ duy nhất và không thay đổi sau khi tạo.
- Dịch vụ không còn cung cấp được chuyển `active = false`, không xóa vật lý nếu đã được booking tham chiếu.
- Thay đổi tên, mô tả, giá hoặc trạng thái catalog không làm thay đổi `unit_price` và `total_price` đã snapshot trong `booking_add_ons`.
- PENDING chưa hết hạn và CONFIRMED/CHECKED_IN chiếm inventory khi overlap.
- Housekeeping MVP dùng trực tiếp state machine của `rooms`: `DIRTY -> CLEANING -> AVAILABLE`.
  Không tạo bảng task/phân công vì chức năng đó không nằm trong yêu cầu hiện tại.

## Reporting

Định nghĩa query và công thức cho Manager dashboard được mô tả tại
[`18-manager-dashboard-metrics.md`](18-manager-dashboard-metrics.md). Reporting sử dụng
`payment_transactions` cho doanh thu, `booking_nights` cho occupied room nights và
`bookings.created_at` cho thống kê booking.
# Policy RAG vector storage

Migration `V202608301600` bật extension `vector`, tạo `policy_vector_store` theo schema tương thích
Spring AI (`id text`, `content text`, `metadata json`, `embedding vector(1536)`) và HNSW cosine index.
Dimension 1536 khớp production model `text-embedding-3-small`; thay model/dimension cần migration mới.
`policy_knowledge_ingestion` lưu SHA-256 checksum, số chunk và thời điểm ingest để re-ingestion
idempotent. Đây là demo policy corpus, không phải dữ liệu chính sách của khách sạn đang vận hành.
Migration `V202609041200` adds a complete-or-null bank-transfer destination to the existing `payment_refunds` table. OTP, PIN, CVV and banking credentials are not collected.
