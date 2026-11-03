# 11. Testing Strategy

# HANBAY HOTEL

## Testing Strategy Document

---

# 1. Testing Goals

- Verify core business flows.
- Prevent overbooking.
- Validate state transitions.
- Ensure API correctness.
- Ensure payment webhook idempotency.

---

# 2. Test Levels

## Unit Test

Target:

- Service layer
- Price calculation
- Booking validation
- State machine

Tools:

- JUnit 5
- Mockito
- AssertJ

---

## Integration Test

Target:

- Repository queries
- Transaction behavior
- Booking availability
- Payment webhook

Tools:

- Spring Boot Test
- Testcontainers
- PostgreSQL

---

## API Test

Target:

- REST endpoints
- Authentication
- Authorization
- Error response

Tools:

- Postman
- Swagger
- Newman

---

## Frontend Test

Target:

- Booking flow
- Form validation
- API integration

Tools:

- Manual test
- React Testing Library if time allows

---

# 3. Critical Test Cases

## TC-01 Search Available Room

Given requested date range  
When user searches room  
Then system returns available room types.

## TC-02 Overlapping Booking

Given an existing booking  
When user searches overlapped dates  
Then available count decreases.

## TC-03 Create Pending Booking

Given RoomType đang hoạt động và còn inventory  
When khách hoàn thành hai bước đầu và bấm "Kiểm tra đơn hàng"  
Then frontend gửi `POST /api/v1/bookings`, API trả `201`, booking có trạng thái PENDING, `expiredAt = now + 15 phút` và frontend chuyển sang trang thanh toán.

Các trường hợp cần kiểm tra:

- Tạo booking anonymous thành công, không yêu cầu access token.
- Guest, status history, nightly pricing snapshot và add-on snapshot được persist cùng transaction.
- RoomType không tồn tại/inactive trả `404`.
- Ngày không hợp lệ, vượt sức chứa hoặc add-on không hợp lệ trả `400`.
- Hết phòng sau availability re-check trả `409` và transaction rollback.
- Pending booking chưa hết hạn được tính vào occupied inventory.
- Frontend giữ dữ liệu RoomType/ngày lưu trú khi chuyển từ bước chọn phòng sang thông tin khách.
- Frontend chỉ mở trang thanh toán sau khi create API thành công.
- Trang thanh toán hiển thị booking code, trạng thái, tổng tiền và thời điểm hết hạn từ response thật.

## TC-04 Expire Pending Booking

Given booking expired  
When scheduler runs  
Then booking becomes EXPIRED.

## TC-05 Prevent Overbooking

Given only 1 room available  
When two users book at same time  
Then only one booking succeeds.

## TC-06 Create Payment Request

Cho trước khách đã tạo đặt phòng `PENDING` còn hạn thanh toán
Khi frontend gọi `POST /api/v1/payments/requests` với `bookingId` và mã truy cập đặt phòng
Thì API trả `201`, tạo payment trạng thái `PENDING` và trả thông tin chuyển khoản SePay.

Cần kiểm tra thêm:

- Khách vãng lai thiếu hoặc sai mã truy cập đặt phòng nhận `404` để tránh lộ dữ liệu.
- Request thiếu hoặc sai định dạng `bookingId` trả `400 VALIDATION_FAILED`.
- Đặt phòng không tồn tại trả `404 PAYMENT_BOOKING_NOT_FOUND`.
- Đặt phòng `EXPIRED`, `CANCELLED`, `CONFIRMED` hoặc hết hạn giữ phòng trả `409 PAYMENT_BOOKING_NOT_PAYABLE`.
- Webhook nhận đủ tiền chuyển payment sang `SUCCESS` và booking sang `CONFIRMED` đúng một lần.
- Thanh toán thiếu tiền, quá tiền, đến trễ và webhook trùng đều được xử lý an toàn.
- Booking da co payment `SUCCESS` tra `409 PAYMENT_ALREADY_COMPLETED`.
- Booking da co payment `PENDING` con hieu luc thi tra lai payment do, khong tao duplicate.
- `amount` trong response phai lay tu booking snapshot, client khong duoc truyen hoac override so tien.
- FE hien thi QR SePay, ngan hang, so tai khoan, ten tai khoan, so tien va noi dung chuyen khoan.
- Gửi email xác nhận không nằm trong phạm vi create payment request.

## TC-07 Payment Webhook Idempotency

Given hai webhook hợp lệ có cùng `id` được gửi tuần tự  
When endpoint xử lý request thứ hai  
Then API trả `200`, chỉ có một event mang khóa `sepay:event:<id>` và event giữ trạng thái `RECEIVED`.

Kiểm tra thêm:

- Thiếu hoặc sai `Authorization: Apikey <key>` trả `401` với code `INVALID_WEBHOOK_API_KEY`.
- Payload vi phạm validation trả `400` với code `VALIDATION_FAILED`; JSON, enum hoặc ngày giờ sai định dạng trả `MALFORMED_REQUEST`.
- Field tùy chọn (`subAccount`, `code`, `description`, `referenceCode`) được phép thiếu hoặc `null`.
- Field không được khai báo trong DTO bị bỏ qua để tương thích khi SePay bổ sung metadata.
- ACK `200` chỉ xác nhận event đã được tiếp nhận; không dùng booking status làm assertion ở phiên bản hiện tại.
- Cần bổ sung integration test cho hai request cùng `id` đến đồng thời trước khi cam kết idempotency production.

## TC-08 HAN-33 - Trang kết quả thanh toán

Phạm vi:

- Xác nhận `GET /api/v1/payments/{paymentId}/status` là nguồn dữ liệu duy nhất của trang kết quả.
- Xác nhận guest tra cứu bằng booking access token hợp lệ và chủ booking đăng nhập có thể tra cứu bằng JWT; token không xuất hiện trong URL.
- Xác nhận giao diện phân biệt rõ `PENDING`, `SUCCESS`, `FAILED`, `CANCELLED`, `EXPIRED`, `REFUNDED` và `PARTIALLY_REFUNDED`.
- Xác nhận frontend dừng polling khi payment không còn ở trạng thái `PENDING`.
- Xác nhận late payment và overpayment ưu tiên hiển thị trạng thái, số tiền và lý do refund.

Kiểm tra tự động:

| Lớp | Trường hợp | Kết quả mong đợi |
|---|---|---|
| Service | Guest có booking access token hợp lệ | Trả đúng payment, booking, số tiền và thời gian thanh toán |
| Service | Token sai hoặc không có quyền sở hữu | Trả `404 PAYMENT_BOOKING_NOT_FOUND`, không tiết lộ payment |
| Service | Payment `PENDING` đã quá hạn | Chuyển payment sang `EXPIRED` trước khi trả response |
| Controller | `paymentId` không phải UUID | Trả `400 VALIDATION_FAILED` |
| Security | Guest ẩn danh gửi booking access token | Route được phép truy cập và service tiếp tục kiểm tra token |
| Frontend | `SUCCESS` và booking `CONFIRMED` | Hiển thị thành công, mã booking, mã giao dịch và số tiền đã ghi nhận |
| Frontend | `PENDING` | Hiển thị đang đối soát và tiếp tục polling mỗi 2 giây |
| Frontend | `FAILED` hoặc `EXPIRED` | Hiển thị phản hồi phù hợp và hành động quay lại luồng đặt phòng |
| Frontend | API lỗi tạm thời | Hiển thị lỗi dễ hiểu, tiếp tục polling với backoff và cho phép `Thử lại` |
| Frontend | Không có token trong `sessionStorage`, user đã đăng nhập | Gọi API bằng JWT của owner |
| Frontend | Refund `PENDING`/`PROCESSING` | Thông báo đã nhận tiền, số tiền và lý do đang hoàn |

Lệnh kiểm tra:

```bash
cd hotel-booking
./mvnw -Dtest='PaymentServiceImplTest,BookingPaymentControllerTest,BookingPaymentSecurityIntegrationTest' test
./mvnw spotless:check

cd ../client
npm run test:run -- src/features/booking/api/booking-api.test.ts src/features/booking/pages/payment-result-page.test.tsx
npm run format:check
npm run lint
npm run build
```

