# Hướng dẫn đóng góp

Tài liệu này mô tả quy trình đóng góp code cho HANBAY HOTEL. Mọi thay đổi nên gắn với một Jira issue và được đưa vào `develop` thông qua pull request (PR).

## 1. Yêu cầu môi trường

- Git.
- JDK 17.
- Docker và Docker Compose để chạy PostgreSQL.
- Không cần cài Maven riêng; dự án đã có Maven Wrapper.

Kiểm tra môi trường:

```bash
java -version
docker compose version
./mvnw --version
```

Trên Windows, thay `./mvnw` bằng `mvnw.cmd` trong các lệnh bên dưới.

## 2. Cách tạo branch và Bắt đầu Task (Jira Sync)

Luôn tạo branch mới từ phiên bản mới nhất của `develop`:

```bash
git switch develop
git pull --rebase origin develop
git switch -c feature/HAN-123-add-room-search
```

Kích hoạt Jira Smart Commits: Ngay sau khi tạo branch, bạn cần tạo một commit rỗng đầu tiên và đẩy lên Remote để Jira tự động nhận diện nhánh và cập nhật trạng thái thẻ:

```bash
git commit --allow-empty -m "HAN-123: Initialize task"
git push -u origin feature/HAN-123-add-room-search
```

Không commit trực tiếp vào `develop` hoặc `main`. Mỗi branch chỉ nên xử lý một issue hoặc một thay đổi có phạm vi rõ ràng.

## 3. Cách đặt tên branch

Định dạng:

```text
<type>/<jira-key>-<short-description>
```

Quy ước:

- Dùng chữ thường cho phần mô tả.
- Dùng dấu gạch ngang (`-`) để phân tách từ.
- Mô tả ngắn gọn, thể hiện mục tiêu thay đổi.
- Luôn có Jira key nếu thay đổi thuộc một issue.

Các `type` được sử dụng:

| Type | Khi sử dụng | Ví dụ |
| --- | --- | --- |
| `feature` | Thêm chức năng | `feature/HAN-123-add-room-search` |
| `fix` | Sửa lỗi | `fix/HAN-245-prevent-double-booking` |
| `refactor` | Cải tổ code, không đổi hành vi | `refactor/HAN-310-simplify-price-calculation` |
| `test` | Thêm hoặc sửa test | `test/HAN-188-add-booking-service-tests` |
| `docs` | Chỉ thay đổi tài liệu | `docs/HAN-401-update-api-guide` |
| `chore` | Cấu hình, dependency hoặc công việc bảo trì | `chore/HAN-220-upgrade-postgresql-driver` |
| `hotfix` | Sửa lỗi production khẩn cấp | `hotfix/HAN-999-fix-payment-timeout` |

## 4. Cách viết commit và Quản lý lịch sử (Amend)

Dự án ưu tiên lịch sử Git tuyến tính và sạch sẽ (1 branch = 1 commit). Thay vì tạo nhiều commit lắt nhắt, bạn sẽ gộp chung code mới vào commit khởi tạo ban đầu.

Khi cần lưu code, thực hiện:

```bash
git add .
git commit --amend --no-edit
git push origin <tên-branch> --force
```

Nếu bạn cần sửa đổi thông điệp commit (thường làm trước khi tạo PR), sử dụng Conventional Commits với Jira key:

```text
<type>(<scope>): <jira-key> <short-description>
```

Ví dụ:

```text
feat(booking): HAN-123 add room availability validation
fix(payment): HAN-245 handle duplicated callback
test(pricing): HAN-188 cover seasonal price calculation
docs(api): HAN-401 update booking response examples
```

Các commit nên:

- Dùng câu mệnh lệnh, phần mô tả viết thường và không kết thúc bằng dấu chấm.
- Có phạm vi (`scope`) là module bị ảnh hưởng, ví dụ `booking`, `room`, `payment`, `security`.
- Nhỏ, độc lập và luôn ở trạng thái build được.
- Không chứa secret, credential, file IDE hoặc thay đổi không liên quan.
- Giải thích lý do và ảnh hưởng trong commit body nếu thay đổi không hiển nhiên.

