# HANBAY HOTEL — Backend

Smart Hotel Reservation & Property Management System (PMS).

Hệ thống số hóa toàn bộ quy trình vận hành khách sạn: tìm phòng, đặt phòng trực tuyến, thanh toán, check-in/check-out, housekeeping, dynamic pricing và trợ lý AI.

> Chính sách RAG nằm tại [`src/main/resources/knowledge-base`](src/main/resources/knowledge-base).

---

## 1. Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot |
| Security | Spring Security + JWT |
| Persistence | Spring Data JPA, PostgreSQL |
| Migration | Flyway |
| AI | Spring AI |
| Build | Maven (Maven Wrapper) |
| Containerization | Docker, Docker Compose |
| API Docs | Springdoc OpenAPI (Swagger UI) |

Kiến trúc: **Modular Monolith**, thiết kế theo hướng có thể tách microservices trong tương lai.

---

## 2. Yêu cầu môi trường

- JDK 17
- Docker & Docker Compose
- Git
- Không cần cài Maven riêng — dự án đã có Maven Wrapper (`./mvnw`)

Kiểm tra môi trường:

```bash
java -version
docker compose version
./mvnw --version
```

Trên Windows, thay `./mvnw` bằng `mvnw.cmd`.

---

## 3. Cấu trúc package

```text
com.tpanh.hotel
├── authentication
├── user
├── room
├── booking
├── payment
├── pricing
├── housekeeping
├── ai
└── common
```

Các module tổ chức theo nghiệp vụ; tùy module có `controller`, `service`,
`repository`, `domain`/`entity`, `dto`, `mapper` và `exception`.


---

## 4. Chạy backend để demo local

Hướng dẫn cấu hình và chạy cả frontend, Docker, ngrok/SePay nằm ở
[README chính](../README.md#chạy-demo-local-trên-windows).

PostgreSQL, Redis, MinIO và pgAdmin chạy trong Docker. Bật chúng trước theo
README chính; backend sử dụng lại database hiện có. Dừng backend khác đang giữ
cổng `8081`, rồi chạy trong thư mục `hotel-booking`:

```powershell
.\mvnw.cmd spring-boot:run `
    "-Dspring-boot.run.profiles=local" `
    "-Dspring-boot.run.arguments=--server.port=8081 --spring.docker.compose.enabled=false"
```

Cấu hình riêng nằm trong `src/main/resources/application-local.yml` và `.env`
ở gốc repository; không commit hai file này. Profile local đã có trên máy sau
khi sao chép cấu hình cũ. Nếu clone mới, làm bước cấu hình một lần trong README chính.

Chờ log `Started HotelBookingApplication`. Backend dùng cổng `8081`; API có
prefix `/api/v1`. `spring.docker.compose.enabled=false` ngăn Spring tự bật cả
stack và tránh tranh chấp cổng với Maven/Vite đang chạy local.

Dùng `Ctrl+C` để dừng backend; các dịch vụ Docker tiếp tục chạy và giữ dữ liệu.

---

## 5. Database Migration

Flyway chạy tự động khi backend khởi động.

Thư mục migration:

```text
src/main/resources/db/migration/
```

Đặt tên file theo định dạng:

```text
V<yyyyMMddHHmm>__<description>.sql
```

---

## 6. API Documentation

Sau khi chạy backend, truy cập Swagger UI:

```text
http://localhost:8081/swagger-ui/index.html
```

Base URL của API:

```text
/api/v1
```


---

## 7. Testing

Format code (Palantir Java Format):

```bash
./mvnw spotless:apply
```

Chạy toàn bộ test:

```bash
./mvnw test
```

Chạy một test class / method:

```bash
./mvnw -Dtest=BookingServiceTest test
./mvnw -Dtest=BookingServiceTest#shouldRejectOverlappingBooking test
```

Chạy đầy đủ pipeline giống CI trước khi tạo PR:

```bash
./mvnw --batch-mode clean verify
```


---

## 8. Business Highlights

- **Availability Calculation**: tính phòng trống bằng overlapping booking, không dựa vào trạng thái phòng hiện tại.
- **Concurrency Control**: pessimistic lock khi xác nhận booking, tránh overbooking.
- **Booking Hold**: booking `PENDING` giữ chỗ tối đa 15 phút, tự động `CANCELLED` nếu quá hạn.
- **Payment Webhook Idempotency**: đảm bảo một giao dịch chỉ được xử lý một lần.
- **Dynamic Pricing**: tính giá theo từng đêm dựa trên lịch giá (cuối tuần, ngày lễ).
- **AI Assistant**: hỗ trợ gợi ý hạng phòng và trả lời chính sách khách sạn (không tự tạo booking, không xử lý thanh toán).


---

## 9. Đóng góp

Vui lòng đọc [`CONTRIBUTING.md`](../CONTRIBUTING.md) trước khi tạo Pull Request — bao gồm quy ước branch, commit, Flyway migration và checklist review.

---

## 10. Tài liệu liên quan

- [Hướng dẫn demo đầy đủ](../README.md#chạy-demo-local-trên-windows).
- [Chính sách RAG](src/main/resources/knowledge-base).
- [HTTP request examples](http/README.md).
- [Dockerfile MinIO và cách build](../deploy/minio/README.md).