Kiểm tra thủ công frontend-backend:

1. Tạo booking `PENDING`, tạo payment và giữ nguyên tab để xác nhận trang thanh toán vẫn polling.
2. Mô phỏng webhook hợp lệ, xác nhận frontend chuyển đến `/payments/{paymentId}/result` và hiển thị `SUCCESS`/`CONFIRMED`.
3. Tải lại trang kết quả trong cùng tab, xác nhận booking access token trong `sessionStorage` vẫn dùng được.
4. Mở URL kết quả ở tab mới khi đã đăng nhập, xác nhận API dùng JWT; mở ẩn danh không có booking token phải nhận `404` và không lộ thông tin.
5. Kiểm tra payment thất bại, bị hủy và hết hạn; nội dung và hành động điều hướng phải phù hợp từng trạng thái.
6. Tạm dừng backend, bấm `Thử lại` sau khi backend hoạt động và xác nhận kết quả được tải lại.
7. Kiểm tra desktop, tablet, mobile và điều hướng bằng bàn phím.

Request kiểm tra thủ công nằm tại:

```text
hotel-booking/http/HAN-33-payment-result.http
```


## TC-09 HAN-24 - Kiểm thử tính giá phòng khi tìm kiếm

Phạm vi:

- Xác nhận `GET /api/v1/room-types/available` trả về hạng phòng còn trống cùng giá lưu trú đã tính.
- Xác nhận backend kiểm tra khoảng ngày, sức chứa và `roomTypeId` tùy chọn.
- Xác nhận frontend hiển thị giá cơ bản, `totalNights`, `roomTotal` và chi tiết giá từng đêm.

Kiểm tra backend:

| Trường hợp | Yêu cầu | Kết quả mong đợi |
|---|---|---|
| Tìm kiếm hợp lệ trong 3 đêm | `checkIn=2027-08-12`, `checkOut=2027-08-15`, `adults=2`, `children=0` | `200 OK`, `totalNights=3`, `roomTotal` bằng tổng `nightlyPrices[].price` và bằng `roomSubtotal - roomDiscount` |
| Ngày trả trùng ngày nhận | `checkIn == checkOut` | `400 Bad Request` kèm lỗi dữ liệu không hợp lệ |
| Số khách không hợp lệ | `adults=0` hoặc `children` âm | `400 Bad Request` kèm lỗi dữ liệu không hợp lệ |
| Kiểm tra lại hạng phòng đã chọn | `roomTypeId` hợp lệ với `page=0`, `size=1` | Chỉ trả về hạng phòng đã chọn nếu còn phòng |
| Vượt sức chứa | Số khách lớn hơn sức chứa | Hạng phòng bị loại khỏi kết quả |
| Trùng thời gian đặt phòng | Có đặt phòng khác giao với khoảng tìm kiếm | Số phòng trống giảm hoặc hạng phòng bị loại khi đã kín |
| Có nhiều quy tắc trong một đêm | Một phụ thu và một khuyến mãi cùng có hiệu lực | `adjustments` chứa riêng từng quy tắc với đúng tên, loại và số tiền |

Kiểm tra frontend thủ công:

1. Khởi động PostgreSQL, backend và frontend.
2. Mở trang tìm phòng và tìm một khoảng hợp lệ có ít nhất 2 đêm.
3. Xác nhận mỗi thẻ hiển thị giá cơ bản, `totalNights` và tổng tiền do API trả về.
4. Xác nhận chi tiết từng đêm liệt kê từ `checkIn` đến ngày liền trước `checkOut`.
5. Bấm “Xem chi tiết điều chỉnh giá” bằng chuột, bàn phím và thiết bị cảm ứng; nội dung phải mở được.
6. Xác nhận tên và số tiền điều chỉnh khớp với `adjustments`, không suy đoán ngày lễ hoặc cuối tuần ở frontend.
7. Thay đổi khoảng ngày; số đêm và tổng tiền phải cập nhật theo response mới.
8. Bấm `Chọn phòng`; phần tóm tắt đặt phòng phải giữ nguyên `roomTotal` đã tính.

File hỗ trợ kiểm tra:

```text
hotel-booking/http/HAN-24-room-price-search.http
```

## TC-09A HAN-42 - Kiểm thử quản lý quy tắc giá

Phạm vi:

- Xác nhận `MANAGER` và `ADMIN` có thể quản lý price rules qua `/api/v1/management/price-rules`.
- Xác nhận guest/public pricing chỉ đọc các rule đang active khi tính giá lưu trú.
- Xác nhận frontend quản trị dùng dữ liệu API thật, không tự suy đoán phụ thu hoặc khuyến mãi.

Kiểm tra backend:

| Trường hợp | Yêu cầu | Kết quả mong đợi |
|---|---|---|
| Tra cứu danh sách không filter | `GET /api/v1/management/price-rules` | `200 OK`, trả page object; danh sách rỗng trả `content=[]`, không trả `404` |
| Tra cứu có filter | Gửi `search`, `roomTypeId`, `ruleType`, `active`, `sort` hợp lệ | Chỉ trả rule khớp filter, không lỗi khi filter bị bỏ trống |
| Tạo rule hợp lệ | Gửi đủ `roomTypeId`, `name`, `ruleType`, `adjustmentType`, `adjustmentValue`, `startDate`, `endDate`, `priority` | `201 Created`, có `Location`, rule mới luôn `active=true` |
| Cập nhật rule hợp lệ | `PUT /api/v1/management/price-rules/{id}` với payload đầy đủ | `200 OK`, dữ liệu phản hồi khớp thay đổi |
| Cập nhật trạng thái | `PATCH /api/v1/management/price-rules/{id}/status` | Rule active/inactive theo request; inactive rule không được dùng cho lần tính giá mới |
| Validation sai | Thiếu field, enum sai, ngày kết thúc trước ngày bắt đầu, percentage lớn hơn 100 hoặc amount âm | `400` với error code/field errors phù hợp |
| Nhiều rule overlap | Tạo nhiều active rule cùng hiệu lực trên một đêm | Các rule cùng được lưu và pricing engine áp dụng theo stacking hiện hữu; chính sách chọn một rule duy nhất vẫn là `NEEDS PRODUCT DECISION` |
| Cập nhật đồng thời cùng rule | Hai transaction cập nhật chi tiết/trạng thái cùng một rule | Repository lấy pessimistic write lock; transaction sau đọc trạng thái đã commit, không ghi đè ngầm thay đổi ở field ngoài command của nó |
| Không đủ quyền | Anonymous hoặc role không thuộc `MANAGER`/`ADMIN` gọi API quản trị | `401` hoặc `403` theo trạng thái xác thực |
| Áp dụng giá phía khách | Public search/stay price nằm trong khoảng rule active | Giá từng đêm, tổng tiền và adjustment breakdown khớp rule |

Kiểm tra frontend thủ công:

1. Đăng nhập bằng admin hoặc manager, mở màn `Quản lý quy tắc giá`.
2. Xác nhận loading, empty, error/retry và danh sách rule hiển thị đúng.
3. Tạo rule weekend/holiday cho một hạng phòng, kiểm tra thông báo thành công và dòng mới xuất hiện.
4. Sửa rule, kiểm tra bảng cập nhật đúng tên, loại, kiểu điều chỉnh, giá trị, ngày và trạng thái.
5. Ngưng rule rồi kích hoạt lại, kiểm tra UI feedback và dữ liệu được reload.
6. Dùng tìm kiếm, filter hạng phòng, loại rule, trạng thái, sort và phân trang.
7. Sang luồng đặt phòng phía khách, chọn ngày nằm trong khoảng rule active và kiểm tra giá điều chỉnh.
8. Chọn ngày ngoài khoảng rule hoặc ngưng rule rồi tìm lại, giá phải quay về giá không có rule.
9. Kiểm tra popup chi tiết điều chỉnh giá hiển thị một dòng cho mỗi đêm được áp rule.

Lệnh kiểm tra:

```bash
cd hotel-booking
./mvnw -Dtest='PriceRuleManagementServiceImplTest,PriceRuleManagementControllerTest,PriceRuleManagementSecurityIntegrationTest,PricingServiceTest,PricingControllerTest,PricingSecurityIntegrationTest,AvailableRoomTypeSearchServiceTest' test
./mvnw spotless:check

cd ../client
npm run test:run -- price-rule-management available-room-results stay-price
npm run lint
npm run build
```

## TC-10 Check-in State

Given booking is CONFIRMED  
When receptionist checks in  
Then booking becomes CHECKED_IN and room becomes OCCUPIED.

## TC-11 Check-out State

Given room is OCCUPIED  
When guest checks out  
Then room becomes DIRTY.

---

# 4. Manual Demo Test Flow

```text
Search Room
→ Create Booking
→ Simulate Payment Success
→ Check-in
→ Check-out
→ Mark Room Cleaned
```

---

# 5. Test Coverage Target

Minimum:

- Service layer: 60%
- Critical booking logic: 80%
- Price calculation: 80%

---

# 6. HAN-105 - View Room Type Catalog QA

## Scope

This QA scope verifies the public room type catalog delivered by HAN-101 through HAN-104:

- Public `GET /api/v1/room-types` contract.
- Active room type filtering.
- Case-insensitive name search.
- Pagination and sorting by `name` or `basePrice`.
- Adult and child capacities returned separately.
- Anonymous customer access.
- Catalog UI integration, including loading, success, empty, error, retry, search, sort, pagination and responsive states.

Availability by stay dates, physical room selection and booking creation are outside this story. They require separate APIs and acceptance criteria.

## Acceptance Criteria Traceability

| Acceptance criterion | Verification | Expected result |
|---|---|---|
| Anonymous customers can browse room types | `RoomTypeCatalogSecurityIntegrationTest` and manual browser test without a token | `200 OK` and catalog content is visible |
| Only active room types are exposed | `RoomTypeRepositoryIntegrationTest` | Inactive records are excluded |
| Search is case-insensitive | Repository integration test and UI search | Matching active room types are returned regardless of keyword case |
| Catalog supports pagination and sorting | `RoomTypeCatalogServiceTest` and manual Network inspection | Page metadata and requested sort order are correct |
| Invalid query parameters are rejected | Service and controller tests | `400 Bad Request` with `VALIDATION_FAILED` |
| Contract fields are returned | Service mapping and controller response tests | ID, name, description, adult/child capacity, base price and nullable thumbnail are present |
| Frontend consumes the public API | `room-type-api.test.ts` and local frontend/backend test | Request uses `/api/v1/room-types` and renders backend data |
| UI handles non-success states | Manual browser test | Loading, empty, connection error and retry states are understandable |

## Automated Tests

Backend:

```bash
cd hotel-booking
./mvnw test
```

Covered backend test classes:

- `RoomTypeCatalogServiceTest`
- `RoomTypeCatalogControllerTest`
- `RoomTypeCatalogSecurityIntegrationTest`
- `RoomTypeRepositoryIntegrationTest`

Frontend:

```bash
cd client
npm run test:run
npm run lint
npm run format:check
npm run build
```

The API client tests verify response unwrapping, query parameters, keyword trimming and omission of blank keywords.

## Manual Frontend-Backend Test

Prerequisites:

1. PostgreSQL container is healthy.
2. Backend is running at `http://localhost:8080`.
3. Frontend is running at `http://localhost:5173` with `VITE_API_BASE_URL=/api/v1`.

Test flow:

1. Open the home page in a signed-out browser session.
2. Confirm the seeded Superior, Deluxe and Junior Suite room types are displayed.
3. Search for `Deluxe` and confirm only the matching room type remains.
4. Search for a missing name and confirm the empty state is displayed.
5. Sort by name ascending/descending and price ascending/descending.
6. Inspect the Network request and confirm the query parameters match the selected filters.
7. Stop the backend, confirm the error state, restart it and confirm the Retry action reloads data.
8. Open a room type detail and confirm the selected room type and booking parameters are preserved.
9. Repeat the catalog flow at desktop, tablet and mobile widths.

## Local Verification Record

Verified on 2026-08-05:

- Backend Maven tests: `15` passed, `0` failed.
- Frontend Vitest tests: `2` passed, `0` failed.
- Frontend TypeScript production build: passed.

---

# 7. HAN-216 - Manage Add-on Services API Contract QA

## Scope

HAN-216 thiết kế contract; việc triển khai tự động hóa các kiểm thử dưới đây thuộc HAN-217, HAN-218 và HAN-220.

| Contract rule | Verification required | Expected result |
|---|---|---|
| Chỉ manager/admin truy cập management API | Security integration test cho anonymous, guest, receptionist, manager và admin | Anonymous nhận `401`; role không hợp lệ nhận `403`; manager/admin truy cập được |
| Public catalog không đổi | Controller/repository integration test | `GET /api/v1/add-ons` chỉ trả active add-on |
| Management list đầy đủ | Repository/service/controller test | Hỗ trợ active/inactive, search, pagination và allowlisted sorting |
| Code duy nhất và bất biến | Service/repository integration test | Trùng code không phân biệt hoa thường trả `409`; update không nhận code |
| Không hard-delete | API contract/controller test | Không công bố `DELETE`; status endpoint bật/tắt dịch vụ |
| Deactivate ảnh hưởng booking mới | Booking service integration test | Inactive add-on không thể được thêm vào booking mới |
| Snapshot booking được bảo toàn | Booking integration test | Giá catalog thay đổi không sửa `unitPrice`/`totalPrice` đã lưu |
| Validation đúng contract | DTO/controller test | Field/query sai trả `400` cùng `VALIDATION_FAILED` hoặc `MALFORMED_REQUEST` |
| Error envelope nhất quán | Controller test | Error code, message, path, timestamp và errors đúng schema |

OpenAPI contract phải được parse thành công và được đối chiếu với `docs/06-api-specification.md` trước khi hoàn tất implementation.
- Frontend ESLint: passed.
- Frontend-backend manual integration: passed.

Pull request review and approval remain required before HAN-105 satisfies the full Definition of Done.

---

# 7. HAN-100 - Kiểm thử quản lý thông tin cá nhân

## Phạm vi

Phạm vi kiểm thử xác nhận luồng thông tin cá nhân được triển khai từ HAN-96 đến HAN-99:

- `GET /api/v1/users/me` trả về hồ sơ của tài khoản đang hoạt động.
- `PATCH /api/v1/users/me` chỉ cập nhật từng phần đối với `fullName` và `phone`.
- Backend lấy mã người dùng từ JWT; client không thể lựa chọn hồ sơ của người khác.
- Dữ liệu sai, tài khoản không tồn tại hoặc ngừng hoạt động, trùng số điện thoại và JSON sai định dạng đều trả lỗi tiếng Việt đã công bố.
- Route frontend được bảo vệ, đồng bộ hồ sơ vào session và hiển thị lịch sử đặt phòng ngay trong trang hồ sơ.

Tài khoản khách có thể có danh sách `roles` rỗng; frontend hiển thị trường hợp này là “Khách hàng”.

Tải ảnh đại diện, đổi mật khẩu, tùy chọn thông báo và nhật ký hoạt động không thuộc phạm vi story nên không hiển thị nút thao tác giả trên giao diện.

## Đối chiếu tiêu chí chấp nhận

| Tiêu chí | Cách kiểm tra | Kết quả mong đợi |
|---|---|---|
| Người dùng đã đăng nhập xem được hồ sơ của mình | Test security, controller, service và trình duyệt | `200 OK`, không nhận mã người dùng từ client |
| Người dùng ẩn danh không được truy cập | Test security và protected route | API trả `401 AUTH_UNAUTHORIZED`, UI chuyển đến `/login` |
| Cập nhật từng phần | Test service, controller, API client và form | Chỉ gửi và lưu trường `fullName`/`phone` đã thay đổi |
| Không sửa được trường chỉ đọc | Test DTO/controller và control bị khóa | Trả `400 VALIDATION_FAILED` cho trường không hỗ trợ |
| Xử lý dữ liệu sai | Test service/controller | Trả lỗi trường bằng tiếng Việt; JSON sai trả `MALFORMED_REQUEST` |
| Xử lý trùng số điện thoại | Test service và API thủ công | Trả `409 PHONE_ALREADY_EXISTS`; phone null không bị báo trùng |
| Xử lý tài khoản không tồn tại/ngừng hoạt động | Test service/controller | Trả `404 USER_001` hoặc `403 USER_002` |
| Lịch sử đặt phòng nằm trong hồ sơ | Test UI và trình duyệt | Lọc, phân trang và xem chi tiết không chuyển sang trang lịch sử riêng |
| Hồ sơ cập nhật được đồng bộ | Test React Query, storage và tải lại trang | Header/form hiển thị dữ liệu mới mà không cần đăng nhập lại |