Các type commit thông dụng là `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `build` và `ci`.

## 5. Cách chạy project (Monorepo & Docker)

Dự án hiện đang vận hành trong môi trường Monorepo và sử dụng tính năng Spring Boot Docker Compose support. File cấu hình Docker docker-compose.yml (stack hanbay-hotel) và file .env được đặt ở thư mục gốc.

Nhờ tính năng này, Spring Boot sẽ tự động đọc cấu hình docker-compose.yml, khởi tạo PostgreSQL và chạy Flyway migration mỗi khi ứng dụng khởi động. Team không cần phải khởi chạy Docker Compose thủ công trước.

### Thiết lập Run/Debug Configuration (Quản lý nhiều Profiles)

Để khởi động ứng dụng và dễ dàng chuyển đổi giữa các môi trường (ví dụ: dev, local, test), team thực hiện thiết lập trực tiếp trên IDE:

1. Trên thanh công cụ (Toolbar) ở góc trên bên phải `IntelliJ`, click vào dropdown chọn cấu hình chạy và chọn `Edit Configurations....`
2. Nhấn nút `+ (Add New Configuration)` góc trên bên trái và chọn Spring Boot.
3. Cấu hình các thông số cơ bản:
   - Name: Đặt tên cấu hình theo profile (ví dụ: HotelBooking - DEV).
   - Main class: Chỉ định class com.hanbay.hotel.HanbayHotelApplication.
   - Active profiles: Điền tên profile bạn muốn kích hoạt (ví dụ: dev).
4. Nhấn Apply và OK.
5. Bây giờ, bạn chỉ cần chọn cấu hình này và nhấn nút Run (Shift + F10) hoặc Debug (Shift + F9). IDE và Spring Boot sẽ tự động lo toàn bộ vòng đời của các Docker container đi kèm.

Mặc định API sẽ chạy tại http://localhost:8080 và health check tại http://localhost:8080/actuator/health.

### Chạy với cơ sở dữ liệu có sẵn (Sử dụng Environment Variables)

Nếu bạn không muốn sử dụng các container mặc định từ Docker Compose mà muốn trỏ ứng dụng tới một database có sẵn bên ngoài thông qua biến môi trường:

1. Mở lại `Edit Configurations...` và chọn cấu hình Spring Boot của bạn.
2. Tìm đến trường `Environment variables` (nếu không hiển thị sẵn, hãy click vào `Modify options` > tích chọn `Environment variables`).
3. Click vào icon thư mục ở cuối ô nhập liệu và thêm các biến môi trường sau để ghi đè cấu hình mặc định:
   - DB_URL = jdbc:postgresql://localhost:5432/hanbay_hotel
   - DB_USERNAME = postgres
   - DB_PASSWORD = postgres
4. Nhấn `OK` để lưu lại. Khi chạy cấu hình này, Spring Boot sẽ ưu tiên sử dụng thông tin kết nối từ biến môi trường.

Lưu ý bảo mật:
Tuyệt đối không commit file `.env`, mật khẩu thật, hoặc file cấu hình IDE (`.idea/runConfigurations/`) có chứa credentials tĩnh lên repository của dự án.

## 6. Cách chạy test

Format Java code bằng Palantir Java Format:

```bash
./mvnw spotless:apply
```

Chỉ kiểm tra format mà không thay đổi file:

```bash
./mvnw spotless:check
```

Chạy toàn bộ test:

```bash
./mvnw test
```

Chạy một test class:

```bash
./mvnw -Dtest=BookingServiceTest test
```

Chạy một test method:

```bash
./mvnw -Dtest=BookingServiceTest#shouldRejectOverlappingBooking test
```

Chạy đầy đủ các bước kiểm tra giống CI trước khi tạo PR:

```bash
./mvnw --batch-mode clean verify
```

Test phải ổn định, độc lập với thứ tự chạy và không phụ thuộc dữ liệu còn sót lại từ môi trường local.

## 7. Cách thêm Flyway migration

Mọi thay đổi schema hoặc dữ liệu cần triển khai cùng ứng dụng phải có migration mới trong:

```text
src/main/resources/db/migration/
```

Đặt tên file theo định dạng:

```text
V<yyyyMMddHHmm>__<description>.sql
```

Ví dụ:

```text
V202607151030__create_rooms_table.sql
V202607151115__add_status_to_bookings.sql
```

Quy trình:

1. Đồng bộ `develop` trước khi chọn version để tránh trùng migration.
2. Tạo file SQL mới trong `src/main/resources/db/migration/`.
3. Viết migration tương thích với PostgreSQL và đặt tên object rõ nghĩa.
4. Chạy project hoặc integration test trên database sạch để kiểm tra toàn bộ chuỗi migration.
5. Kiểm tra cả trường hợp nâng cấp từ schema hiện tại nếu migration thay đổi dữ liệu đang có.

Không sửa, đổi tên hoặc xóa migration đã được merge/chạy ở môi trường dùng chung. Nếu cần sửa schema, hãy tạo migration mới. Với thay đổi có rủi ro, mô tả kế hoạch rollback hoặc khôi phục dữ liệu trong PR.

## 8. Cách tạo pull request

Trước khi tạo PR:

```bash
git fetch origin
git rebase origin/develop
./mvnw --batch-mode clean verify
git push -u origin feature/HAN-123-add-room-search
```

Sau đó tạo PR với base branch là `develop` và:

- Tiêu đề theo định dạng `[HAN-123] Add room search`.
- Điền đầy đủ `.github/pull_request_template.md`.
- Gắn link Jira issue và mô tả mục tiêu, thay đổi chính, phạm vi ảnh hưởng.
- Nêu rõ thay đổi database, API, cấu hình và hướng dẫn kiểm thử.
- Đính kèm request/response mẫu hoặc ảnh nếu giúp reviewer xác minh thay đổi.
- Chỉ định ít nhất một reviewer phù hợp.
- Đảm bảo PR có phạm vi nhỏ, không trộn refactor không liên quan.
- Chờ CI thành công và xử lý toàn bộ review comment trước khi merge.

Nếu `develop` thay đổi trong lúc review, cập nhật branch và chạy lại test. Không force-push sau khi review nếu không thật sự cần thiết; nếu có, phải báo cho reviewer.

## 9. Definition of Done

Một task chỉ được xem là **Done** khi:

1. Code đúng acceptance criteria.
2. Có validation cần thiết.
3. Có xử lý lỗi.
4. Có test cho business logic quan trọng.
5. Có Flyway migration nếu thay đổi database.
6. Swagger/OpenAPI được cập nhật.
7. CI chạy thành công.
8. Có ít nhất một người review.
9. Không còn comment review chưa xử lý.
10. Merge vào `develop` thành công.

## 10. Code review checklist

### Tác giả PR

- [ ] PR chỉ giải quyết đúng phạm vi của Jira issue và đáp ứng acceptance criteria.
- [ ] Đã tự review diff; không còn debug code, TODO vô chủ đích hoặc file không liên quan.
- [ ] Tên class, method, biến, endpoint và database object rõ ràng, nhất quán.
- [ ] Thiết kế tuân theo cấu trúc module hiện có; controller, service và repository đúng trách nhiệm.
- [ ] Input được validation; lỗi được xử lý và trả về response phù hợp.
- [ ] Không log dữ liệu nhạy cảm và không commit secret.
- [ ] Business logic quan trọng có unit test; luồng tích hợp quan trọng có integration test khi cần.
- [ ] Đã kiểm tra happy path, boundary case và failure path.
- [ ] Có Flyway migration nếu database thay đổi và migration chạy được trên database sạch.
- [ ] Swagger/OpenAPI, tài liệu và ví dụ request/response đã được cập nhật khi API thay đổi.
- [ ] Không có breaking change ngoài dự kiến; breaking change đã được mô tả rõ nếu bắt buộc.
- [ ] `./mvnw --batch-mode clean verify` chạy thành công ở local.
- [ ] PR template đã được điền đầy đủ và CI thành công.

### Reviewer

- [ ] Thay đổi đúng acceptance criteria và không vượt phạm vi issue.
- [ ] Logic đúng, dễ đọc, không trùng lặp không cần thiết và không tạo độ phức tạp quá mức.
- [ ] Phân quyền, validation, xử lý lỗi, transaction và concurrency được xem xét khi có liên quan.
- [ ] Không có lỗ hổng bảo mật, lộ secret hoặc dữ liệu nhạy cảm trong code/log/response.
- [ ] API và database giữ tương thích ngược hoặc có kế hoạch chuyển đổi rõ ràng.
- [ ] Migration an toàn với dữ liệu hiện có, có thứ tự đúng và không chỉnh sửa migration đã phát hành.
- [ ] Test có giá trị, bao phủ rủi ro chính và kiểm tra hành vi thay vì chi tiết triển khai.
- [ ] Swagger/OpenAPI và tài liệu phản ánh đúng hành vi mới.
- [ ] Hiệu năng và số lượng database query hợp lý đối với luồng bị ảnh hưởng.
- [ ] Mọi comment bắt buộc đã được xử lý trước khi approve.

Chỉ approve khi reviewer đã hiểu thay đổi và có đủ bằng chứng để tin rằng thay đổi an toàn để merge.
