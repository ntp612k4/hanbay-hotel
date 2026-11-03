# Đặc tả API theo hệ thống hiện tại

URL cơ sở: `/api/v1`. Phản hồi thành công luôn có cấu trúc:

```json
{"success":true,"message":"...","data":{},"timestamp":"..."}
```

Phản hồi lỗi có cấu trúc:

```json
{
  "code": "VALIDATION_FAILED",
  "message": "Dữ liệu yêu cầu không hợp lệ",
  "path": "/api/v1/resource",
  "timestamp": "2026-08-10T10:00:00Z",
  "errors": {
    "field": "Giá trị không hợp lệ"
  }
}
```

## Phân loại quyền truy cập

Mỗi API phải được mô tả bằng đúng một trong ba mức bảo mật:

- `PUBLIC`: không yêu cầu access token. API có thể sử dụng cơ chế xác thực tích hợp riêng, ví dụ API key của SePay.
- `AUTHENTICATED`: yêu cầu access token hợp lệ, nhưng không yêu cầu vai trò nhân viên cụ thể.
- `ROLE_RESTRICTED`: yêu cầu access token hợp lệ và người dùng phải có ít nhất một vai trò trong cột `Vai trò được phép`.

API được bảo vệ (`AUTHENTICATED` hoặc `ROLE_RESTRICTED`) sử dụng header:

```http
Authorization: Bearer <access_token>
```

Lỗi xác thực và phân quyền:

| HTTP | Mã lỗi | Trường hợp |
| --- | --- | --- |
| `401 Unauthorized` | `AUTH_UNAUTHORIZED` | Chưa xác thực; access token bị thiếu, không hợp lệ hoặc đã hết hạn |
| `403 Forbidden` | `AUTH_FORBIDDEN` | Xác thực thành công nhưng người dùng không có vai trò được phép |

## Endpoint hiện có

| Phương thức | Đường dẫn | Mức bảo mật | Vai trò được phép | Mô tả |
| --- | --- | --- | --- | --- |
| POST | `/api/v1/auth/register` | `PUBLIC` | — | Đăng ký tài khoản khách |
| POST | `/api/v1/auth/login` | `PUBLIC` | — | Đăng nhập và nhận access token/refresh token |
| POST | `/api/v1/auth/refresh-token` | `PUBLIC` | — | Cấp lại cặp token |
| POST | `/api/v1/auth/forgot-password` | `PUBLIC` | — | Tiếp nhận yêu cầu đặt lại mật khẩu |
| POST | `/api/v1/auth/reset-password/verify` | `PUBLIC` | — | Kiểm tra mã đặt lại mật khẩu còn hiệu lực |
| POST | `/api/v1/auth/reset-password` | `PUBLIC` | — | Đặt lại mật khẩu bằng mã hợp lệ |
| GET | `/api/v1/room-types` | `PUBLIC` | — | Danh sách hạng phòng |
| GET | `/api/v1/room-types/available` | `PUBLIC` | — | Hạng phòng còn trống và chi tiết giá động |
| GET | `/api/v1/room-types/{id}` | `PUBLIC` | — | Chi tiết hạng phòng |
| POST | `/api/v1/pricing/stay-price-calculations` | `PUBLIC` | — | Tính giá lưu trú theo từng đêm cho luồng đặt phòng của khách |
| GET | `/api/v1/add-ons` | `PUBLIC` | — | Danh sách dịch vụ đang hoạt động |
| GET | `/api/v1/management/add-ons` | `ROLE_RESTRICTED` | `MANAGER`, `ADMIN` | Tra cứu toàn bộ dịch vụ bổ sung |
| POST | `/api/v1/management/add-ons` | `ROLE_RESTRICTED` | `MANAGER`, `ADMIN` | Tạo dịch vụ bổ sung |
| GET | `/api/v1/management/add-ons/{id}` | `ROLE_RESTRICTED` | `MANAGER`, `ADMIN` | Xem chi tiết dịch vụ bổ sung |
| PUT | `/api/v1/management/add-ons/{id}` | `ROLE_RESTRICTED` | `MANAGER`, `ADMIN` | Cập nhật thông tin và giá dịch vụ |
| PATCH | `/api/v1/management/add-ons/{id}/status` | `ROLE_RESTRICTED` | `MANAGER`, `ADMIN` | Kích hoạt hoặc ngừng kích hoạt dịch vụ |
| GET | `/api/v1/management/price-rules` | `ROLE_RESTRICTED` | `MANAGER`, `ADMIN` | Tra cứu toàn bộ quy tắc giá |
| POST | `/api/v1/management/price-rules` | `ROLE_RESTRICTED` | `MANAGER`, `ADMIN` | Tạo quy tắc giá |
| GET | `/api/v1/management/price-rules/{id}` | `ROLE_RESTRICTED` | `MANAGER`, `ADMIN` | Xem chi tiết quy tắc giá |
| PUT | `/api/v1/management/price-rules/{id}` | `ROLE_RESTRICTED` | `MANAGER`, `ADMIN` | Cập nhật quy tắc giá |
| PATCH | `/api/v1/management/price-rules/{id}/status` | `ROLE_RESTRICTED` | `MANAGER`, `ADMIN` | Kích hoạt hoặc ngừng kích hoạt quy tắc giá |
| POST | `/api/v1/bookings` | `PUBLIC` | — | Tạo đặt phòng chờ thanh toán |
| PATCH | `/api/v1/bookings/{bookingId}` | `PUBLIC` | — | Cập nhật đặt phòng chờ thanh toán còn hạn |
| GET | `/api/v1/bookings/my-bookings` | `AUTHENTICATED` | Tất cả tài khoản | Đặt phòng thuộc tài khoản đang đăng nhập |
| GET | `/api/v1/bookings/{bookingCode}` | `AUTHENTICATED` | Tất cả tài khoản | Chi tiết đặt phòng thuộc tài khoản đang đăng nhập; không tồn tại/không sở hữu cùng trả `404`. UI chỉ công bố route này cho Guest không có staff role |
| GET | `/api/v1/users/me` | `AUTHENTICATED` | Tất cả tài khoản | Xem hồ sơ của tài khoản đang đăng nhập |
| PATCH | `/api/v1/users/me` | `AUTHENTICATED` | Tất cả tài khoản | Cập nhật hồ sơ của tài khoản đang đăng nhập |
| POST | `/api/v1/payments/requests` | `PUBLIC_WITH_BOOKING_TOKEN` | — | Tạo yêu cầu thanh toán cho đặt phòng PENDING |
| GET | `/api/v1/payments/{paymentId}/status` | `PUBLIC_WITH_BOOKING_TOKEN` | — | Theo dõi kết quả thanh toán và xác nhận đặt phòng |
| POST | `/api/v1/payments/sepay/webhook` | `PUBLIC` (API key SePay) | — | Nhận webhook giao dịch từ SePay |
| GET | `/api/v1/payment-monitoring/summary` | `ROLE_RESTRICTED` | `MANAGER`, `ADMIN` | Tổng quan thanh toán và webhook |
| GET | `/api/v1/payment-monitoring/payments` | `ROLE_RESTRICTED` | `MANAGER`, `ADMIN` | Tra cứu thanh toán |
| GET | `/api/v1/payment-monitoring/webhook-events` | `ROLE_RESTRICTED` | `MANAGER`, `ADMIN` | Tra cứu sự kiện webhook, không gồm dữ liệu thô |
| GET | `/api/v1/payment-monitoring/webhook-events/{eventId}` | `ROLE_RESTRICTED` | `ADMIN` | Xem sự kiện webhook và dữ liệu thô |
| GET | `/api/v1/reports/dashboard` | `ROLE_RESTRICTED` | `MANAGER`, `ADMIN` | Báo cáo doanh thu, occupancy và booking theo khoảng ngày |

Các API ở những phần dưới nhưng không có trong bảng này là API dự kiến, chưa được backend công bố. Mức bảo mật và vai trò được phép của chúng là `CẦN XÁC NHẬN NGHIỆP VỤ` trừ khi phần tương ứng ghi rõ khác đi.

Giới hạn khôi phục payment của guest ẩn danh: booking access token chỉ được client lưu trong `sessionStorage` và không xuất hiện trong URL. Backend không cung cấp API tìm lại token; đổi trình duyệt, tab không chia sẻ session hoặc kết thúc phiên sẽ không thể mở lại kết quả bằng danh tính ẩn danh. Chủ booking có tài khoản có thể dùng JWT và đi từ kết quả thanh toán tới `/account/bookings/{bookingCode}`.

## API contract HAN-237: Manager dashboard report

> Trạng thái: **đã được công bố trong production code**.
> OpenAPI đầy đủ: [`openapi/reporting/manager-dashboard.yaml`](./openapi/reporting/manager-dashboard.yaml).

```http
GET /api/v1/reports/dashboard?from=2026-08-01&to=2026-09-01&currency=VND
Authorization: Bearer <access_token>
```

- Chỉ `MANAGER` và `ADMIN` được phép truy cập.
- `from` và `to` bắt buộc ở định dạng ISO `yyyy-MM-dd`.
- Khoảng thời gian là `[from,to)`, `from < to` và tối đa 366 ngày.
- `currency` tùy chọn, mặc định `VND`, gồm đúng ba chữ cái viết hoa.
- Response gồm summary và daily trend cho revenue, occupancy và booking.
- Ngày không có dữ liệu vẫn xuất hiện trong daily trend với giá trị `0`.
- Công thức và giới hạn dữ liệu được mô tả tại [`18-manager-dashboard-metrics.md`](./18-manager-dashboard-metrics.md).

## Available room response data

Mỗi hạng phòng gồm `basePrice`, `availableRooms`, `totalNights`, `roomSubtotal`, `roomDiscount`, `roomTotal` và `nightlyPrices[{stayDate,price,adjustments}]`. Mỗi phần tử `adjustments` là quy tắc giá thực tế đã áp dụng, gồm `name`, `ruleType`, `adjustmentType`, `adjustmentValue` và `amount`.

## API contract HAN-43: Tính giá lưu trú theo từng đêm