## Kiểm thử tự động

Backend profile suite:

```bash
cd hotel-booking
./mvnw -Dtest='UserServiceImplTest,UserControllerTest,UserSecurityIntegrationTest,UpdateUserRequestTest' test
./mvnw spotless:check
```

Frontend suite:

```bash
cd client
npm run format:check
npm run lint
npm run test:run
npm run build
```

## Kiểm thử thủ công frontend–backend

Điều kiện:

1. PostgreSQL hoạt động và Flyway xác nhận toàn bộ migration hiện tại.
2. Backend chạy tại `http://localhost:8080`.
3. Frontend chạy tại `http://localhost:5173` với `VITE_API_BASE_URL=/api/v1`.

Các bước:

1. Đăng ký và đăng nhập bằng tài khoản khách.
2. Mở `/account/profile`, xác nhận email, trạng thái và vai trò chỉ đọc.
3. Cập nhật riêng họ tên rồi riêng số điện thoại; trường còn lại phải giữ nguyên.
4. Nhập dữ liệu có khoảng trắng đầu/cuối và xác nhận frontend/backend cùng chuẩn hóa.
5. Tải lại trang và kiểm tra dữ liệu vẫn được giữ.
6. Dùng số điện thoại của tài khoản khác và xác nhận lỗi `409 PHONE_ALREADY_EXISTS` gắn đúng trường.
7. Gửi `{}`, `null`, JSON sai, tên/số điện thoại sai và trường chỉ đọc; đối chiếu lỗi `400`.
8. Xóa token, xác nhận API trả `401` và UI chuyển đến trang đăng nhập.
9. Mở mục lịch sử đặt phòng trong hồ sơ, kiểm tra lọc, phân trang và drawer chi tiết.
10. Kiểm tra responsive trên desktop, tablet và điện thoại.

## Kết quả kiểm tra cục bộ

Đã kiểm tra ngày 2026-08-10; số lượng test được ghi nhận theo lần chạy CI gần nhất.

- Backend profile tests: `32` passed, `0` failed.
- Full backend Maven suite: `164` passed, `0` failed, `16` Testcontainers tests skipped because the Docker test environment was unavailable.
- Frontend tests: `23` passed, `0` failed.
- Backend Spotless: passed.
- Frontend Prettier, ESLint and production build: passed.
- API E2E verification: passed for register, login, unauthorized access, GET, PATCH, persisted reload, duplicate phone, `null` body and read-only field rejection.
- Chrome responsive verification: passed at desktop `1440x900` and mobile `390x844`.

PR vẫn cần được review và phê duyệt trước khi HAN-100 đáp ứng đầy đủ Definition of Done.

---

# 9. HAN-170 - View Today Arrivals QA

## TC-12 View Today Arrivals

### Phạm vi và quy tắc nghiệp vụ

Receptionist, Manager và Admin truy cập `/front-desk/arrivals/today` để xem các
booking có `checkInDate` bằng ngày hiện tại của khách sạn và trạng thái
`CONFIRMED`. Ngày hiện tại do backend tính theo `Asia/Ho_Chi_Minh`; frontend
không tự lọc theo đồng hồ trình duyệt.

`PENDING`, `CHECKED_IN`, `CHECKED_OUT`, `CANCELLED` và `EXPIRED` không xuất hiện.
Danh sách không bao gồm tìm kiếm, lọc trạng thái, phòng vật lý, gán phòng hoặc
thao tác check-in. API có thể trả `primaryGuest=null` cho dữ liệu cũ không hợp
lệ; UI phải hiển thị cảnh báo và không fallback sang người đặt booking.

### Đối chiếu tiêu chí chấp nhận

| Tiêu chí | Kiểm thử tự động | Kết quả mong đợi |
|---|---|---|
| Chỉ hiển thị khách dự kiến đến hôm nay | Repository và service test | Chỉ `checkInDate=today`, `status=CONFIRMED` |
| Ngày khách sạn chính xác | Service test tại ranh giới UTC/+07 | Repository nhận ngày `Asia/Ho_Chi_Minh` |
| Phân trang và thứ tự ổn định | Repository, service, controller và UI test | `page` zero-based, `size` 1–100, sort `createdAt` rồi `bookingCode` tăng dần |
| Kết quả rỗng | Service, controller và UI test | `200 OK`, `content=[]`, UI hiển thị empty state |
| Khách chính bị thiếu | Service, controller và UI test | API trả `null`, UI cảnh báo, trang không lỗi |
| Authentication | Security integration test | Anonymous/token sai trả `401 AUTH_UNAUTHORIZED` |
| Authorization | Security và route/menu test | Receptionist/Manager/Admin được phép; Housekeeper/Guest bị `403` |
| Validation | Service/controller test | Page/size sai giá trị hoặc sai kiểu trả `400 VALIDATION_FAILED` đúng field |
| UI feedback | Component test | Loading, error, retry, refresh và empty state hoạt động |
| Hạn chế dữ liệu nhạy cảm trên UI | Component test | `identityNumber` không xuất hiện trực tiếp trong bảng |

### Ma trận dữ liệu backend

| Fixture | Có trong kết quả |
|---|---|
| Hôm nay + `CONFIRMED` | Có |
| Hôm nay + `PENDING` | Không |
| Hôm nay + `CHECKED_IN` | Không |
| Hôm nay + `CHECKED_OUT` | Không |
| Hôm nay + `CANCELLED` | Không |
| Hôm nay + `EXPIRED` | Không |
| Ngày khác + `CONFIRMED` | Không |
| Hôm nay + `CONFIRMED`, thiếu primary guest | Có, `primaryGuest=null` |

Repository integration test phải chạy trên PostgreSQL thật qua Testcontainers để
kiểm tra PostgreSQL enum, Flyway schema, count query, entity graph và batch query
primary guest. Không thay test này bằng H2.

### Ma trận quyền

| Người dùng | API | Menu/route frontend |
|---|---|---|
| Anonymous | `401` | Chuyển vào authentication flow |
| Token sai/hết hạn | `401` | Session hết hạn được xử lý bởi API client |
| `RECEPTIONIST` | `200` | Hiển thị và truy cập được |
| `MANAGER` | `200` | Hiển thị và truy cập được |
| `ADMIN` | `200` | Hiển thị và truy cập được |
| `HOUSEKEEPER` | `403` | Ẩn menu, URL trực tiếp chuyển `/forbidden` |
| Guest/không có staff role | `403` | URL trực tiếp chuyển `/forbidden` |

### Kiểm thử tự động

Backend:

```bash
cd hotel-booking
./mvnw -Dtest=FrontDeskServiceImplTest test
./mvnw -Dtest=FrontDeskControllerTest,FrontDeskSecurityIntegrationTest test
./mvnw -Dtest=FrontDeskRepositoryIntegrationTest test
./mvnw spotless:check
./mvnw test
```

Frontend:

```bash
cd client
npm run test:run -- src/features/front-desk src/layouts/dashboard-layout.test.tsx src/routes/role-route.test.tsx
npm run test:run
npm run lint
npm run format:check
npm run build
```

Contract:

1. Parse `docs/openapi/front-desk/view-today-arrivals.yaml`.
2. Resolve toàn bộ `$ref` nội bộ.
3. Đối chiếu path, pagination, security, DTO và error response với controller.
4. Kiểm tra `/v3/api-docs` hoặc Swagger UI khi backend đang chạy.

