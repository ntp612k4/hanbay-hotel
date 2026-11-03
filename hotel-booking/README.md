# HANBAY HOTEL — Backend

Smart Hotel Reservation & Property Management System (PMS).

Hệ thống số hóa toàn bộ quy trình vận hành khách sạn: tìm phòng, đặt phòng trực tuyến, thanh toán, check-in/check-out, housekeeping, dynamic pricing và trợ lý AI.

> Tài liệu chi tiết về nghiệp vụ, kiến trúc, database, API... xem tại thư mục [`/docs`](./docs).

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
com.hanbay.hotel
├── auth
├── user
├── room
├── booking
├── payment
├── pricing
├── housekeeping
├── ai
└── common
```

Mỗi module gồm: `controller`, `service`, `repository`, `entity`, `dto`, `mapper`, `exception`.

Chi tiết quy ước đặt tên, REST convention, response format, exception handling: xem [`docs/12-coding-convention.md`](./docs/12-coding-convention.md).

---

## 4. Chạy dự án

### 4.1. Chạy bằng Docker Compose (khuyến nghị)

Dự án sử dụng Spring Boot Docker Compose support — Spring Boot tự động khởi tạo PostgreSQL và chạy Flyway migration khi ứng dụng khởi động, không cần chạy `docker compose up` thủ công.

```bash
./mvnw spring-boot:run
```

Kiểm tra container:

```bash
docker compose ps
```

Dừng service:

```bash
docker compose down
```

### 4.2. Chạy local trỏ tới database có sẵn

Thiết lập biến môi trường (qua IDE Run Configuration hoặc export trực tiếp):

```env
DB_URL=jdbc:postgresql://localhost:5432/hanbay_hotel
DB_USERNAME=postgres
DB_PASSWORD=postgres
```

Sau đó chạy:

```bash
./mvnw spring-boot:run
```

Ứng dụng mặc định chạy tại `http://localhost:8080`, health check tại `http://localhost:8080/actuator/health`.

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
http://localhost:8080/swagger-ui/index.html
```

Base URL của API:

```text
/api/v1
```

Chi tiết đặc tả API: [`docs/06-api-specification.md`](./docs/06-api-specification.md)

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

Chiến lược test chi tiết (Unit / Integration / API / Coverage target): xem [`docs/11-testing-strategy.md`](./docs/11-testing-strategy.md).

---

## 8. Business Highlights

- **Availability Calculation**: tính phòng trống bằng overlapping booking, không dựa vào trạng thái phòng hiện tại.
- **Concurrency Control**: pessimistic lock khi xác nhận booking, tránh overbooking.
- **Booking Hold**: booking `PENDING` giữ chỗ tối đa 15 phút, tự động `CANCELLED` nếu quá hạn.
- **Payment Webhook Idempotency**: đảm bảo một giao dịch chỉ được xử lý một lần.
- **Dynamic Pricing**: tính giá theo từng đêm dựa trên lịch giá (cuối tuần, ngày lễ).
- **AI Assistant**: hỗ trợ gợi ý hạng phòng và trả lời chính sách khách sạn (không tự tạo booking, không xử lý thanh toán).

Chi tiết: [`docs/08-concurrency-design.md`](./docs/08-concurrency-design.md), [`docs/09-ai-integration.md`](./docs/09-ai-integration.md).

---

## 9. Đóng góp

Vui lòng đọc [`CONTRIBUTING.md`](./CONTRIBUTING.md) trước khi tạo Pull Request — bao gồm quy ước branch, commit, Flyway migration và checklist review.

---

## 10. Tài liệu liên quan

| Tài liệu | Nội dung |
|---|---|
| `01-project-overview.md` | Tổng quan dự án |
| `02-business-requirements.md` | Yêu cầu nghiệp vụ (BRD) |
| `03-software-requirements.md` | Yêu cầu phần mềm (SRS) |
| `04-system-architecture.md` | Kiến trúc hệ thống |
| `05-database-design.md` | Thiết kế database |
| `06-api-specification.md` | Đặc tả API |
| `07-business-workflows.md` | Luồng nghiệp vụ |
| `08-concurrency-design.md` | Thiết kế concurrency |
| `09-ai-integration.md` | Tích hợp AI |
| `10-deployment-guide.md` | Hướng dẫn triển khai |
| `11-testing-strategy.md` | Chiến lược test |
| `12-coding-convention.md` | Quy ước code |
| `13-git-workflow.md` | Quy trình Git |
| `14-sprint-report-template.md` | Mẫu báo cáo sprint |
| `15-contribution-guide.md` | Hướng dẫn đóng góp (bản mở rộng) |
| `16-authentication-jwt.md` | Đặc tả xác thực JWT và API guest login |