> Trạng thái: **đã được công bố trong production code**.
> OpenAPI đầy đủ: [`openapi/pricing/calculate-stay-price.yaml`](./openapi/pricing/calculate-stay-price.yaml).

```http
POST /api/v1/pricing/stay-price-calculations
Content-Type: application/json
```

Endpoint dùng `POST` vì đầu vào là một DTO tính toán và kết quả là representation của một lần tính giá; đường dẫn vẫn dùng danh từ số nhiều theo REST convention hiện tại.

### Authorization

Endpoint là `PUBLIC` để khách chưa đăng nhập có thể xem giá chính xác trước khi tiếp tục
luồng tạo booking. Backend chỉ đọc hạng phòng và quy tắc giá đang hoạt động; không thay
đổi dữ liệu hay giữ inventory tại bước này.

### Request DTO

```json
{"roomTypeId":"10000000-0000-0000-0000-000000000001","checkInDate":"2026-08-20","checkOutDate":"2026-08-23"}
```

Contract dùng `roomTypeId`, không dùng `roomId`, vì `bookings` và `price_rules` đều tham chiếu `room_type_id`; phòng vật lý chỉ được gán cho booking sau đó.

| Trường | Quy tắc |
| --- | --- |
| `roomTypeId` | Bắt buộc, UUID hợp lệ, hạng phòng phải tồn tại và thuộc catalog đang hoạt động. |
| `checkInDate` | Bắt buộc, ISO `yyyy-MM-dd`, không ở quá khứ theo rule tìm phòng hiện tại. |
| `checkOutDate` | Bắt buộc, ISO `yyyy-MM-dd`, phải lớn hơn `checkInDate`. |

Khoảng lưu trú là `[checkInDate, checkOutDate)`: check-in được tính, check-out không được tính. Số đêm bằng số ngày giữa hai mốc.

### Response DTO

```json
{
  "success": true,
  "message": "Tính giá lưu trú thành công",
  "data": {
    "roomType": {"id":"10000000-0000-0000-0000-000000000001","code":"DLX","name":"Deluxe"},
    "checkInDate": "2026-08-20",
    "checkOutDate": "2026-08-23",
    "numberOfNights": 3,
    "nightlyPrices": [
      {"date":"2026-08-20","finalPrice":1200000},
      {"date":"2026-08-21","finalPrice":1350000},
      {"date":"2026-08-22","finalPrice":1500000}
    ],
    "totalPrice": 4050000
  },
  "timestamp": "2026-08-18T08:00:00Z"
}
```

`nightlyPrices` tăng dần và chứa đúng một phần tử cho mỗi stay date. `numberOfNights` bằng số phần tử; `totalPrice` bằng tổng chính xác mọi `finalPrice`. Số tiền là số nguyên không âm, đơn vị VND, phù hợp `BIGINT`/`long`. Contract tối thiểu không expose Price Rule để tránh cam kết hành vi chưa xác nhận.

### Error contract

Mọi lỗi dùng `ApiErrorResponse {code,message,path,timestamp,errors}` hiện tại.

| HTTP | Mã lỗi hiện có | Trường hợp |
| --- | --- | --- |
| `400` | `VALIDATION_FAILED` | Thiếu field bắt buộc trong request DTO. |
| `400` | `MALFORMED_REQUEST` | JSON không đọc được, UUID sai định dạng hoặc ngày không theo ISO `yyyy-MM-dd`. |
| `400` | `PRICING_VALIDATION_FAILED` | Check-in ở quá khứ, check-out không sau check-in hoặc cấu hình pricing vi phạm invariant hiện có. |
| `404` | `ROOM_001` | Không tìm thấy hạng phòng hoặc hạng phòng không active. |
| `500` | `INTERNAL_SERVER_ERROR` | Dữ liệu giá thiếu/không nhất quán ngoài invariant đã biết. |

`NEED BUSINESS CONFIRMATION`: “Pricing data not available” cần domain error riêng (và HTTP status nào) hay tiếp tục che thành `INTERNAL_SERVER_ERROR`. `room_types.base_price` hiện `NOT NULL` và không âm nên không thể thiếu qua schema hợp lệ.

### Pricing rule boundary

Schema cho phép nhiều Price Rule trên một booking night qua `booking_night_price_rules`. Service hiện tại lấy mọi active rule, tách promotion/surcharge và sắp theo priority, nhưng tài liệu nghiệp vụ chưa quy định đầy đủ ý nghĩa priority hoặc cách giải quyết rule trùng.

`NEED BUSINESS CONFIRMATION: Multiple pricing rules applied to the same night.`

Phải xác nhận stacking, chọn priority cao nhất, rule có lợi nhất hoặc chính sách khác trước khi coi kết quả là contract nghiệp vụ ổn định. HAN-43 không thay đổi hoặc hợp thức hóa thuật toán hiện hữu.

```http
POST /auth/login
```

## Refresh Token

```http
POST /auth/refresh-token
```

## Quên mật khẩu

```http
POST /auth/forgot-password
```

Mục đích:

- API công khai được trang đăng nhập sử dụng khi người dùng quên mật khẩu.
- Luôn trả cùng một phản hồi thành công khi email đúng định dạng, bất kể tài khoản có tồn tại hay không.
- Nếu email thuộc tài khoản đang hoạt động, tạo mã đặt lại mật khẩu và gửi hướng dẫn qua email.
- Không tạo mã và không tiết lộ email chưa đăng ký nếu không tìm thấy tài khoản phù hợp.

Yêu cầu:

```json
{
  "email": "guest@example.com"
}
```

Kiểm tra dữ liệu:

| Trường | Quy tắc |
| --- | --- |
| `email` | Bắt buộc, đúng định dạng email, tối đa 255 ký tự; được loại bỏ khoảng trắng hai đầu và chuyển thành chữ thường. |

Response `200 OK`:

```json
{
  "success": true,
  "message": "Nếu email tồn tại, hướng dẫn đặt lại mật khẩu sẽ được gửi",
  "data": {
    "status": "RESET_INSTRUCTIONS_ACCEPTED"
  },
  "timestamp": "2026-08-10T06:00:00Z"
}
```

Lỗi:

| HTTP | Mã lỗi | Trường hợp |
| --- | --- | --- |
| 400 | `VALIDATION_FAILED` | Email bị thiếu, sai định dạng hoặc dài hơn 255 ký tự. |
| 429 | `AUTH_RATE_LIMITED` | Địa chỉ IP gửi quá nhiều yêu cầu trong khoảng thời gian giới hạn. |

Luồng gửi email:

1. Sinh mã ngẫu nhiên an toàn với ít nhất 32 byte và mã hóa Base64 URL không có padding.
2. Chỉ lưu `SHA-256(token)` trong `password_reset_tokens.token_hash`; không lưu hoặc ghi log mã gốc.
3. Tạo liên kết giao diện dạng `/reset-password?token=<raw-token>`.
4. Gửi email bất đồng bộ bằng executor riêng của Spring.
5. Nếu SMTP thất bại, ghi log vận hành và thu hồi ngay mã vừa phát hành.

Vòng đời mã đặt lại mật khẩu:

- Bảng `password_reset_tokens` liên kết với `users(id)` bằng `ON DELETE CASCADE`.
- Mặc định hết hạn sau 15 phút và chỉ được sử dụng một lần.
- Trước khi cấp mã mới, hệ thống thu hồi mọi mã chưa dùng của cùng người dùng.
- Khi đặt lại mật khẩu, bản ghi được khóa bi quan để ngăn hai yêu cầu đồng thời cùng sử dụng một mã.
- Scheduler hằng ngày xóa bản ghi đã hết hạn quá thời gian lưu trữ cấu hình.

## Đặt lại mật khẩu

```http
POST /auth/reset-password/verify
```

```json
{"token":"base64url-token"}
```

Response `200 OK` khi token hợp lệ:

```json
{
  "success": true,
  "message": "Token đặt lại mật khẩu hợp lệ",
  "data": {
    "status": "RESET_TOKEN_VALID",
    "expiresAt": "2026-08-10T06:15:00Z"
  },
  "timestamp": "2026-08-10T06:05:00Z"
}
```

API trả lỗi `AUTH_RESET_TOKEN_INVALID`, `AUTH_RESET_TOKEN_EXPIRED` hoặc
`AUTH_RESET_TOKEN_USED` nếu mã không còn sử dụng được. Mã gốc nằm trong request body,
chỉ được hash trước khi tra cứu và không xuất hiện trong query string.

```http
POST /auth/reset-password
```

Mục đích:

- API công khai nhận mã hợp lệ và cập nhật mật khẩu cho tài khoản đang hoạt động.
- Không yêu cầu access token; mã đặt lại mật khẩu hợp lệ là bằng chứng xác thực của luồng này.

Yêu cầu:

```json
{
  "token": "base64url-token",
  "newPassword": "Password123",
  "confirmPassword": "Password123"
}
```

Kiểm tra dữ liệu:

| Trường | Quy tắc |
| --- | --- |
| `token` | Bắt buộc, từ 32 đến 512 ký tự. |
| `newPassword` | Bắt buộc, từ 8 đến 72 ký tự và được mã hóa bằng `PasswordEncoder`. |
| `confirmPassword` | Bắt buộc, từ 8 đến 72 ký tự và phải trùng với `newPassword`. |

Response `200 OK`:

```json
{
  "success": true,
  "message": "Đặt lại mật khẩu thành công",
  "data": {
    "status": "PASSWORD_RESET"
  },
  "timestamp": "2026-08-10T06:05:00Z"
}
```

Lỗi:

| HTTP | Mã lỗi | Trường hợp |
| --- | --- | --- |
| 400 | `VALIDATION_FAILED` | Cấu trúc hoặc dữ liệu gửi lên không hợp lệ. |
| 400 | `AUTH_RESET_TOKEN_INVALID` | Mã không tồn tại hoặc thuộc tài khoản không hoạt động. |
| 400 | `AUTH_RESET_TOKEN_EXPIRED` | Mã đã hết hạn. |
| 400 | `AUTH_RESET_TOKEN_USED` | Mã đã được sử dụng hoặc bị thu hồi. |
| 400 | `PASSWORD_MISMATCH` | Mật khẩu mới và mật khẩu xác nhận không trùng nhau. |
| 429 | `AUTH_RATE_LIMITED` | Địa chỉ IP thử đặt lại mật khẩu quá nhiều lần trong khoảng giới hạn. |