### Kiểm thử thủ công frontend-backend

Điều kiện:

1. PostgreSQL hoạt động và Flyway đã chạy hết migration.
2. Có booking cho hôm nay ở từng trạng thái cần đối chiếu.
3. Có JWT Receptionist, Manager, Admin và Housekeeper; không ghi token thật vào Git.
4. Backend chạy tại `http://localhost:8080`, frontend tại `http://localhost:5173`.

Các bước:

1. Dùng `hotel-booking/http/today-arrivals.http` kiểm tra happy path, pagination,
   `401`, `403`, page/size sai giá trị và sai kiểu.
2. Đăng nhập Receptionist, mở menu **Khách đến hôm nay** và đối chiếu dữ liệu với DB.
3. Xác nhận `PENDING`, `CHECKED_IN` và booking khác ngày không xuất hiện.
4. Kiểm tra booking thiếu primary guest hiển thị **Thiếu thông tin khách**.
5. Kiểm tra empty state, nút **Làm mới**, lỗi kết nối và nút **Thử lại**.
6. Chuyển trang và xác nhận UI page 2 gọi API `page=1&size=20`.
7. Đăng nhập Manager và Admin, xác nhận cả hai truy cập được.
8. Đăng nhập Housekeeper, xác nhận menu bị ẩn và URL trực tiếp bị từ chối.
9. Kiểm tra desktop, tablet và mobile; bảng có thể cuộn ngang và không mất dữ liệu.
10. Xác nhận UI không hiện CCCD/identity number và không có nút check-in/gán phòng giả.

File hỗ trợ kiểm tra:

```text
hotel-booking/http/today-arrivals.http
docs/openapi/front-desk/view-today-arrivals.yaml
```

### Known limitations

- Search booking, Today Departures, gán phòng và check-in thuộc story khác.
- Danh sách không theo dõi khách đã `CHECKED_IN`.
- Browser E2E chưa được tự động hóa vì project chưa có Playwright configuration;
  checklist trình duyệt ở trên phải được thực hiện trước Sprint Review.
- PR vẫn cần được review và phê duyệt trước khi Definition of Done hoàn tất.

---

# 10. HAN-39 và HAN-40 - Housekeeping QA

## TC-13 View Rooms Requiring Cleaning và TC-14 Mark Room as Cleaned

### Phạm vi và quy tắc nghiệp vụ

Housekeeper truy cập `/housekeeping` để xem các phòng vật lý có trạng thái
`DIRTY` hoặc `CLEANING`. Backend là nguồn dữ liệu duy nhất; frontend không tự
suy luận hay tự cập nhật trạng thái phòng.

Luồng dọn phòng là `DIRTY -> CLEANING -> AVAILABLE`: Housekeeper bắt đầu dọn
phòng `DIRTY` và chỉ có thể hoàn tất khi phòng đang `CLEANING`. Mỗi mutation khóa
phòng ở backend. Sau khi complete thành công, phòng `AVAILABLE` không còn xuất
hiện trong danh sách phòng cần xử lý. Ghi chú phòng có thể `null` và UI phải hiển
thị trạng thái thay thế thay vì lỗi render.

### Đối chiếu tiêu chí chấp nhận

| Tiêu chí | Kiểm thử tự động | Kết quả mong đợi |
|---|---|---|
| Danh sách phòng cần xử lý | Repository, service và API integration test | Chỉ room `DIRTY`/`CLEANING`; `AVAILABLE`, `OCCUPIED`, `MAINTENANCE` không xuất hiện |
| Phân trang và thứ tự ổn định | Repository, service, controller và UI test | `page` zero-based, `size` 1–100, sort `floor` rồi `roomNumber` tăng dần |
| Hoàn tất dọn phòng | Service, controller và API integration test | `CLEANING -> AVAILABLE`, response trả status `AVAILABLE`, room biến mất khỏi danh sách xử lý |
| Chuyển trạng thái sai | Service và controller test | `DIRTY`, `AVAILABLE` hoặc trạng thái khác gọi complete trả `409 ROOM_CLEANING_COMPLETE_CONFLICT` |
| Phòng không tồn tại | Service và controller test | `404 ROOM_NOT_FOUND` |
| Concurrent complete | PostgreSQL/Testcontainers concurrency test | Hai request đồng thời chỉ một request thành công, trạng thái cuối là `AVAILABLE` |
| Kết quả rỗng | Service, controller và UI test | `200 OK`, `content=[]`, UI hiển thị empty state |
| Ghi chú nullable | Mapping, controller và UI test | API trả `note=null`, UI hiển thị **Không có ghi chú** |
| Authentication | Security integration test | Anonymous/token sai trả `401 AUTH_UNAUTHORIZED` |
| Authorization | Security, route và menu test | Chỉ `HOUSEKEEPER` được phép; role khác nhận `403` hoặc `/forbidden` |
| Validation | Controller test | `roomId` sai định dạng trả `400 VALIDATION_FAILED` |
| UI feedback | Component test | Loading button, success/error toast, refetch sau mutation, retry, refresh, thống kê và empty state hoạt động |
| Responsive | Build và kiểm thử thủ công | Bảng cuộn ngang trên mobile, không mất cột dữ liệu |

### Ma trận dữ liệu backend

| Fixture | Có trong kết quả |
|---|---|
| Room `DIRTY`, có note | Có, giữ nguyên note |
| Room `DIRTY`, note `null` | Có, `note=null` |
| Room `CLEANING` | Có, hiển thị **Đang dọn** và nút **Hoàn tất dọn** |
| Room `AVAILABLE` | Không |
| Room `OCCUPIED` | Không |
| Room `MAINTENANCE` | Không |

API integration test chạy controller → service → repository trên PostgreSQL thật
qua Testcontainers, gồm cả complete-cleaning và xác nhận room đã `AVAILABLE`
không còn trong danh sách cần xử lý. Repository integration test kiểm tra thêm
PostgreSQL enum, Flyway schema, pagination, sort và entity graph của `roomType`.
Không thay các test này bằng H2.

### Ma trận quyền

| Người dùng | API | Menu/route frontend |
|---|---|---|
| Anonymous | `401` | Chuyển vào authentication flow |
| Token sai/hết hạn | `401` | Session hết hạn được API client xử lý |
| `HOUSEKEEPER` | `200` | Hiển thị menu **Phòng cần dọn** và truy cập được |
| `RECEPTIONIST` | `403` | Ẩn menu, URL trực tiếp chuyển `/forbidden` |
| `MANAGER` | `403` | Ẩn menu, URL trực tiếp chuyển `/forbidden` |
| `ADMIN` | `403` | Ẩn menu, URL trực tiếp chuyển `/forbidden` |
| Guest/không có staff role | `403` | URL trực tiếp chuyển `/forbidden` |

Tài khoản có nhiều role vẫn được phép nếu danh sách role chứa `HOUSEKEEPER`.

### Kiểm thử tự động

Backend:

```bash
cd hotel-booking
./mvnw -Dtest=HousekeepingServiceImplTest test
./mvnw -Dtest=HousekeepingControllerTest,HousekeepingSecurityIntegrationTest test
./mvnw -Dtest=HousekeepingRepositoryIntegrationTest,HousekeepingApiIntegrationTest,HousekeepingConcurrencyIntegrationTest test
./mvnw spotless:check
./mvnw test
```

Frontend:

```bash
cd client
npm run test:run -- src/features/housekeeping src/layouts/dashboard-layout.test.tsx src/routes/role-route.test.tsx
npm run test:run
npm run lint
npm run format:check
npm run build
```

Contract:

1. Parse `docs/openapi/housekeeping/view-dirty-rooms.yaml`.
2. Resolve toàn bộ `$ref` nội bộ.
3. Đối chiếu path, security, DTO, response `AVAILABLE` và error response của
   `complete-cleaning` với controller.
4. Kiểm tra `/v3/api-docs` hoặc Swagger UI khi backend đang chạy.

### Chuẩn bị tài khoản Housekeeper local

Khuyến nghị đăng ký một tài khoản test riêng, sau đó chạy SQL sau trong pgAdmin.
Không thay role của tài khoản dùng cho production và không commit mật khẩu/token.

