# HANBAY HOTEL

Hệ thống đặt phòng và quản lý khách sạn: tìm phòng, đặt phòng trực tuyến,
thanh toán SePay, nhận/trả phòng, buồng phòng, bảo trì, quản lý tài khoản và trợ lý AI.

Backend theo kiến trúc **Modular Monolith**: một ứng dụng Spring Boot chia module
nghiệp vụ. Frontend dùng React và TypeScript. PostgreSQL lưu dữ liệu nghiệp vụ và
vector RAG, Redis phục vụ phiên/token và bộ nhớ hội thoại, MinIO lưu ảnh tải lên.

## Công nghệ

- Backend: Java 17, Spring Boot, Spring Security/JWT, Spring Data JPA, Flyway, Spring AI.
- Frontend: React, TypeScript, Vite, React Router, TanStack Query, Ant Design.
- Hạ tầng local: Docker Desktop, PostgreSQL/pgvector, Redis, MinIO, pgAdmin.
- Demo thanh toán: ngrok và SePay Test mode.

## Cấu trúc repository

```text
hanbay-hotel/
├── client/                         # Frontend React
├── hotel-booking/                  # Backend Spring Boot
│   └── src/main/resources/
│       ├── db/migration/           # Flyway migrations
│       └── knowledge-base/         # Tài liệu chính sách RAG
├── deploy/minio/                   # Dockerfile build MinIO từ source chính thức
├── deploy/vps/                     # Cấu hình triển khai VPS
├── .github/workflows/
├── docker-compose.yml
├── .env.example
└── README.md
```

## Chạy demo local trên Windows

Backend và frontend chạy trong hai terminal riêng. Docker chạy PostgreSQL,
Redis, MinIO và pgAdmin. Terminal ngrok giữ kết nối để SePay gọi webhook vào backend.

### 1. Cấu hình một lần

Yêu cầu: JDK 17, Node.js đáp ứng `^20.19.0 || >=22.12.0`, Docker Desktop đang
bật Linux containers và ngrok đã được cấu hình tài khoản.

```powershell
cd D:\FPT\Mock-Project\hanbay-hotel
java -version
node -v
docker compose version
ngrok version
```

Các file cấu hình riêng trên máy:

| File | Nội dung |
|---|---|
| `.env` | Database, Redis, JWT, OpenAI, email, SePay và MinIO |
| `hotel-booking/src/main/resources/application-local.yml` | Cấu hình backend profile `local` |
| `client/.env.local` | Cấu hình Vite; dùng `VITE_API_BASE_URL=/api/v1` |

Máy đã chuyển từ `hotel-booking-system` có thể tiếp tục dùng các file đã sao chép.
Không ghi đè `.env` hoặc `application-local.yml` đang hoạt động bằng giá trị mẫu.
Những file này được Git bỏ qua; khi clone lên máy khác cần tạo lại cấu hình riêng.

Nếu chưa có file, chỉ tạo những file còn thiếu:

```powershell
cd D:\FPT\Mock-Project\hanbay-hotel
if (-not (Test-Path -LiteralPath '.env')) {
    Copy-Item -LiteralPath '.env.example' -Destination '.env'
}
if (-not (Test-Path -LiteralPath 'client/.env.local')) {
    Copy-Item -LiteralPath 'client/.env.example' -Destination 'client/.env.local'
}
if (-not (Test-Path -LiteralPath 'hotel-booking/src/main/resources/application-local.yml')) {
    Copy-Item -LiteralPath 'hotel-booking/src/main/resources/application-prod.yml' `
        -Destination 'hotel-booking/src/main/resources/application-local.yml'
}
```

Nếu tạo `application-local.yml` từ profile prod như trên, bổ sung vào `.env` các
biến dưới đây với thông tin thật của môi trường demo. `DB_USERNAME` và
`DB_PASSWORD` phải khớp database đang chạy; mật khẩu không được đổi chỉ vì đổi thư mục source.

```dotenv
DB_URL=jdbc:postgresql://localhost:5432/hotelbooking_db
DB_USERNAME=hotelbooking_user
DB_PASSWORD=<mật khẩu PostgreSQL đang sử dụng>
SEPAY_BANK_CODE=<mã ngân hàng nhận tiền>
SEPAY_BANK_ACCOUNT_NUMBER=<số tài khoản nhận tiền>
SEPAY_BANK_ACCOUNT_NAME=<tên tài khoản nhận tiền>
SEPAY_WEBHOOK_API_KEY=<khóa webhook khớp cấu hình SePay>
```

Điền các biến JWT và OpenAI trong `.env.example` trước khi chạy. Demo gửi email
cần cấu hình `RESEND_API_KEY`; demo RAG cần `OPENAI_API_KEY` hợp lệ.

### 2. Khởi động các dịch vụ Docker

Với máy đang dùng các container cũ:

```powershell
cd D:\FPT\Mock-Project\hanbay-hotel
docker start hotelbooking_db hotelbooking_redis hotelbooking-pgadmin
docker compose -p hotelbooking up -d --build --no-deps minio
docker ps
```

Hai bản source dùng chung Compose project `hotelbooking` và các volume
`hotelbooking_postgres_data`, `hotelbooking_redis_data`, `hotelbooking_pgadmin_data`.
Đổi thư mục hoặc repository Git không làm mất dữ liệu trong các volume này.

Trên máy mới chưa có các container, thay lệnh `docker start` bằng:

```powershell
docker compose -p hotelbooking up -d postgres redis pgadmin
```

MinIO được build tại máy từ một phiên bản source upstream cố định, vì image
`minio/minio:RELEASE.2025-09-07T16-13-09Z` cũ không còn tải được từ registry.
Lần build đầu cần tải dependencies; các lần sau dùng cache. Xem
[`deploy/minio/README.md`](deploy/minio/README.md).

### 3. Terminal backend

Dừng backend cũ đang dùng cổng `8081` trước khi chạy. Sau đó mở terminal:

```powershell
cd D:\FPT\Mock-Project\hanbay-hotel\hotel-booking
.\mvnw.cmd spring-boot:run `
    "-Dspring-boot.run.profiles=local" `
    "-Dspring-boot.run.arguments=--server.port=8081 --spring.docker.compose.enabled=false"
```

Chờ log `Started HotelBookingApplication`. Backend dùng các dịch vụ Docker đã
bật; không tự khởi động cả stack backend/frontend. Flyway tự chạy migration khi
backend khởi động. Giữ terminal mở trong suốt buổi demo.

### 4. Terminal frontend

Mở terminal mới:

```powershell
cd D:\FPT\Mock-Project\hanbay-hotel\client
npm run dev -- --port 5173 --strictPort
```

Nếu chưa có `node_modules`, chạy `npm ci` trước. `--strictPort` giúp Vite báo lỗi
khi cổng bận thay vì tự chuyển sang `5174`. Vite chuyển request `/api` và
`/actuator` sang `http://localhost:8081` theo `client/vite.config.ts`.

Mở `http://localhost:5173` và giữ terminal frontend mở.

### 5. Terminal ngrok và webhook SePay

Mở terminal thứ ba:

```powershell
ngrok http 8081
```

Sao chép địa chỉ HTTPS tại dòng `Forwarding`. Cấu hình URL webhook trong SePay:

```text
https://<domain-ngrok>/api/v1/payments/sepay/webhook
```

- Ngrok trỏ vào backend `8081`.
- Cấu hình xác thực API Key trong SePay khớp `SEPAY_WEBHOOK_API_KEY` của backend.
  Backend nhận header `Authorization: Apikey <khóa webhook>`.
- Nếu domain ngrok thay đổi, cập nhật URL webhook trong SePay trước khi demo lại.
- Trong SePay Test mode, dùng đúng nội dung chuyển khoản và số tiền từ yêu cầu
  thanh toán còn hạn của booking hiện tại; không dùng lại mã đã hết hạn.