Xử lý phiên sau khi đặt lại mật khẩu:

- Cập nhật `users.password_hash` và đánh dấu `used_at` trong cùng transaction.
- Thu hồi toàn bộ refresh token đang hoạt động.
- Tăng phiên bản access token của người dùng trong Redis; mọi JWT mang phiên bản cũ bị từ chối ngay.
- Không trả access token hoặc refresh token; giao diện chuyển người dùng về trang đăng nhập.

## Logout

```http
POST /auth/logout
```

Authorization header:

```http
Authorization: Bearer <access_token>
```

---

# 3. API thông tin cá nhân

Các API thông tin cá nhân yêu cầu mã truy cập hợp lệ. Backend xác định người dùng hiện tại từ ngữ cảnh xác thực; client không được gửi mã người dùng để lựa chọn hồ sơ.

API chỉ phục vụ tài khoản đã đăng nhập có trạng thái `ACTIVE`. Người dùng chỉ được xem hoặc cập nhật hồ sơ của chính mình. Thay đổi email, mật khẩu, vai trò và trạng thái tài khoản không thuộc phạm vi API này.

## Lấy thông tin cá nhân hiện tại

```http
GET /api/v1/users/me
Authorization: Bearer <access_token>
```

Phản hồi thành công — `200 OK`:

```json
{
  "success": true,
  "message": "Lấy thông tin cá nhân thành công",
  "data": {
    "id": "96a8f221-c24d-4a52-a91d-b110fbde8c0e",
    "email": "guest@example.com",
    "fullName": "Nguyen Van A",
    "phone": "0900000000",
    "status": "ACTIVE",
    "roles": [],
    "createdAt": "2026-08-01T08:30:00Z",
    "updatedAt": "2026-08-10T10:00:00Z"
  }
}
```

## Cập nhật thông tin cá nhân hiện tại

```http
PATCH /api/v1/users/me
Authorization: Bearer <access_token>
Content-Type: application/json
```

Nội dung yêu cầu:

```json
{
  "fullName": "Nguyen Van B",
  "phone": "+84912345678"
}
```

Yêu cầu sử dụng cơ chế cập nhật từng phần:

- Phải cung cấp ít nhất một trong hai trường `fullName` hoặc `phone`.
- Trường bị bỏ qua sẽ giữ nguyên giá trị hiện tại.
- Trường được gửi với giá trị `null` hoặc chuỗi rỗng là không hợp lệ.
- Khoảng trắng ở đầu và cuối được loại bỏ trước khi kiểm tra và lưu dữ liệu.
- Các trường không hỗ trợ hoặc chỉ đọc như `id`, `email`, `status`, `roles` phải bị từ chối.

Quy tắc kiểm tra:

| Trường | Bắt buộc | Quy tắc |
|---|---|---|
| `fullName` | Ít nhất một trường có thể sửa | Từ 2 đến 100 ký tự sau khi loại bỏ khoảng trắng; không được rỗng |
| `phone` | Ít nhất một trường có thể sửa | Từ 8 đến 15 chữ số, có thể bắt đầu bằng `+`; mẫu `^\+?[0-9]{8,15}$` |

Phản hồi thành công — `200 OK`:

```json
{
  "success": true,
  "message": "Cập nhật thông tin cá nhân thành công",
  "data": {
    "id": "96a8f221-c24d-4a52-a91d-b110fbde8c0e",
    "email": "guest@example.com",
    "fullName": "Nguyen Van B",
    "phone": "+84912345678",
    "status": "ACTIVE",
    "roles": [],
    "createdAt": "2026-08-01T08:30:00Z",
    "updatedAt": "2026-08-10T10:15:00Z"
  }
}
```

Lỗi dữ liệu — `400 Bad Request`:

```json
{
  "code": "VALIDATION_FAILED",
  "message": "Dữ liệu yêu cầu không hợp lệ",
  "path": "/api/v1/users/me",
  "timestamp": "2026-08-10T10:15:00Z",
  "errors": {
    "phone": "Số điện thoại phải có từ 8 đến 15 chữ số và có thể bắt đầu bằng dấu +"
  }
}
```

Các phản hồi có thể xảy ra:

| Trạng thái HTTP | Mã lỗi | Trường hợp |
|---|---|---|
| `200 OK` | — | Lấy hoặc cập nhật thông tin cá nhân thành công |
| `400 Bad Request` | `VALIDATION_FAILED` | Không có trường có thể sửa, chứa trường không hỗ trợ hoặc dữ liệu không hợp lệ |
| `400 Bad Request` | `MALFORMED_REQUEST` | Thiếu body, body là `null` hoặc JSON sai định dạng |
| `401 Unauthorized` | `AUTH_UNAUTHORIZED` | Thiếu mã truy cập, mã không hợp lệ hoặc đã hết hạn |
| `403 Forbidden` | `USER_002` | Tài khoản đã xác thực không còn hoạt động |
| `404 Not Found` | `USER_001` | Người dùng đã xác thực không còn tồn tại |
| `409 Conflict` | `PHONE_ALREADY_EXISTS` | Số điện thoại mới đã được tài khoản khác sử dụng |
| `500 Internal Server Error` | `INTERNAL_SERVER_ERROR` | Phát sinh lỗi hệ thống ngoài dự kiến |

Cập nhật hồ sơ không thay đổi thông tin khách đã lưu trong các đặt phòng hiện có. Thông tin khách trong đặt phòng là dữ liệu chụp tại thời điểm tạo đặt phòng.

---

# 4. API tìm kiếm phòng

## Tìm kiếm hạng phòng còn trống

```http
GET /room-types/available?checkIn=2026-07-12&checkOut=2026-07-15&adults=2&children=0&page=0&size=6
```

Mục đích:

- Tìm hạng phòng còn trống theo khoảng ngày lưu trú.
- Tính giá theo các quy tắc giá linh hoạt đang có hiệu lực.
- Loại các hạng phòng đã hết phòng hoặc đang được giữ chỗ.
- Lọc sức chứa và số phòng còn trống trước khi phân trang để kết quả không bị thiếu phần tử.
- Trả về `PageResponse<AvailableRoomTypeResponse>`; `page` bắt đầu từ `0`, `size` mặc định là `6` và chỉ nhận từ `1` đến `100`.
- Chỉ tính giá cho các hạng phòng thuộc trang được yêu cầu.
- Có thể truyền `roomTypeId` để kiểm tra lại một hạng phòng đã chọn mà không phụ thuộc vị trí phân trang.
- `checkOut` không được tính là một đêm lưu trú. Ví dụ: từ `2027-08-12` đến `2027-08-15` là 3 đêm.
- `totalNights` là số đêm tính tiền do backend trả về và frontend phải sử dụng để hiển thị.
- `basePrice` là giá cơ bản của một đêm.
- `roomSubtotal` là tổng giá phòng sau phụ thu và trước khuyến mãi.
- `roomDiscount` là tổng số tiền được giảm.
- `roomTotal` là tổng tiền phòng cuối cùng sau khi áp dụng toàn bộ quy tắc giá.
- `nightlyPrices` chứa giá cuối cùng và danh sách `adjustments` thực tế của từng đêm.
- Chỉ trả về hạng phòng có `availableRooms > 0`.

Các công thức phải luôn thỏa mãn:

```text
roomTotal = tổng nightlyPrices[].price
roomTotal = roomSubtotal - roomDiscount
```

---

## Xem chi tiết hạng phòng

```http
GET /room-types/{id}
```

Mục đích:

- Trả về chi tiết hạng phòng cho HAN-22.
- Bao gồm tên, mô tả, giá cơ bản, sức chứa, thư viện ảnh và tiện nghi.
- Đây là API công khai, được trang chi tiết hạng phòng sử dụng trước khi đặt phòng.

Response `200 OK`:

```json
{
  "success": true,
  "data": {
    "id": "10000000-0000-0000-0000-000000000001",
    "name": "Phòng Suite Hướng Biển Cao Cấp",
    "description": "Trải nghiệm sự xa hoa...",
    "basePrice": 8500000.00,
    "maxGuests": 4,
    "capacityAdults": 2,
    "capacityChildren": 2,
    "images": ["https://..."],
    "amenities": [
      {
        "id": "uuid",
        "name": "Wi-Fi tốc độ cao",
        "icon": "wifi"
      }
    ]
  }
}
```

Lỗi:

- `400 VALIDATION_FAILED` khi `id` không đúng định dạng UUID.
- `404 RESOURCE_NOT_FOUND` khi loại phòng không tồn tại hoặc không còn kinh doanh.

Local smoke test file:

```text
hotel-booking/http/HAN-22-room-detail.http
```

---

# 5. Booking APIs

## Create Pending Booking

```http
POST /bookings
```

Tạo booking public với trạng thái `PENDING`. Hệ thống lock RoomType, kiểm tra lại availability trong transaction và giữ inventory theo RoomType trong 15 phút. Không gán phòng vật lý tại bước này.

Request:

```json
{
  "roomTypeId": "uuid",
  "checkInDate": "2026-08-08",
  "checkOutDate": "2026-08-10",
  "adults": 2,
  "children": 2,
  "guestInfo": {
    "fullName": "Nguyễn Văn A",
    "email": "guest@example.com",
    "phone": "0901234567",
    "identityNumber": "012345678901"
  },
  "addOns": [
    {"addOnServiceId": "uuid", "quantity": 2}
  ]
}
```

Backend kiểm tra riêng adult capacity, child capacity và max guests. Không yêu cầu tuổi từng trẻ; guest chịu trách nhiệm áp dụng quy ước `age <= 12` là child. Tiền phòng tính theo nightly pricing, không nhân theo số trẻ. Add-on được lấy lại từ DB, phải active, quantity 1–20, và snapshot giá vào booking.