```sql
BEGIN;

INSERT INTO user_roles (user_id, role_id)
SELECT users.id, roles.id
FROM users
CROSS JOIN roles
WHERE lower(users.email) = lower('housekeeper.test@hanbay.local')
  AND roles.code = 'HOUSEKEEPER'
ON CONFLICT (user_id, role_id) DO NOTHING;

COMMIT;

SELECT users.email, roles.code
FROM users
JOIN user_roles ON user_roles.user_id = users.id
JOIN roles ON roles.id = user_roles.role_id
WHERE lower(users.email) = lower('housekeeper.test@hanbay.local');
```

Sau khi gán role, phải đăng xuất rồi đăng nhập lại vì danh sách role được ghi vào
JWT tại thời điểm đăng nhập. Token phát hành trước khi cập nhật database không có
claim `HOUSEKEEPER`.

### Chuẩn bị dữ liệu phòng cần dọn local

Chỉ sử dụng trên database local. Chọn phòng test trước khi cập nhật để tránh sửa
nhầm dữ liệu đang dùng cho demo khác.

```sql
SELECT id, room_number, floor, status, note
FROM rooms
ORDER BY floor, room_number;

UPDATE rooms
SET status = CASE room_number
        WHEN 'S101' THEN 'DIRTY'::room_status
        WHEN 'D301' THEN 'CLEANING'::room_status
        ELSE status
    END,
    note = 'QA HAN-40 - kiểm tra minibar và thay ga giường',
    updated_at = CURRENT_TIMESTAMP
WHERE room_number IN ('S101', 'D301');
```

### Kiểm thử thủ công frontend-backend

Điều kiện:

1. PostgreSQL hoạt động và Flyway đã chạy hết migration.
2. Có ít nhất một room `DIRTY`, một room `CLEANING` và một room `AVAILABLE`.
3. Có tài khoản `HOUSEKEEPER` và một tài khoản role khác để kiểm tra `403`.
4. Backend chạy tại `http://localhost:8080`, frontend tại `http://localhost:5173`.

Các bước:

1. Dùng `hotel-booking/http/dirty-rooms.http` kiểm tra list, happy path complete,
   `400`, `401`, `403`, `404` và `409`.
2. Đăng nhập Housekeeper và mở menu **Phòng cần dọn**.
3. Đối chiếu tổng phòng, số phòng, tầng, hạng phòng, status và note với database.
4. Với room `DIRTY`, chọn **Bắt đầu dọn** và xác nhận UI chuyển thành **Đang dọn**.
5. Với room `CLEANING`, chọn **Hoàn tất dọn**; xác nhận toast thành công, loading
   button, refetch danh sách và room không còn xuất hiện.
6. Gửi lại complete cho cùng room; xác nhận UI hiển thị lỗi từ
   `ROOM_CLEANING_COMPLETE_CONFLICT`.
7. Thực hiện cùng lúc hai complete requests cho một room `CLEANING`; chỉ một
   request thành công và status cuối trong database là `AVAILABLE`.
8. Xác nhận room `AVAILABLE` không xuất hiện.
9. Đặt note của một room thành `null`; UI phải hiện **Không có ghi chú**.
10. Chuyển toàn bộ room về trạng thái khác `DIRTY`/`CLEANING`; UI phải hiện empty state.
11. Kiểm tra nút **Làm mới**, lỗi kết nối và nút **Thử lại**.
12. Tạo hơn 20 room `DIRTY` hoặc giảm page size bằng API; xác nhận page UI thứ hai
   gọi API zero-based tương ứng.
13. Đăng nhập Receptionist/Manager/Admin; menu bị ẩn và URL trực tiếp chuyển
   `/forbidden`.
14. Kiểm tra desktop, tablet và mobile; bảng cuộn ngang nhưng không mất dữ liệu.

File hỗ trợ kiểm tra:

```text
hotel-booking/http/dirty-rooms.http
docs/openapi/housekeeping/view-dirty-rooms.yaml
```

### Known limitations

- Story chưa hỗ trợ phân công/nhận việc, SLA dọn phòng hoặc ghi nhận người hoàn
  tất dọn phòng.
- Thống kê số tầng và số phòng có note trên UI chỉ tính dữ liệu trang hiện tại;
  tổng phòng cần xử lý lấy từ `totalElements` của backend.
- Browser E2E chưa được tự động hóa; checklist thủ công phải hoàn thành trước
  Sprint Review.
- PR vẫn cần được review và phê duyệt trước khi HAN-40 đáp ứng Definition of Done.

---

# 11. HAN-216 đến HAN-220 - Manage Add-on Services QA

## Phạm vi

- Management API dành cho `MANAGER` và `ADMIN` tại `/api/v1/management/add-ons`.
- Service/repository logic cho create, update, activate/deactivate, search, filter, sort và pagination.
- Frontend route `/add-on-services`, dashboard menu theo role và API integration.
- UI list, loading, empty, error/retry, create/update form và confirm thay đổi trạng thái.
- Public add-on catalog và booking snapshot tiếp tục giữ nguyên hành vi hiện có.

## Đối chiếu tiêu chí

| Tiêu chí | Cách kiểm tra | Kết quả mong đợi |
|---|---|---|
| Manager/admin quản lý add-on | Security integration test và RoleRoute/menu test | Role hợp lệ truy cập được; role khác nhận `403` hoặc không thấy menu |
| Danh sách quản trị | Repository, API client và UI test | Search, active filter, sort và pagination gửi đúng contract |
| Tạo/cập nhật dịch vụ | Service/controller/API client test và kiểm tra form | Payload không chứa field read-only; code bị khóa khi edit |
| Không hard-delete | Controller/UI inspection | Chỉ có activate/deactivate với confirm rõ tác động |
| Validation và conflict | Controller/service test và form validation | Lỗi field hiển thị đúng; duplicate code gắn vào trường code |
| UI feedback | Component/manual test | Có loading, empty, error/retry và success/error message |
| Booking snapshot | Domain/booking test | Giá catalog mới không sửa add-on đã snapshot trong booking cũ |

## HAN-219 Local Verification Record

Đã kiểm tra ngày 2026-08-20:

- Frontend Vitest: `64` passed, `0` failed trên `17` test files.
- API client tests xác nhận list params, bỏ filter rỗng và đúng payload/path cho detail/create/update/status.
- Dashboard tests xác nhận manager/admin nhìn thấy menu; receptionist không nhìn thấy.
- Frontend ESLint: passed.
- Frontend Prettier check: passed.
- Frontend TypeScript và Vite production build: passed.
- `git diff --check`: passed.

## HAN-220 Automated Verification Record

Đã kiểm tra ngày 2026-08-20:

- Backend Spotless và full Maven suite: `305` tests, `0` failures, `0` errors; `20` Testcontainers tests skipped trong lần chạy sandbox do không truy cập Docker.
- Add-on repository integration test: `2` passed với PostgreSQL 17 Testcontainer; Flyway áp dụng thành công `21` migrations.
- Add-on management security integration test: `7` passed; anonymous nhận `401`, role không hợp lệ nhận `403`, manager/admin được phép và receptionist bị từ chối trên toàn bộ write endpoints.
- Frontend Vitest: `68` passed, `0` failed trên `18` test files.
- Test UI bổ sung xác nhận list data, create/edit modal, code bất biến khi edit, empty state, error/retry, search trim và active filter.
- Frontend Prettier, ESLint, TypeScript và Vite production build: passed.

Các cảnh báo `localStorage` và `getComputedStyle` trong Vitest đến từ giới hạn của môi trường jsdom; không làm test thất bại.

## HAN-220 Manual Frontend-Backend Checklist

Điều kiện:

1. PostgreSQL, backend và frontend đang chạy với dữ liệu manager/admin hợp lệ.
2. Đăng nhập bằng manager hoặc admin và mở `/add-on-services`.

Luồng cần xác nhận trước khi duyệt PR:

1. Tải danh sách, search, lọc active/inactive, sort và phân trang.
2. Tạo dịch vụ hợp lệ; kiểm tra success message và dữ liệu mới xuất hiện.
3. Thử code trùng, dữ liệu rỗng, tên quá dài và giá không hợp lệ; kiểm tra lỗi gắn đúng field.
4. Sửa name, description và price; xác nhận code không thể chỉnh sửa.
5. Deactivate rồi activate; kiểm tra confirm dialog, trạng thái mới và ảnh hưởng tới booking mới.
6. Dừng backend để kiểm tra error/retry rồi khởi động lại.
7. Đăng nhập receptionist/guest và xác nhận không thấy menu, không truy cập được route/API.
8. Kiểm tra desktop, tablet và mobile widths.

Automated verification đã hoàn tất. Manual frontend-backend checklist và PR review/approval vẫn cần thực hiện trước khi HAN-220 đáp ứng toàn bộ Definition of Done.

---

# 12. HAN-190 - Check out guest QA

## Phạm vi

- Backend API `PATCH /api/v1/bookings/{bookingId}/check-out` dành cho `RECEPTIONIST`,
  `MANAGER` và `ADMIN`.
- Business logic khóa booking/phòng, kiểm tra công nợ, chuyển booking sang `CHECKED_OUT`
  và chuyển phòng vật lý đã gán sang `DIRTY` trong cùng transaction.
- Frontend màn hình **Khách trả phòng** tại `/front-desk/departures/today`, bao gồm
  danh sách khách đang lưu trú, modal kiểm tra phòng, phí phát sinh/đền bù, tiền thu thêm,
  ghi chú tùy chọn và UI feedback.
- Contract trong `docs/openapi/booking/check-out-guest.yaml` và tóm tắt API trong
  `docs/06-api-specification.md`.
- Luồng liên quan tới housekeeping chỉ dừng ở việc room có trạng thái `DIRTY`; phân công
  dọn phòng hoặc chuyển room về `AVAILABLE` thuộc story housekeeping khác.

## Đối chiếu tiêu chí chấp nhận

| Tiêu chí | Cách kiểm tra | Kết quả mong đợi |
|---|---|---|
| Checkout thành công | Service/controller test và manual FE demo | Booking `CHECKED_IN`, có room `OCCUPIED`, đã thanh toán đủ được chuyển sang `CHECKED_OUT`; room chuyển `DIRTY` |
| Công nợ cuối phải bằng 0 | Service/controller test và form thu tiền tại quầy | Booking còn outstanding sau phí phát sinh và tiền thu thêm không checkout được; API trả `PAYMENT_REQUIRED`, FE hiển thị lý do thân thiện |
| Trạng thái booking hợp lệ | Service test | Chỉ `CHECKED_IN` được checkout; `CHECKED_OUT` trả `BOOKING_ALREADY_CHECKED_OUT`, trạng thái khác trả `BOOKING_NOT_CHECKED_IN` |
| Phòng hợp lệ | Service test | Booking thiếu phòng trả `ROOM_NOT_ASSIGNED`; room không `OCCUPIED` trả `ROOM_NOT_OCCUPIED` |
| Phân quyền | Security integration test và manual login | Anonymous nhận `401`; role không hợp lệ nhận `403`; `RECEPTIONIST`, `MANAGER`, `ADMIN` được phép |
| Request kiểm tra phòng | Controller/API/FE test | Body có thể bỏ trống; `note` được trim và tối đa 500 ký tự; phí phát sinh và tiền thu thêm không âm; có phí phát sinh thì bắt buộc có lý do |
| UI feedback | FE component test và manual demo | Loading, success message, error message, modal reset và refresh danh sách hoạt động đúng |
| Contract/docs | Review OpenAPI và API specification | Path, method, security, request, response và error code khớp implementation |

## Ma trận dữ liệu backend

| Fixture | Kết quả mong đợi |
|---|---|
| Booking `CHECKED_IN`, room `OCCUPIED`, `paid_amount = total_amount` | `200 OK`, booking `CHECKED_OUT`, room `DIRTY` |
| Booking `CHECKED_IN`, room `OCCUPIED`, `paid_amount < total_amount`, `collectedAmount` đủ phần thiếu | `200 OK`, `paid_amount` tăng, booking `CHECKED_OUT`, room `DIRTY` |
| Booking `CHECKED_IN`, room `OCCUPIED`, có `additionalChargeAmount` và `collectedAmount` đủ | `200 OK`, `total_amount` và `paid_amount` tăng, booking `CHECKED_OUT`, room `DIRTY` |
| Booking `CHECKED_IN`, room `OCCUPIED`, có `additionalChargeAmount` nhưng thiếu lý do | `400 VALIDATION_FAILED`, không gọi service |
| Booking `CHECKED_IN`, room `OCCUPIED`, còn outstanding sau điều chỉnh | `409 PAYMENT_REQUIRED`, không đổi booking/room |
| Booking `CHECKED_OUT` | `409 BOOKING_ALREADY_CHECKED_OUT` |
| Booking `CONFIRMED`, `PENDING`, `CANCELLED` hoặc `EXPIRED` | `409 BOOKING_NOT_CHECKED_IN` |
| Booking không có `assigned_room_id` | `409 ROOM_NOT_ASSIGNED` |
| Room đã gán không phải `OCCUPIED` | `409 ROOM_NOT_OCCUPIED` |
| Booking ID không tồn tại | `404 BOOKING_NOT_FOUND` |
| `note` dài hơn 500 ký tự | `400 VALIDATION_FAILED` |

## Ma trận quyền

| Người dùng | API | Frontend |
|---|---|---|
| Anonymous | `401 AUTH_UNAUTHORIZED` | Chuyển vào authentication flow |
| Guest/không có staff role | `403 AUTH_FORBIDDEN` | Không được truy cập chức năng lễ tân |
| `HOUSEKEEPER` | `403 AUTH_FORBIDDEN` | Không thực hiện được checkout |
| `RECEPTIONIST` | `200 OK` nếu dữ liệu hợp lệ | Thấy menu **Khách trả phòng** và thao tác được |
| `MANAGER` | `200 OK` nếu dữ liệu hợp lệ | Thao tác được theo quyền quản lý |
| `ADMIN` | `200 OK` nếu dữ liệu hợp lệ | Thao tác được |

## Kiểm thử tự động

Backend:

```bash
cd hotel-booking
./mvnw -Dtest=CheckOutServiceImplTest,CheckOutControllerTest,CheckOutSecurityIntegrationTest,TodayDepartureControllerTest,TodayDepartureServiceTest test
./mvnw spotless:check
./mvnw test
```

Frontend:

```bash
cd client
npm run test:run -- src/features/front-desk
npm run lint
npm run build
```

Contract:

1. Parse `docs/openapi/booking/check-out-guest.yaml`.
2. Đối chiếu path `PATCH /api/v1/bookings/{bookingId}/check-out`, bearer security,
   request body optional, response `CheckOutGuestResult` và error responses với controller.
3. Đối chiếu summary trong `docs/06-api-specification.md` với OpenAPI contract.

## Chuẩn bị dữ liệu demo local

Tài khoản lễ tân:

```sql
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.code = 'RECEPTIONIST'
WHERE lower(u.email) = 'reception@gmail.com'
ON CONFLICT DO NOTHING;
```

Reset booking để demo lại:

```sql
UPDATE bookings
SET status = 'CHECKED_IN',
    check_out_date = CURRENT_DATE,
    paid_amount = total_amount
WHERE booking_code = 'BK20260824000601';

UPDATE rooms
SET status = 'OCCUPIED'
WHERE id = (
  SELECT assigned_room_id
  FROM bookings
  WHERE booking_code = 'BK20260824000601'
);
```

Kiểm tra precondition:

```sql
SELECT b.booking_code,
       b.status AS booking_status,
       b.check_out_date,
       b.total_amount,
       b.paid_amount,
       b.total_amount - b.paid_amount AS outstanding_amount,
       r.room_number,
       r.status AS room_status
FROM bookings b
JOIN rooms r ON r.id = b.assigned_room_id
WHERE b.booking_code = 'BK20260824000601';
```