- Theo dõi request webhook tại `http://localhost:4040` và kết quả xử lý trong
  màn hình giám sát thanh toán của ứng dụng.

### Địa chỉ truy cập

| Dịch vụ | Địa chỉ local |
|---|---|
| Frontend | http://localhost:5173 |
| Quản lý tài khoản (ADMIN) | http://localhost:5173/admin/users |
| Backend API | http://localhost:8081/api/v1 |
| Health endpoint | http://localhost:8081/actuator/health |
| Swagger UI | http://localhost:8081/swagger-ui/index.html |
| pgAdmin | http://localhost:5050 |
| MinIO API | http://localhost:9000 |
| MinIO Console | http://localhost:9001 |
| Ngrok Inspector (khi ngrok chạy) | http://localhost:4040 |

### Cổng bận và dừng demo

Kiểm tra tiến trình đang giữ cổng:

```powershell
Get-NetTCPConnection -State Listen -LocalPort 8081,5173 -ErrorAction SilentlyContinue |
    Select-Object LocalPort, OwningProcess
```

Nếu backend cũ từ `hotel-booking-system` vẫn chạy nền bằng file
`account-management-runtime.args`, có thể dừng đúng tiến trình đó:

```powershell
Get-CimInstance Win32_Process |
    Where-Object {
        $_.Name -eq 'java.exe' -and
        $_.CommandLine -like '*hotel-booking-system*account-management-runtime.args*'
    } |
    ForEach-Object { Stop-Process -Id $_.ProcessId }
```

Dùng `Ctrl+C` trong từng terminal backend, frontend và ngrok để dừng demo.
Có thể giữ Docker chạy cho buổi demo tiếp theo. Không chạy
`docker compose down -v` hoặc xóa volume nếu cần giữ dữ liệu.

## Quản lý tài khoản và phân quyền

Đăng nhập bằng tài khoản có vai trò `ADMIN`, chọn **Tài khoản và phân quyền**
hoặc truy cập `/admin/users`. Admin có thể tạo tài khoản, tìm theo họ tên/email/
số điện thoại, lọc theo vai trò/trạng thái, cấp nhiều vai trò và khóa/mở tài khoản.
Không chọn vai trò sẽ chuyển tài khoản thành khách hàng.

API dành riêng cho `ADMIN`:

- `GET /api/v1/admin/users?page=0&size=10&search=&role=&status=`
- `POST /api/v1/admin/users`
- `PATCH /api/v1/admin/users/{id}/roles` với `{"roles":["RECEPTIONIST","HOUSEKEEPER"]}`
- `PATCH /api/v1/admin/users/{id}/status` với `{"status":"LOCKED"}`

Vai trò gồm `ADMIN`, `MANAGER`, `RECEPTIONIST`, `HOUSEKEEPER`. Bộ lọc `GUEST`
chọn khách hàng không có vai trò nhân viên. Trạng thái gồm `ACTIVE`, `LOCKED`,
`INACTIVE`. Quyền Admin không tự kế thừa các thao tác riêng của lễ tân/buồng phòng;
có thể cấp thêm vai trò cần thiết cho cùng một tài khoản.

Thay đổi vai trò hoặc trạng thái sẽ thu hồi access token và refresh token;
người dùng cần đăng nhập lại. JWT được đối chiếu với trạng thái và vai trò hiện
tại trong database ở mỗi request. Admin không thể tự khóa/gỡ vai trò Admin và
hệ thống bảo vệ Admin hoạt động cuối cùng khi có thao tác đồng thời.
Lịch sử được lưu trong `account_access_audits`.

## Tài liệu trong source

- [Backend](hotel-booking/README.md)
- [Frontend](client/README.md)
- [Build MinIO](deploy/minio/README.md)
- [Chính sách RAG](hotel-booking/src/main/resources/knowledge-base)
- [Triển khai VPS](deploy/vps/README.md)