Response data gồm `bookingId`, `bookingCode`, `status=PENDING`, `totalAmount`, `expiredAt`.

Các lỗi chính:

| HTTP | Trường hợp                                                                                   |
| ---- | -------------------------------------------------------------------------------------------- |
| 400  | Request không hợp lệ, ngày lưu trú hoặc sức chứa không hợp lệ, add-on không tồn tại/inactive |
| 404  | RoomType không tồn tại hoặc inactive                                                         |
| 409  | Không còn phòng trong khoảng ngày yêu cầu                                                    |

Frontend public `/bookings/new` gồm ba bước: chọn phòng, nhập thông tin khách và review trang thanh toán. Nút **Kiểm tra đơn hàng** gọi API này; chỉ khi nhận `201` mới chuyển sang trang thanh toán. Từ trang thanh toán, frontend gọi API tạo payment request ở phần Payment APIs. Mã giảm giá chưa thuộc phạm vi hiện tại.

## Get Booking Detail

Mức bảo mật: `CẦN XÁC NHẬN NGHIỆP VỤ`.

```http
GET /bookings/{id}
```

## View Booking History

Contract tại [`openapi/booking/view-booking-history.yaml`](openapi/booking/view-booking-history.yaml) định nghĩa:

- `GET /api/v1/bookings/my-bookings`: dùng access token và lấy chủ tài khoản từ `SecurityContext`.
- Query `page` bắt đầu từ `0`, `size` từ `1` đến `100`; mặc định lần lượt là `0` và `10`.
- Query `status` không bắt buộc, nhận một trong các giá trị `PENDING`, `CONFIRMED`, `CANCELLED`, `CHECKED_IN`, `CHECKED_OUT`, `EXPIRED`.
- Kết quả được sắp xếp theo thời điểm tạo mới nhất và trả theo cấu trúc `PageResponse`.
- `paymentStatus` là trạng thái của payment mới nhất; nhận `null` nếu booking chưa phát sinh payment.
- `paymentId` là payment mới nhất, dùng cho liên kết **Tiếp tục thanh toán** khi booking và payment còn `PENDING`.

Trong phạm vi MVP, người dùng chưa đăng nhập không được xem lịch sử đặt phòng. Hệ thống không hỗ trợ OTP hoặc tra cứu trực tiếp bằng email/số điện thoại. Client không truyền `userId` hoặc `guestId`; response chỉ chứa booking summary và payment status, không lộ payload webhook hay thông tin giao dịch nội bộ.

Frontend hiển thị lịch sử đặt phòng trong mục “Lịch sử đặt phòng” của trang `/account/profile` sau lớp bảo vệ đăng nhập. Giao diện hỗ trợ lọc trạng thái, phân trang phía server và xem chi tiết booking mà không cần một trang lịch sử riêng. Có thể kiểm tra API thủ công bằng [`../hotel-booking/http/booking-history.http`](../hotel-booking/http/booking-history.http).

Các lỗi chính:

| HTTP | Trường hợp                                                                      |
| ---- | ------------------------------------------------------------------------------- |
| 400  | `page < 0`, `size` ngoài khoảng 1–100 hoặc `status` không thuộc enum được hỗ trợ |
| 401  | Thiếu access token, token sai hoặc hết hạn                                       |

## Cancel Booking

Mức bảo mật: `AUTHENTICATED`.

Khách được hủy đặt phòng của chính mình theo chính sách: trước giờ nhận phòng từ 48 giờ được hoàn 100%; từ 24 đến dưới 48 giờ được hoàn 50%; dưới 24 giờ không được hủy. Booking đang chờ thanh toán có thể hủy mà không phát sinh hoàn tiền. Với booking đã thanh toán, backend tạo yêu cầu hoàn tiền đang chờ; chỉ Manager xác nhận sau khi thực hiện chuyển khoản và không được thay đổi số tiền do hệ thống tính.

Contract chi tiết tại [`openapi/booking/cancel-booking.yaml`](openapi/booking/cancel-booking.yaml).

```http
PATCH /bookings/{id}/cancel
```

---

# 6. Payment APIs

## Create Payment Request

Mức bảo mật: `CẦN XÁC NHẬN NGHIỆP VỤ`.

```http
POST /payments/requests
```

Public endpoint cho khách bắt đầu thanh toán booking đang chờ. API này dùng cho cả khách vãng lai đã nhập thông tin liên hệ trong booking form; không bắt buộc JWT.

Request:

```json
{
  "bookingId": "uuid"
}
```

Business rules:

- Chỉ tạo payment request cho booking tồn tại, status `PENDING` và `expiredAt > now`.
- Số tiền thanh toán lấy từ `bookings.total_amount`; client không được truyền `amount`.
- Gửi email xác nhận không nằm trong phạm vi API tạo payment request.
- API chỉ tạo payment attempt với status `PENDING`; không xác nhận thanh toán và không chuyển booking sang `CONFIRMED`.
- Nếu booking đã có payment `PENDING` còn hiệu lực, API trả lại payment đó thay vì tạo bản ghi trùng.
- Nếu booking đã có payment `SUCCESS`, booking không còn payable.
- Payment success/failure được xử lý bởi webhook hoặc bước đối soát riêng.

Response `201 Created`:

```json
{
  "success": true,
  "message": "Tạo yêu cầu thanh toán thành công",
  "data": {
    "paymentId": "uuid",
    "bookingId": "uuid",
    "bookingCode": "BKG-000001",
    "transactionRef": "PAY-20260810-000001",
    "amount": 2450000,
    "currency": "VND",
    "status": "PENDING",
    "paymentUrl": "https://qr.sepay.vn/img?acc=0000000000&bank=TESTBANK&amount=2450000&des=PAY-20260810-000001&template=compact&download=false&showinfo=true",
    "expiredAt": "2026-08-10T08:15:00Z",
    "bankCode": "TESTBANK",
    "accountNumber": "0000000000",
    "accountName": "HANBAY HOTEL DEV",
    "transferContent": "PAY-20260810-000001"
  },
  "timestamp": "2026-08-10T08:00:00Z"
}
```

Các lỗi chính:

| HTTP | Code | Trường hợp |
| --- | --- | --- |
| 400 | `VALIDATION_FAILED` | `bookingId` thiếu hoặc không đúng UUID |
| 404 | `PAYMENT_BOOKING_NOT_FOUND` | Không tìm thấy booking |
| 409 | `PAYMENT_BOOKING_NOT_PAYABLE` | Booking không còn `PENDING`, đã hết hạn hoặc tổng tiền không hợp lệ |
| 409 | `PAYMENT_ALREADY_COMPLETED` | Booking đã có payment `SUCCESS` |
| 500 | `INTERNAL_SERVER_ERROR` | Lỗi không mong muốn hoặc lỗi tạo payment URL |

Contract chi tiết nằm tại [`openapi/payment/create-payment-request.yaml`](openapi/payment/create-payment-request.yaml).

## Payment Status

Mức bảo mật: chủ booking đã xác thực bằng JWT **hoặc** guest cung cấp booking access token.

```http
GET /api/v1/payments/{paymentId}/status
Authorization: Bearer <access-token>
```

Guest không đăng nhập sử dụng header sau thay cho `Authorization`:

```http
X-Booking-Access-Token: <booking-access-token>
```

API này tái sử dụng endpoint tra cứu trạng thái thanh toán hiện có. Không truyền token qua URL.
Backend chỉ trả dữ liệu khi người gọi là chủ booking hoặc có booking access token hợp lệ. Trường
hợp payment không tồn tại và trường hợp không có quyền đều trả `404` để hạn chế dò tìm payment.

Response thành công:

```json
{
  "success": true,
  "message": "Lấy trạng thái thanh toán thành công",
  "data": {
    "paymentId": "0c34c243-94b5-4ff5-a0d4-5cd6fa4e0a91",
    "bookingId": "8a1f6e4c-5b3a-4d9d-9e3d-4c8a3a9a1001",
    "bookingCode": "BK20260814000201",
    "transactionRef": "PAY-20260814070244-E02D78616064F1D9FA5F7DD96F876D",
    "paymentStatus": "SUCCESS",
    "bookingStatus": "CONFIRMED",
    "amount": 2450000,
    "paidAmount": 2450000,
    "currency": "VND",
    "refundStatus": null,
    "refundAmount": null,
    "refundReason": null,
    "paymentUrl": "https://qr.sepay.vn/img?...",
    "bankCode": "Vietcombank",
    "accountNumber": "0000000002",
    "accountName": "HANBAY HOTEL",
    "transferContent": "PAY20260814070244E02D78616064F1D9FA5F7DD96F876D",
    "expiredAt": "2026-08-14T07:17:00Z",
    "paidAt": "2026-08-14T07:05:30Z"
  },
  "timestamp": "2026-08-14T07:05:31Z"
}
```

Ý nghĩa trạng thái để frontend hiển thị kết quả:

| `paymentStatus` | Cách xử lý trên trang kết quả |
| --- | --- |
| `PENDING` | Hiển thị đang chờ thanh toán và tiếp tục polling có giới hạn |
| `SUCCESS` | Hiển thị thanh toán thành công; booking dự kiến ở trạng thái `CONFIRMED` |
| `FAILED` | Hiển thị thanh toán thất bại và cho phép quay lại luồng thanh toán |
| `CANCELLED` | Hiển thị yêu cầu thanh toán đã bị hủy hoặc được thay thế |
| `EXPIRED` | Hiển thị đã hết thời hạn thanh toán |
| `REFUNDED` | Hiển thị giao dịch đã được hoàn tiền toàn bộ |
| `PARTIALLY_REFUNDED` | Hiển thị giao dịch đã được hoàn tiền một phần |

Nếu payment có refund, frontend ưu tiên `refundStatus`, `refundAmount` và `refundReason` để hiển thị đúng các trường hợp thanh toán trễ, booking đã hủy/hết hạn và chuyển khoản dư. `paidAmount` chỉ là tổng transaction thành công của payment đang tra cứu, không phải tổng tiền trên toàn booking.

