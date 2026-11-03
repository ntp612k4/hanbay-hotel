# Dữ liệu demo trả phòng hôm nay

`seed-today-departures.sql` tạo hai booking mẫu nhận phòng hôm qua và trả phòng hôm nay theo múi giờ `Asia/Ho_Chi_Minh`:

| Khách mẫu | Hạng phòng | Trạng thái |
| --- | --- | --- |
| Nguyễn Minh An (Demo) | Superior | Đang lưu trú, đã thanh toán đủ |
| Trần Ngọc Linh (Demo) | Junior Suite | Đang lưu trú, đã thanh toán đủ |

Script chọn phòng vật lý `AVAILABLE`, kiểm tra tồn và chuyển phòng được gán sang `OCCUPIED`. Dữ liệu có khách chính, giá snapshot một đêm, payment, transaction mô phỏng và lịch sử trạng thái để dùng cho demo checkout. Giá mẫu lấy giá cơ bản hiện tại của hạng phòng. Không có giao dịch ngân hàng thực tế.

Chạy từ thư mục gốc repository bằng PowerShell:

```powershell
$OutputEncoding = [System.Text.UTF8Encoding]::new($false)
Get-Content -LiteralPath '.\hotel-booking\scripts\seed-today-departures.sql' -Raw -Encoding UTF8 |
  docker compose exec -T postgres psql -U hotelbooking_user -d hotelbooking_db -v ON_ERROR_STOP=1 -f -
```

Hoặc mở file SQL trong pgAdmin Query Tool, kết nối database `hotelbooking_db` rồi chạy toàn bộ file.

Mã booking có dạng `DEMO-OUT-YYYYMMDD-01` và `DEMO-OUT-YYYYMMDD-02`. Chạy lại trong cùng ngày sẽ bỏ qua các mã đã tồn tại; không tạo trùng và không đặt lại booking đã checkout. Chạy vào ngày khác sẽ tạo hai booking mới cho ngày đó.

Sau khi chạy, mở **Khách trả phòng** và bấm **Làm mới**. Hai booking mới đủ điều kiện checkout thường; để phí phát sinh và tiền thu thêm bằng 0 nếu muốn demo ngay. Checkout thành công chuyển booking sang `CHECKED_OUT` và phòng sang `DIRTY`.