Điều kiện đạt trước khi demo: `booking_status = CHECKED_IN`,
`outstanding_amount = 0` và `room_status = OCCUPIED`.

## Kiểm thử thủ công frontend-backend

1. Chạy backend ở port `8080` và frontend Vite ở `http://localhost:5173`.
2. Đăng nhập bằng tài khoản `reception@gmail.com` đã có role `RECEPTIONIST`.
3. Mở `/front-desk/departures/today` và xác nhận booking demo xuất hiện trong danh sách.
4. Bấm **Trả phòng**, kiểm tra modal hiển thị đúng mã booking, tên khách, tổng tiền,
   đã thanh toán, còn phải thu, phí phát sinh, tổng sau kiểm tra và còn phải thu sau điều chỉnh.
5. Nhập phí phát sinh/đền bù nếu có, lý do phát sinh và số tiền thu thêm tại quầy; nếu có phí
   phát sinh mà bỏ trống lý do thì UI phải chặn submit.
6. Nhập ghi chú tùy chọn dưới 500 ký tự hoặc để trống.
7. Bấm **Xác nhận trả phòng**.
8. Xác nhận FE hiển thị thông báo thành công, đóng modal và refresh danh sách.
9. Kiểm tra DB sau checkout:

```sql
SELECT b.booking_code,
       b.status AS booking_status,
       r.room_number,
       r.status AS room_status
FROM bookings b
JOIN rooms r ON r.id = b.assigned_room_id
WHERE b.booking_code = 'BK20260824000601';
```

Kết quả đạt: `booking_status = CHECKED_OUT` và `room_status = DIRTY`.

## Known limitations

- Browser E2E chưa được tự động hóa; manual frontend-backend checklist phải được thực hiện
  trước Sprint Review.
- Story này không tạo màn hình housekeeping task riêng; tiêu chí bàn giao cho housekeeping là
  room chuyển sang `DIRTY` để API danh sách phòng cần dọn lấy được.
- PR vẫn cần được review và phê duyệt trước khi HAN-190 đáp ứng toàn bộ Definition of Done.

## HAN-238 - Manager dashboard charts and cards

### Phạm vi

- Dashboard dành cho `MANAGER` và `ADMIN` hiển thị bốn KPI: doanh thu thuần,
  công suất phòng, tổng booking và tỷ lệ hủy.
- Biểu đồ thể hiện xu hướng doanh thu, xu hướng công suất và cơ cấu trạng thái
  booking từ contract `GET /api/v1/reports/dashboard`.
- Khoảng thời gian mặc định là từ đầu tháng hiện tại đến đầu ngày kế tiếp;
  bộ lọc ngày tương tác thuộc HAN-239.
- UI phải xử lý đầy đủ loading, lỗi có thể thử lại, dữ liệu rỗng và responsive.
- Receptionist và Housekeeper không thấy menu Tổng quan và không được truy cập
  route dashboard.

### Kiểm thử tự động

```bash
cd client
npm run test:run -- src/features/dashboard src/layouts/dashboard-layout.test.tsx src/routes/role-route.test.tsx
npm run test:run
npm run lint
npm run format:check
npm run build
```

Test dashboard bao phủ việc ánh xạ response API, hiển thị KPI và biểu đồ, các
trạng thái loading/error/empty, hành động thử lại và phân quyền menu/route.

### Kiểm thử thủ công frontend-backend

1. Đăng nhập Manager hoặc Admin và mở `/dashboard`.
2. Đối chiếu bốn KPI và dữ liệu từng ngày trên biểu đồ với response API.
3. Xác nhận tiền tệ hiển thị theo VND, phần trăm nằm trong khoảng hợp lệ và ngày
   kết thúc được hiểu theo quy ước độc quyền `[from, to)`.
4. Kiểm tra nút **Làm mới**, lỗi kết nối, retry và trường hợp không có dữ liệu.
5. Đăng nhập Receptionist/Housekeeper, xác nhận menu Tổng quan bị ẩn và URL trực
   tiếp chuyển tới trang từ chối truy cập.
6. Kiểm tra bố cục trên desktop, tablet và mobile.

## HAN-239 - Dashboard date range filters

- Người dùng chọn **Từ ngày** và **Đến ngày** theo khoảng bao gồm cả hai đầu mút;
  frontend chuyển ngày kết thúc sang biên độc quyền trước khi gọi report API.
- Frontend chặn ngày kết thúc trước ngày bắt đầu, thiếu ngày và khoảng vượt quá
  giới hạn 366 ngày của backend.
- Nút **Tháng hiện tại** khôi phục khoảng mặc định; thay đổi nháp không gọi API
  cho đến khi người dùng chọn **Áp dụng**.
- Kiểm thử tự động của dashboard xác nhận query params, phần ngày hiển thị và
  các lỗi validation. Chạy lại các lệnh frontend trong mục HAN-238.

## HAN-240 - Verify dashboard numbers against database

`DashboardMetricRepositoryIntegrationTest.dashboardResponseMatchesKnownDatabaseFacts`
tạo fixture xác định trong PostgreSQL Testcontainers, gọi report service thật và
đối chiếu revenue, occupancy, booking summary cùng toàn bộ daily trend. Chi tiết
dataset và kết quả kỳ vọng nằm trong `docs/18-manager-dashboard-metrics.md`.

```bash
cd hotel-booking
./mvnw -Dtest=DashboardMetricRepositoryIntegrationTest test
./mvnw spotless:check
```

Điều kiện đạt là 4 test pass và `Skipped: 0`. Test có transaction rollback nên
không để lại dữ liệu QA trong database.
# Unified AI Chat and RAG validation

Contract tests xác nhận `/api/v1/ai/chat/stream` tồn tại và hai endpoint conversational cũ trả 404.
Unified orchestration tests bao phủ policy tool, tám read-only tools, ownership, no-evidence, prompt
injection marker, citation metadata, safe error và rate-limit integration. Security phải từ chối
anonymous chat; room-maintenance vẫn Manager-only và không mutate room trong bước interpret.
Frontend tests bao phủ source event, provider error, malformed/empty/partial SSE và action whitelist.

`PolicyPgVectorIntegrationTest` dùng Testcontainers `pgvector/pgvector:pg17` và deterministic
`TEST EMBEDDING` 1536 chiều để kiểm tra Flyway, extension, schema dimension, insert/upsert, cosine
search, topK/threshold, metadata và repeated ingestion. Test này không được báo cáo là xác minh
production embedding. Real-embedding verification cần `OPENAI_API_KEY` và được báo riêng.
# Stay modification coverage

Cover nightly-snapshot calculation, the 20/80 split, refund destination validation, cancellation-policy isolation, room preservation, availability, outstanding balance, ownership/RBAC, duplicate commands, and checkout races. Report PostgreSQL concurrency separately and never mark skipped Testcontainers tests as passed.

### Room maintenance regression

- Flyway: migrate database mới và nâng cấp từ develop version `202609051400` lên `202609081200`.
- Mỗi phòng chỉ có một phiếu mở; phiếu hoàn tất cho phép tạo lần bảo trì mới.
- API bảo trì trực tiếp không thể đưa phòng có phiếu mở về AVAILABLE.
- Transaction lỗi rollback cả phiếu và trạng thái phòng.
- UI: tải phòng vượt trang đầu 100 kết quả, chỉ chọn phòng trống, lỗi validation tiếng Việt, retry tải phòng và ngăn đóng khi đang tạo.
- Route `/maintenance` và alias `/rooms/maintenance` chỉ dành cho MANAGER.

### Checkout inspection settlement regression

- Thu đúng công nợ sau cộng phí đền bù tạo chứng từ tiền mặt và audit người thu.
- Từ chối thu vượt công nợ, số tiền vượt giới hạn và tràn tổng tiền.
- PostgreSQL: rollback đồng thời chứng từ, số dư, booking và phòng khi transaction lỗi.
- PostgreSQL: hai checkout đồng thời chỉ có một lần thu tiền.
- UI: không tự điền tiền đã thu, cập nhật số dư khi modal đang mở, không cho thu vượt nợ.
- PR không thêm hoặc sửa Flyway migration; dùng schema hiện tại của develop.