Khi payment còn `PENDING`, response trả lại QR và thông tin tài khoản để người dùng mở lại trang thanh toán từ lịch sử đặt phòng hoặc liên kết **Tiếp tục thanh toán** trong header mà không phụ thuộc state của form tạo booking.

Các lỗi chính:

| HTTP | Code | Trường hợp |
| --- | --- | --- |
| 400 | `VALIDATION_FAILED` | `paymentId` không đúng định dạng UUID |
| 404 | `PAYMENT_BOOKING_NOT_FOUND` | Payment không tồn tại, token không hợp lệ hoặc người gọi không có quyền |
| 429 | `RATE_LIMIT_EXCEEDED` | Vượt quá 120 request/phút/IP |
| 500 | `INTERNAL_SERVER_ERROR` | Lỗi hệ thống ngoài dự kiến |

Contract chi tiết nằm tại [`openapi/payment/payment-status.yaml`](openapi/payment/payment-status.yaml).

## Payment Webhook

```http
POST /api/v1/payments/sepay/webhook
Authorization: Apikey <SEPAY_WEBHOOK_API_KEY>
Content-Type: application/json
```

Purpose:

- Nhận payload giao dịch `in` hoặc `out` từ SePay và xác thực chính xác header `Authorization`.
- Dùng `id` của SePay tạo khóa chống trùng `sepay:event:<id>`.
- Lưu event mới với trạng thái `RECEIVED`; event trùng tuần tự được ACK mà không tạo thêm bản ghi.
- Trả `200` sau khi event được xử lý hoặc đã tồn tại. Client dùng API trạng thái payment để xác nhận kết quả nghiệp vụ.
- Với giao dịch tiền vào đúng tài khoản, hệ thống tìm mã payment trong `code`, `content` hoặc `description`, lưu transaction và cộng dồn số tiền đã nhận.
- Khi tổng tiền nhận đủ, payment chuyển sang `SUCCESS`. Booking `PENDING` còn hiệu lực tự động chuyển sang `CONFIRMED`.
- Nếu tổng tiền nhận lớn hơn số tiền payment, booking vẫn được xác nhận và hệ thống tạo refund `PENDING` đúng bằng phần chênh lệch. Sau khi hoàn phần dư, payment vẫn giữ `SUCCESS` vì khách sạn đã giữ đủ số tiền booking.
- Nếu booking đã hết hạn, không còn hợp lệ để xác nhận, hoặc payment trước đó đã `CANCELLED`/`FAILED`, booking không chuyển sang `CONFIRMED`; mỗi khoản tiền đến trễ, kể cả nhỏ hơn payment amount, đều được đối soát và tạo refund `PENDING` đúng bằng số tiền của giao dịch để Manager xử lý.
- Payload, response và error contract đầy đủ nằm tại
  [`openapi/payment/sepay-webhook.yaml`](openapi/payment/sepay-webhook.yaml).
- Các request kiểm thử thủ công nằm tại
  [`../hotel-booking/http/sepay-webhook.http`](../hotel-booking/http/sepay-webhook.http).

Ở môi trường dev/test, dùng **SePay Test mode** để mô phỏng giao dịch và kích hoạt
chính webhook trên. Hệ thống không cung cấp endpoint simulation riêng.

---

# 7. Front Desk APIs

Các API trong phần này là API dành cho nghiệp vụ lễ tân. Trừ endpoint “Khách trả phòng hôm nay” đã được xác định contract tại HAN-171, các endpoint còn lại vẫn là API dự kiến và cần xác nhận nghiệp vụ trước khi triển khai.

## Today Arrivals

```http
GET /api/v1/front-desk/arrivals/today?page=0&size=20
```

Mức bảo mật: `ROLE_RESTRICTED`. Các vai trò được phép là `RECEPTIONIST`,
`MANAGER` và `ADMIN`.

Contract chi tiết tại
[`openapi/front-desk/view-today-arrivals.yaml`](openapi/front-desk/view-today-arrivals.yaml)
định nghĩa:

- Ngày hiện tại được tính theo múi giờ khách sạn `Asia/Ho_Chi_Minh`.
- Chỉ trả booking có `checkInDate` bằng ngày hiện tại và trạng thái `CONFIRMED`.
  Các trạng thái `PENDING`, `CHECKED_IN`, `CHECKED_OUT`, `CANCELLED` và
  `EXPIRED` không xuất hiện trong danh sách.
- Query `page` bắt đầu từ `0`; `size` từ `1` đến `100`. Giá trị mặc định lần
  lượt là `0` và `20`.
- Kết quả được sắp xếp theo `createdAt` tăng dần, sau đó `bookingCode` tăng dần.
- `primaryGuest` là khách có `isPrimary=true`. Trường này có thể là `null` với
  dữ liệu cũ bị thiếu khách chính và không fallback sang tài khoản `bookedBy`.
- Danh sách rỗng trả `200 OK` với `content=[]`.
- Contract chưa hỗ trợ tìm kiếm, lọc trạng thái, thông tin phòng vật lý, trạng
  thái gán phòng hoặc thao tác check-in.
- Frontend sử dụng route `/front-desk/arrivals/today`. API danh sách không trả
  `identityNumber` để hạn chế lộ dữ liệu định danh không cần thiết.
- Request kiểm thử thủ công nằm tại
  [`../hotel-booking/http/today-arrivals.http`](../hotel-booking/http/today-arrivals.http).
- Ma trận QA và checklist thủ công nằm tại mục **TC-12 View Today Arrivals** của
  [`11-testing-strategy.md`](11-testing-strategy.md).

Các lỗi chính:

| HTTP | Trường hợp                                                                 |
| ---- | -------------------------------------------------------------------------- |
| 400  | `page < 0` hoặc `size` nằm ngoài khoảng 1–100                               |
| 401  | Thiếu access token, token sai hoặc hết hạn                                  |
| 403  | Tài khoản không có role `RECEPTIONIST`, `MANAGER` hoặc `ADMIN`              |
| 500  | Lỗi hệ thống ngoài dự kiến                                                  |

## Today Departures

```http
GET /api/v1/front-desk/departures/today?page=0&size=20
Authorization: Bearer <access_token>
```

Mục đích:

- Trả về danh sách booking có ngày trả phòng bằng ngày hiện tại của hệ thống để lễ tân chuẩn bị thao tác check-out.
- Kết quả được phân trang và sắp xếp ổn định theo ngày trả phòng, sau đó theo mã booking.
- Không trả trực tiếp entity hoặc dữ liệu nhạy cảm của payment/webhook.

Mức bảo mật: `ROLE_RESTRICTED`.

Vai trò được phép: `RECEPTIONIST`, `MANAGER`, `ADMIN`.

Query parameters:

| Tên | Kiểu | Bắt buộc | Mặc định | Quy tắc |
| --- | --- | --- | --- | --- |
| `page` | integer | Không | `0` | Bắt đầu từ `0` và không được âm |
| `size` | integer | Không | `20` | Từ `1` đến `100` |

Response `200 OK` sử dụng cấu trúc `ApiResponse<PageResponse<TodayDepartureResponse>>`:

```json
{
  "success": true,
  "message": "Lấy danh sách khách trả phòng hôm nay thành công",
  "data": {
    "content": [
      {
        "bookingId": "2f5b7f61-75b0-4ae5-bf02-df1304b25df1",
        "bookingCode": "BK202608190001",
        "guestName": "Nguyễn Văn An",
        "guestPhone": "0912345678",
        "roomTypeName": "Phòng Deluxe",
        "checkInDate": "2026-08-17",
        "checkOutDate": "2026-08-19",
        "adults": 2,
        "children": 1,
        "status": "CHECKED_IN",
        "totalAmount": 5635000,
        "paidAmount": 5000000,
        "outstandingAmount": 635000
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
  },
  "timestamp": "2026-08-19T08:00:00Z"
}
```

Ý nghĩa trường dữ liệu:

| Trường | Kiểu | Mô tả |
| --- | --- | --- |
| `bookingId` | UUID | Định danh nội bộ của booking |
| `bookingCode` | string | Mã booking hiển thị cho lễ tân |
| `guestName` | string | Họ tên khách chính |
| `guestPhone` | string | Số điện thoại khách chính |
| `roomTypeName` | string | Tên hạng phòng của booking |
| `checkInDate` | date | Ngày nhận phòng, định dạng `yyyy-MM-dd` |
| `checkOutDate` | date | Ngày trả phòng, luôn bằng ngày hiện tại của hệ thống |
| `adults` | integer | Số người lớn |
| `children` | integer | Số trẻ em |
| `status` | string | Trạng thái booking đủ điều kiện xuất hiện trong danh sách |
| `totalAmount` | number | Tổng tiền của booking |
| `paidAmount` | number | Tổng tiền đã thanh toán |
| `outstandingAmount` | number | Số tiền còn phải thu, không nhỏ hơn `0` |

Quy tắc nghiệp vụ của contract:

- Ngày “hôm nay” được backend xác định theo múi giờ cấu hình của hệ thống; client không truyền ngày để tránh sai lệch hoặc xem dữ liệu ngoài phạm vi.
- Booking hợp lệ trong danh sách có `checkOutDate` bằng hôm nay và trạng thái `CHECKED_IN`.
- `outstandingAmount = max(totalAmount - paidAmount, 0)`.
- Nếu không có khách trả phòng, API trả `200 OK` với `content` rỗng; đây không phải lỗi `404`.

Các lỗi chính:

| HTTP | Mã lỗi | Trường hợp |
| --- | --- | --- |
| `400 Bad Request` | `VALIDATION_FAILED` | `page < 0` hoặc `size` ngoài khoảng `1`–`100` |
| `401 Unauthorized` | `AUTH_UNAUTHORIZED` | Thiếu access token, token không hợp lệ hoặc đã hết hạn |
| `403 Forbidden` | `AUTH_FORBIDDEN` | Tài khoản không có vai trò `RECEPTIONIST`, `MANAGER` hoặc `ADMIN` |
| `500 Internal Server Error` | `INTERNAL_SERVER_ERROR` | Lỗi hệ thống ngoài dự kiến |

## Assign Room

```http
POST /bookings/{bookingId}/assign-room
```

Request:

```json
{
  "roomId": "uuid"
}
```

## Check-in

```http
GET /api/v1/bookings/{bookingCode}/available-rooms
POST /api/v1/bookings/{bookingCode}/check-in
```

API `GET` trả về các phòng vật lý `AVAILABLE` thuộc hạng phòng của booking để lễ tân
lựa chọn; danh sách không giữ phòng. API `POST` nhận `roomId`, khóa và kiểm tra lại
room rồi thực hiện gán phòng vật lý cùng check-in trong một transaction.
Chỉ booking `CONFIRMED` và room `AVAILABLE` thuộc đúng hạng phòng đã đặt mới hợp lệ.
Khi thành công, booking chuyển sang `CHECKED_IN` và room chuyển sang `OCCUPIED`.
Nếu booking đã được gán phòng từ trước, lễ tân được phép đổi sang room hợp lệ trong
request; assignment chỉ được thay sau khi room mới đã được khóa và kiểm tra lại.

Contract request, response, phân quyền và error code đầy đủ nằm tại
[`openapi/booking/check-in-guest.yaml`](openapi/booking/check-in-guest.yaml).

## Check-out

```http
PATCH /api/v1/bookings/{bookingId}/check-out
```

API cho lễ tân check out một booking đang `CHECKED_IN`. Booking và phòng vật lý đã
gán phải được khóa, kiểm tra và cập nhật trong cùng một transaction. Khi thành
công, booking chuyển sang `CHECKED_OUT`, phòng vật lý chuyển từ `OCCUPIED` sang
`DIRTY`, lịch sử trạng thái booking được ghi nhận và housekeeping có thể tiếp
nhận phòng cần dọn.

Trong bước check-out, lễ tân có thể ghi nhận kết quả kiểm tra phòng gồm phí
phát sinh/đền bù, lý do phát sinh và số tiền thu thêm tại quầy. Backend cộng phí
phát sinh vào `totalAmount`, cộng tiền thu thêm vào `paidAmount`, sau đó tính
outstanding cuối cùng. Booking phải có outstanding balance bằng `0` sau các điều
chỉnh này trước khi hoàn tất check-out. Nếu vẫn còn công nợ, API trả lỗi nghiệp
vụ `PAYMENT_REQUIRED` và booking vẫn giữ trạng thái `CHECKED_IN`.
Số thu thêm không vượt công nợ sau cộng phí (`CHECKOUT_OVER_COLLECTION`). Phí và số thu tối đa
9.999.999.999 đồng mỗi trường. Khoản tiền mặt tạo chứng từ `CASH-…` và giao dịch `PAYMENT / SUCCESS`
để đối soát, cùng transaction với checkout. Không nhập lại khoản chuyển khoản đã được hệ thống ghi nhận.
Trả trước ngày dự kiến phải qua luồng trả phòng sớm (`EARLY_CHECKOUT_REQUIRED`).

Mức bảo mật: `ROLE_RESTRICTED`. Các vai trò được phép là `RECEPTIONIST`,
`MANAGER` và `ADMIN`.

Contract request, response, phân quyền và error code đầy đủ nằm tại
[`openapi/booking/check-out-guest.yaml`](openapi/booking/check-out-guest.yaml).

---

# 8. Housekeeping APIs

Các API trong phần này phục vụ nghiệp vụ buồng phòng. Housekeeper xem các phòng
`DIRTY`/`CLEANING`, bắt đầu dọn và hoàn tất dọn. Mọi mutation khóa phòng và
kiểm tra state transition trong backend.

## View Dirty Rooms

```http
GET /api/v1/housekeeping/dirty-rooms?page=0&size=20
Authorization: Bearer <access_token>
```

Mức bảo mật: `ROLE_RESTRICTED`.

Vai trò được phép: `HOUSEKEEPER`.

Contract chi tiết tại
[`openapi/housekeeping/view-dirty-rooms.yaml`](openapi/housekeeping/view-dirty-rooms.yaml)
định nghĩa:

- Chỉ trả phòng vật lý có trạng thái `DIRTY`.
- Query `page` bắt đầu từ `0`; `size` từ `1` đến `100`. Giá trị mặc định lần
  lượt là `0` và `20`.
- Kết quả được sắp xếp theo `floor` tăng dần, sau đó `roomNumber` tăng dần.
- Mỗi phần tử gồm định danh phòng, số phòng, tầng, trạng thái, ghi chú vận hành
  và thông tin tóm tắt của hạng phòng.
- Danh sách rỗng trả `200 OK` với `content=[]`; không trả `404`.
- Contract không hỗ trợ tìm kiếm, lọc trạng thái, phân công nhân viên hoặc thay
  đổi trạng thái phòng.
- Frontend truy cập tại `/housekeeping`; menu và route chỉ hiển thị cho tài
  khoản có vai trò `HOUSEKEEPER`.

Các lỗi chính:

| HTTP | Mã lỗi | Trường hợp |
| --- | --- | --- |
| `400 Bad Request` | `VALIDATION_FAILED` | `page < 0` hoặc `size` ngoài khoảng `1`–`100` |
| `401 Unauthorized` | `AUTH_UNAUTHORIZED` | Thiếu access token, token không hợp lệ hoặc đã hết hạn |
| `403 Forbidden` | `AUTH_FORBIDDEN` | Tài khoản không có vai trò `HOUSEKEEPER` |
| `500 Internal Server Error` | `INTERNAL_SERVER_ERROR` | Lỗi hệ thống ngoài dự kiến |

## Mark Room Cleaned

```http
GET /api/v1/housekeeping/rooms?page=0&size=20
PATCH /api/v1/housekeeping/rooms/{roomId}/start-cleaning
PATCH /api/v1/housekeeping/rooms/{roomId}/complete-cleaning
```

Contract chi tiết tại
[`openapi/housekeeping/view-dirty-rooms.yaml`](openapi/housekeeping/view-dirty-rooms.yaml).

Mức bảo mật: `ROLE_RESTRICTED`. Chỉ `HOUSEKEEPER` được gọi. Start yêu cầu trạng
thái `DIRTY`; complete yêu cầu `CLEANING`. API complete không nhận request body
và khi thành công trả room với `status=AVAILABLE`.

Các lỗi của API complete-cleaning:

| HTTP | Mã lỗi | Trường hợp |
| --- | --- | --- |
| `400 Bad Request` | `VALIDATION_FAILED` | `roomId` không đúng định dạng UUID |
| `401 Unauthorized` | `AUTH_UNAUTHORIZED` | Thiếu access token, token không hợp lệ hoặc đã hết hạn |
| `403 Forbidden` | `AUTH_FORBIDDEN` | Tài khoản không có vai trò `HOUSEKEEPER` |
| `404 Not Found` | `ROOM_NOT_FOUND` | Không tìm thấy phòng |
| `409 Conflict` | `ROOM_CLEANING_COMPLETE_CONFLICT` | Phòng không còn ở trạng thái `CLEANING` |
| `500 Internal Server Error` | `INTERNAL_SERVER_ERROR` | Lỗi hệ thống ngoài dự kiến |

---

# 9. Add-on Management APIs

Contract OpenAPI đầy đủ: `docs/openapi/addon/manage-add-ons.yaml`.

Các API trong phần này dành cho `MANAGER` và `ADMIN`. Public API `GET /api/v1/add-ons` không thay đổi và tiếp tục chỉ trả các dịch vụ đang hoạt động.

## Tra cứu danh mục quản trị

```http
GET /management/add-ons?page=0&size=20&search=airport&active=true&sort=name,asc
Authorization: Bearer <access_token>
```

Query parameter:

| Tham số | Mặc định | Quy tắc |
|---|---:|---|
| `page` | `0` | Số nguyên từ `0` |
| `size` | `20` | Số nguyên từ `1` đến `100` |
| `search` | — | Tùy chọn; bỏ khoảng trắng hai đầu; tìm không phân biệt hoa thường theo `code` hoặc `name` |
| `active` | — | Tùy chọn; `true` hoặc `false`; bỏ qua để lấy cả hai trạng thái |
| `sort` | `name,asc` | Chỉ nhận `name`, `code`, `price`, `active`, `createdAt`; direction là `asc` hoặc `desc` |

Kết quả `200 OK` dùng response envelope chung; `data` là page object gồm `content`, `page`, `size`, `totalElements`, `totalPages`, `first`, `last`.

## Tạo dịch vụ bổ sung

```http
POST /management/add-ons
Authorization: Bearer <access_token>
Content-Type: application/json
```

```json
{
  "code": "BA_NA_HILLS_TICKET",
  "name": "Vé tham quan Bà Nà Hills",
  "description": "Vé vào cổng dành cho một khách.",
  "price": 950000
}
```

`active` không được nhận từ request tạo mới; dịch vụ luôn được tạo ở trạng thái active. Thành công trả `201 Created`, header `Location: /api/v1/management/add-ons/{id}` và resource vừa tạo trong response envelope.

## Xem chi tiết

```http
GET /management/add-ons/{id}
Authorization: Bearer <access_token>
```

Thành công trả `200 OK`, bao gồm `id`, `code`, `name`, `description`, `price`, `active`, `createdAt`, `updatedAt`.

## Cập nhật thông tin và giá

```http
PUT /management/add-ons/{id}
Authorization: Bearer <access_token>
Content-Type: application/json
```

```json
{
  "name": "Vé Bà Nà Hills",
  "description": "Vé vào cổng dành cho một khách, không gồm ăn trưa.",
  "price": 990000
}
```

`PUT` yêu cầu đủ `name` và `price`; `description` có thể là `null`. `code`, `active`, `id` và audit fields là read-only và phải bị từ chối nếu xuất hiện. Thay đổi giá chỉ áp dụng khi tạo/cập nhật booking sau đó; không sửa snapshot của booking hiện có.

## Cập nhật trạng thái

```http
PATCH /management/add-ons/{id}/status
Authorization: Bearer <access_token>
Content-Type: application/json
```

```json
{"active": false}
```

Thành công trả `200 OK` cùng resource sau cập nhật. Contract không cung cấp `DELETE`; deactivate là cơ chế loại dịch vụ khỏi public catalog và booking mới.

## Validation

| Trường | Quy tắc |
|---|---|
| `code` | Bắt buộc khi tạo; trim; 2–50 ký tự; mẫu `^[A-Z][A-Z0-9_]*$`; duy nhất không phân biệt hoa thường; bất biến sau khi tạo |
| `name` | Bắt buộc khi tạo/cập nhật; trim; 2–150 ký tự |
| `description` | Tùy chọn; trim; `null` hoặc tối đa 2000 ký tự; chuỗi chỉ có khoảng trắng được chuẩn hóa thành `null` |
| `price` | Bắt buộc khi tạo/cập nhật; số nguyên 64-bit từ `0`; đơn vị VND |
| `active` | Bắt buộc và phải là boolean trong status request |

Request body không nhận các thuộc tính ngoài schema. UUID hoặc JSON sai định dạng trả `MALFORMED_REQUEST`; vi phạm field/query trả `VALIDATION_FAILED` với map `errors`.

## Responses và error code

| HTTP | Mã lỗi | Trường hợp |
|---|---|---|
| `200 OK` | — | Tra cứu hoặc cập nhật thành công |
| `201 Created` | — | Tạo mới thành công |
| `400 Bad Request` | `VALIDATION_FAILED` | Field hoặc query parameter không hợp lệ, có thuộc tính read-only/không hỗ trợ |
| `400 Bad Request` | `MALFORMED_REQUEST` | Thiếu body, body `null`, JSON hoặc UUID sai định dạng |
| `401 Unauthorized` | `AUTH_UNAUTHORIZED` | Thiếu access token, token không hợp lệ hoặc hết hạn |
| `403 Forbidden` | `AUTH_FORBIDDEN` | Người dùng không có vai trò `MANAGER` hoặc `ADMIN` |
| `404 Not Found` | `ADD_ON_NOT_FOUND` | Không tìm thấy add-on theo ID |
| `409 Conflict` | `ADD_ON_CODE_ALREADY_EXISTS` | Code đã được sử dụng, không phân biệt hoa thường |
| `409 Conflict` | `CONFLICT_001` | Xung đột dữ liệu đồng thời hoặc uniqueness conflict không xác định được trước khi ghi |
| `500 Internal Server Error` | `INTERNAL_SERVER_ERROR` | Lỗi hệ thống ngoài dự kiến |

---

# 10. Price Rule Management APIs

Contract OpenAPI đầy đủ: `docs/openapi/pricing/manage-price-rules.yaml`.

Các API trong phần này dành cho `MANAGER` và `ADMIN`. Public pricing API `POST /api/v1/pricing/stay-price-calculations` không thay đổi và tiếp tục chỉ đọc các quy tắc giá đang hoạt động để tính giá lưu trú. Contract quản trị này không cung cấp `DELETE`; quy tắc không còn áp dụng được chuyển `active = false` để giữ lịch sử tham chiếu của booking snapshot.

## Tra cứu danh mục quy tắc giá

```http
GET /management/price-rules?page=0&size=20&search=weekend&roomTypeId=10000000-0000-0000-0000-000000000001&ruleType=WEEKEND&active=true&sort=startDate,desc
Authorization: Bearer <access_token>
```

Query parameter:

| Tham số | Mặc định | Quy tắc |
|---|---:|---|
| `page` | `0` | Số nguyên từ `0` |
| `size` | `20` | Số nguyên từ `1` đến `100` |
| `search` | — | Tùy chọn; bỏ khoảng trắng hai đầu; tìm không phân biệt hoa thường theo `name` |
| `roomTypeId` | — | Tùy chọn; UUID hợp lệ của hạng phòng |
| `ruleType` | — | Tùy chọn; một trong `WEEKEND`, `HOLIDAY`, `SEASONAL`, `PROMOTION` |
| `active` | — | Tùy chọn; `true` hoặc `false`; bỏ qua để lấy cả hai trạng thái |
| `sort` | `startDate,desc` | Chỉ nhận `name`, `ruleType`, `adjustmentValue`, `startDate`, `endDate`, `priority`, `active`, `createdAt`; direction là `asc` hoặc `desc` |

Kết quả `200 OK` dùng response envelope chung; `data` là page object gồm `content`, `page`, `size`, `totalElements`, `totalPages`, `first`, `last`. Danh sách rỗng trả `200 OK` với `content=[]`, không trả `404`.

## Tạo quy tắc giá

```http
POST /management/price-rules
Authorization: Bearer <access_token>
Content-Type: application/json
```

```json
{
  "roomTypeId": "10000000-0000-0000-0000-000000000001",
  "name": "Weekend surcharge Deluxe",
  "ruleType": "WEEKEND",
  "adjustmentType": "PERCENTAGE",
  "adjustmentValue": 20,
  "startDate": "2026-09-01",
  "endDate": "2026-12-31",
  "priority": 10
}
```

`active` không được nhận từ request tạo mới; quy tắc mới luôn được tạo ở trạng thái active. Thành công trả `201 Created`, header `Location: /api/v1/management/price-rules/{id}` và resource vừa tạo trong response envelope.

## Xem chi tiết

```http
GET /management/price-rules/{id}
Authorization: Bearer <access_token>
```

Thành công trả `200 OK`, bao gồm `id`, `roomType`, `name`, `ruleType`, `adjustmentType`, `adjustmentValue`, `startDate`, `endDate`, `priority`, `active`, `createdAt`, `updatedAt`.

## Cập nhật quy tắc giá

```http
PUT /management/price-rules/{id}
Authorization: Bearer <access_token>
Content-Type: application/json
```

```json
{
  "roomTypeId": "10000000-0000-0000-0000-000000000001",
  "name": "Weekend surcharge Deluxe",
  "ruleType": "WEEKEND",
  "adjustmentType": "PERCENTAGE",
  "adjustmentValue": 25,
  "startDate": "2026-09-01",
  "endDate": "2026-12-31",
  "priority": 10
}
```

`PUT` yêu cầu đủ các trường nghiệp vụ. `id`, `active`, `createdAt` và `updatedAt` là read-only và phải bị từ chối nếu xuất hiện. Thay đổi quy tắc chỉ ảnh hưởng các lần tính giá/booking mới; không sửa snapshot trong `booking_nights` hoặc `booking_night_price_rules` của booking đã tạo.

## Cập nhật trạng thái

```http
PATCH /management/price-rules/{id}/status
Authorization: Bearer <access_token>
Content-Type: application/json
```

```json
{"active": false}
```

Thành công trả `200 OK` cùng resource sau cập nhật. Inactive rule không được dùng cho các lần tính giá mới.

## Validation

| Trường | Quy tắc |
|---|---|
| `roomTypeId` | Bắt buộc khi tạo/cập nhật; UUID hợp lệ; hạng phòng phải tồn tại và đang `ACTIVE` |
| `name` | Bắt buộc khi tạo/cập nhật; trim; 2–150 ký tự |
| `ruleType` | Bắt buộc khi tạo/cập nhật; chỉ nhận `WEEKEND`, `HOLIDAY`, `SEASONAL`, `PROMOTION`; không nhận `BASE` vì giá cơ bản nằm ở `room_types.base_price` |
| `adjustmentType` | Bắt buộc khi tạo/cập nhật; chỉ nhận `FIXED` hoặc `PERCENTAGE` |
| `adjustmentValue` | Bắt buộc khi tạo/cập nhật; số nguyên 64-bit từ `0`; với `FIXED` là VND, với `PERCENTAGE` là phần trăm và không vượt quá `100` trong MVP |
| `startDate` | Bắt buộc khi tạo/cập nhật; ISO `yyyy-MM-dd` |
| `endDate` | Bắt buộc khi tạo/cập nhật; ISO `yyyy-MM-dd`; phải lớn hơn hoặc bằng `startDate` |
| `priority` | Bắt buộc khi tạo/cập nhật; số nguyên từ `0` đến `1000`; số lớn hơn được áp dụng trước khi nhiều rule cùng hiệu lực |
| `active` | Bắt buộc và phải là boolean trong status request; không được gửi trong create/update request |

Request body không nhận các thuộc tính ngoài schema. UUID, enum, ngày hoặc JSON sai định dạng trả `MALFORMED_REQUEST`; vi phạm field/query trả `VALIDATION_FAILED` với map `errors`.

## Business boundary

- `PriceRule` áp dụng theo hạng phòng và khoảng ngày hiệu lực.
- Khoảng hiệu lực là inclusive: một rule áp dụng cho `stayDate` khi `startDate <= stayDate <= endDate`.
- Chỉ active rule được dùng cho các lần tính giá mới.
- `BASE` không nằm trong API quản trị này; chỉnh giá cơ bản thuộc quản lý `room_types.base_price`.
- Nhiều active rule có thể cùng hiệu lực trên một đêm. Engine hiện tại áp dụng tất cả rule phù hợp, tách surcharge và promotion, rồi sắp xếp theo `priority` giảm dần trong từng nhóm.
- `PUT` và `PATCH status` khóa pessimistic row price rule trong transaction. Hai mutation đồng thời trên cùng rule được tuần tự hóa; command cập nhật chi tiết không tự ghi đè trạng thái và command trạng thái không tự ghi đè chi tiết.
- `PROMOTION` là rule giảm giá; các rule còn lại (`WEEKEND`, `HOLIDAY`, `SEASONAL`) là phụ thu.
- Với `PERCENTAGE`, contract MVP giới hạn `adjustmentValue <= 100` để tránh cấu hình cực đoan; nếu business cần phụ thu trên 100%, phải cập nhật contract trước.
- Không hard-delete price rule trong phạm vi này vì booking cũ có thể đã snapshot rule qua `booking_night_price_rules`.

## Response example

```json
{
  "success": true,
  "message": "Lấy chi tiết quy tắc giá thành công",
  "data": {
    "id": "20000000-0000-0000-0000-000000000001",
    "roomType": {
      "id": "10000000-0000-0000-0000-000000000001",
      "code": "DLX",
      "name": "Deluxe"
    },
    "name": "Weekend surcharge Deluxe",
    "ruleType": "WEEKEND",
    "adjustmentType": "PERCENTAGE",
    "adjustmentValue": 20,
    "startDate": "2026-09-01",
    "endDate": "2026-12-31",
    "priority": 10,
    "active": true,
    "createdAt": "2026-08-29T08:00:00Z",
    "updatedAt": null
  },
  "timestamp": "2026-08-29T08:00:00Z"
}
```

## Responses và error code

| HTTP | Mã lỗi | Trường hợp |
|---|---|---|
| `200 OK` | — | Tra cứu hoặc cập nhật thành công |
| `201 Created` | — | Tạo mới thành công |
| `400 Bad Request` | `VALIDATION_FAILED` | Field hoặc query parameter không hợp lệ, có thuộc tính read-only/không hỗ trợ |
| `400 Bad Request` | `MALFORMED_REQUEST` | Thiếu body, body `null`, JSON, UUID, enum hoặc ngày sai định dạng |
| `401 Unauthorized` | `AUTH_UNAUTHORIZED` | Thiếu access token, token không hợp lệ hoặc hết hạn |
| `403 Forbidden` | `AUTH_FORBIDDEN` | Người dùng không có vai trò `MANAGER` hoặc `ADMIN` |
| `404 Not Found` | `ROOM_001` | Không tìm thấy hạng phòng active theo `roomTypeId` |
| `404 Not Found` | `PRICE_RULE_NOT_FOUND` | Không tìm thấy quy tắc giá theo ID |
| `409 Conflict` | `PRICE_RULE_CONFLICT` | Database từ chối thao tác ghi do constraint; không có nghĩa các khoảng rule overlap bị cấm |
| `500 Internal Server Error` | `INTERNAL_SERVER_ERROR` | Lỗi hệ thống ngoài dự kiến |

---

# 11. Admin APIs

Các API trong phần này chưa được triển khai. FR-11–FR-13 chưa quy định đầy đủ quy tắc xóa, dữ liệu đang được tham chiếu, lịch sử giá hay ảnh hưởng đến booking hiện hữu cho toàn bộ nhóm admin CRUD. Tên đường dẫn `/admin` cũng không đủ để kết luận vai trò được phép. Vì vậy các đường dẫn dưới đây chỉ là dự kiến, chưa phải API contract; mức bảo mật và vai trò được phép là `CẦN XÁC NHẬN NGHIỆP VỤ`. Riêng quản lý quy tắc giá đã có contract chính thức tại phần Price Rule Management APIs và dùng đường dẫn `/api/v1/management/price-rules`.

Phạm vi Admin đã triển khai hiện chỉ gồm những endpoint ghi rõ `ADMIN` trong bảng endpoint hiện có (report, payment monitoring/raw webhook, add-on management và các quyền dùng chung khác). Không suy diễn một CRUD tổng quát cho Admin.

## Room Type Management

```http
GET /admin/room-types
POST /admin/room-types
PUT /admin/room-types/{id}
DELETE /admin/room-types/{id}
```

## Physical Room Management

```http
GET /admin/rooms
POST /admin/rooms
PUT /admin/rooms/{id}
PATCH /admin/rooms/{id}/maintenance
```

---

# 12. AI APIs

## Unified Hotel AI Chat

```http
POST /api/v1/ai/chat/stream
Authorization: Bearer <access-token>
Accept: text/event-stream
Content-Type: application/json

{
  "conversationId": "11111111-1111-4111-8111-111111111111",
  "message": "Mấy giờ khách có thể nhận phòng?"
}
```

- Đây là conversational entry point duy nhất và yêu cầu authentication.
- `conversationId` là UUID bắt buộc; `message` bắt buộc và không rỗng.
- Model tự chọn normal chat, semantic policy RAG, read-only tool calling hoặc kết hợp chúng.
- SSE event ổn định: `delta`, `action`, `sources`, `done`, `error`.
- `sources` chỉ chứa `title`, `category`, `section` từ retrieved chunk; không expose database ID,
  vector, prompt hoặc provider response.
- Validation sai trả `400 VALIDATION_FAILED`.
- Vượt giới hạn mặc định 10 request/phút/principal trả `429`.
- Policy không đủ evidence phải trả lời không tìm thấy, không suy diễn.
- Tool booking/payment lấy identity từ security context và enforce ownership ở service layer.
- Không có mutation tool.

Các endpoint cũ `POST /api/v1/ai/chat` và `POST /api/v1/ai/policy-assistant` đã bị xóa và trả `404`.
OpenAPI chi tiết: [`openapi/ai/unified-chat.yaml`](./openapi/ai/unified-chat.yaml).

---

# 13. Error Codes

| Code | Meaning |
| --- | --- |
| AUTH_001 | Thông tin đăng nhập không hợp lệ |
| AUTH_002 | Token xác thực đã hết hạn |
| AUTH_UNAUTHORIZED | Access token bị thiếu, không hợp lệ hoặc đã hết hạn |
| AUTH_RESET_TOKEN_INVALID | Mã đặt lại mật khẩu không hợp lệ, hết hạn, đã dùng hoặc bị thu hồi |
| AUTH_RESET_TOKEN_EXPIRED | Mã đặt lại mật khẩu đã hết hạn |
| AUTH_RESET_TOKEN_USED | Mã đặt lại mật khẩu đã được sử dụng hoặc bị thu hồi |
| AUTH_RATE_LIMITED | Vượt quá giới hạn yêu cầu xác thực |
| AUTH_FORBIDDEN | Xác thực thành công nhưng người dùng không có vai trò được phép |
| USER_001 | Không tìm thấy người dùng đã xác thực |
| USER_002 | Tài khoản người dùng không hoạt động |
| VALIDATION_FAILED | Dữ liệu yêu cầu không hợp lệ |
| MALFORMED_REQUEST | Nội dung yêu cầu bị thiếu hoặc không đúng định dạng |
| PRICING_VALIDATION_FAILED | Khoảng ngày hoặc cấu hình tính giá không hợp lệ |
| PRICE_RULE_NOT_FOUND | Không tìm thấy quy tắc giá |
| PRICE_RULE_CONFLICT | Xung đột dữ liệu khi cập nhật quy tắc giá |
| PHONE_ALREADY_EXISTS | Số điện thoại đã được người dùng khác sử dụng |
| ROOM_001 | Không tìm thấy hạng phòng |
| ADD_ON_NOT_FOUND | Không tìm thấy dịch vụ bổ sung |
| ADD_ON_CODE_ALREADY_EXISTS | Mã dịch vụ bổ sung đã tồn tại |
| BOOKING_001 | Phòng không còn khả dụng |
| BOOKING_002 | Đặt phòng đã hết hạn |
| BOOKING_003 | Trạng thái đặt phòng không hợp lệ |
| PAYMENT_001 | Thanh toán thất bại |
| CONFLICT_001 | Xung đột dữ liệu đồng thời |
| INTERNAL_SERVER_ERROR | Lỗi hệ thống không mong muốn |

---

# 14. Swagger

Swagger UI:

```text
/swagger-ui/index.html
```
# Quản lý bảo trì phòng

Các endpoint dưới đây chỉ dành cho `MANAGER`:

- `GET /api/v1/rooms?page=0&size=100`: danh sách phòng vật lý có phân trang, tối đa 100 phòng mỗi trang.
- `POST /api/v1/maintenance-requests`: tạo phiếu bằng `roomId`, `title` (1–150 ký tự) và `description` (tối đa 2000 ký tự).
- `GET /api/v1/maintenance-requests?page=0&size=20`: danh sách phiếu.
- `GET /api/v1/maintenance-requests/{id}`: chi tiết phiếu và thời điểm xử lý.
- `PATCH /api/v1/maintenance-requests/{id}/status`: `PENDING → IN_PROGRESS → COMPLETED`.

Tạo phiếu chỉ được thực hiện khi phòng `AVAILABLE`, đồng thời chuyển phòng sang `MAINTENANCE`.
Hoàn tất phiếu đưa phòng về `AVAILABLE`. Mỗi phòng chỉ có một phiếu chưa hoàn tất;
phòng và phiếu được khóa và cập nhật trong cùng transaction.

`PATCH /api/v1/rooms/{roomNumber}/maintenance` vẫn hỗ trợ thao tác trực tiếp với phòng
chưa có phiếu mở. Nếu có phiếu `PENDING` hoặc `IN_PROGRESS`, trả `409 ROOM_HAS_ACTIVE_MAINTENANCE_REQUEST`
và yêu cầu xử lý qua phiếu để tránh sai lệch trạng thái. API phân tích bảo trì bằng AI đã được loại bỏ.

UI mới ở `/maintenance`; đường dẫn cũ `/rooms/maintenance` chuyển hướng sang trang này,
đồng thời giữ kiểm tra vai trò sau đăng nhập.

Contract chi tiết: [`manage-room-maintenance-status.yaml`](openapi/housekeeping/manage-room-maintenance-status.yaml).

## Stay modification commands (booking resource ID)

`POST /api/v1/bookings/{bookingId}/cancel` supersedes the earlier booking-code cancellation command. Eligibility and human-facing views retain `bookingCode`.

`GET /api/v1/bookings/{bookingId}/early-checkout?actualCheckOutDate=yyyy-MM-dd` trả số đêm, số tiền chưa sử dụng, phí 20% và khoản hoàn 80% do backend tính. `POST /api/v1/bookings/{bookingId}/early-checkout` nhận ngày trả thực tế, ghi chú, phí phát sinh/đền bù, lý do và số tiền mặt cần thu thêm. Backend bù trừ phí và công nợ vào khoản hoàn; chỉ chấp nhận đúng số tiền cần thu, rồi trả yêu cầu hoàn `PENDING` (nếu còn khoản hoàn), booking `CHECKED_OUT` và phòng `DIRTY`.

`POST /api/v1/bookings/{bookingId}/extend` accepts `newCheckOutDate`. It returns added nights/amount, new total, outstanding amount, payment-required flag and retained room ID. A same-room or inventory conflict returns `409 ROOM_NOT_AVAILABLE_FOR_EXTENSION`; no room move occurs.
