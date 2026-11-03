# 02. Business Requirements Document (BRD)

# HANBAY HOTEL
## Business Requirements Document

---

# 1. Business Background

HANBAY HOTEL được xây dựng nhằm số hóa toàn bộ quy trình vận hành của một khách sạn, từ đặt phòng trực tuyến đến quản lý vận hành nội bộ.

Hệ thống không chỉ phục vụ khách hàng đặt phòng mà còn hỗ trợ lễ tân, buồng phòng và quản lý trong các nghiệp vụ hằng ngày.

---

# 2. Business Goals

- Tăng tỷ lệ đặt phòng trực tuyến.
- Giảm overbooking.
- Rút ngắn thời gian Check-in / Check-out.
- Chuẩn hóa quy trình vận hành khách sạn.
- Tăng doanh thu thông qua Dynamic Pricing.
- Cải thiện trải nghiệm khách hàng.

---

# 3. Stakeholders

| Stakeholder | Mục tiêu |
|-------------|----------|
| Guest | Tìm kiếm và đặt phòng nhanh chóng |
| Receptionist | Quản lý khách đến/đi, gán phòng |
| Housekeeping | Theo dõi và cập nhật trạng thái phòng |
| Manager | Quản lý vận hành và doanh thu |
| System Administrator | Quản trị hệ thống |

---

# 4. Business Processes

## BP-01 Search Room

Khách hàng:

- Chọn ngày Check-in / Check-out
- Chọn số lượng khách
- Xem danh sách hạng phòng còn trống
- Xem giá theo từng khoảng thời gian

---

## BP-02 Booking

- Chọn hạng phòng
- Nhập thông tin khách
- Chọn dịch vụ bổ sung
- Tạo Booking trạng thái PENDING
- Thanh toán
- Booking chuyển CONFIRMED

---

## BP-03 Check-in

Lễ tân:

- Tìm Booking
- Chọn phòng vật lý AVAILABLE
- Gán phòng
- Khách nhận phòng

---

## BP-04 Check-out

- Tính chi phí phát sinh
- Hoàn tất thanh toán
- Chuyển trạng thái phòng sang DIRTY

---

## BP-05 Housekeeping

- Xem danh sách phòng DIRTY
- Dọn phòng
- Chuyển phòng sang AVAILABLE

---

## BP-06 Dynamic Pricing

Quản lý:

- Thiết lập giá cơ bản
- Thiết lập phụ thu cuối tuần
- Thiết lập giá ngày lễ

Hệ thống tính giá theo từng đêm lưu trú.

---

## BP-07 Manage Add-on Services

Manager hoặc system administrator:

- Xem, tìm kiếm và lọc danh mục dịch vụ bổ sung.
- Tạo dịch vụ mới với mã nghiệp vụ duy nhất và giá hiện hành.
- Cập nhật thông tin hiển thị và giá cho các booking được tạo trong tương lai.
- Ngừng kích hoạt dịch vụ không còn cung cấp thay vì xóa dữ liệu đã được booking tham chiếu.

Khách chỉ nhìn thấy và lựa chọn dịch vụ đang hoạt động. Giá và số lượng đã ghi nhận trong booking là dữ liệu snapshot, không thay đổi khi danh mục được cập nhật sau đó.

---

# 5. Business Rules

BR-01 Không cho phép bán vượt số lượng phòng.

BR-02 Booking PENDING giữ phòng tối đa 15 phút.

BR-03 Sau 15 phút chưa thanh toán, Booking tự động chuyển sang EXPIRED.

BR-04 Chỉ phòng AVAILABLE mới được gán cho khách.

BR-05 Sau Check-out, phòng chuyển sang DIRTY.

BR-06 Sau khi Housekeeping hoàn tất, phòng chuyển AVAILABLE.

BR-07 Giá phòng được tính theo từng đêm.

BR-08 Chỉ dịch vụ bổ sung đang hoạt động mới được chọn cho booking mới.

BR-09 Thay đổi danh mục dịch vụ bổ sung không làm thay đổi giá và số lượng đã snapshot trong booking.

---

# 6. Core Business Problems

## Availability

Kiểm tra phòng trống theo khoảng thời gian bằng Overlapping Booking.

## Overbooking

Ngăn nhiều khách đặt cùng một phòng tại cùng thời điểm.

## Payment Timeout

Giữ chỗ tạm thời và tự động hủy khi quá hạn thanh toán.

## Room Operation

Đảm bảo phòng chỉ được tái sử dụng sau khi hoàn tất dọn phòng.

---

# 7. Success Criteria

- Khách tìm và đặt phòng thành công.
- Không xảy ra overbooking.
- Quy trình Check-in / Check-out hoạt động trơn tru.
- Housekeeping cập nhật trạng thái chính xác.
- Dynamic Pricing tính đúng giá.
- Hệ thống đáp ứng đầy đủ quy trình vận hành khách sạn.

---

# 8. Out of Scope (MVP)

- Quản lý nhiều khách sạn.
- Mobile Application.
- OTA Integration (Booking.com, Agoda...).
- Loyalty Program.
- Revenue Forecasting.
## BP-14 Early checkout, refund and extend stay

- Early checkout applies only to a `CHECKED_IN` booking. Unused amount is the sum of persisted `booking_nights` whose `stay_date >= actualCheckOutDate`.
- The early-checkout penalty is 20% of unused amount; the refund is 80%. This rule does not apply to cancellation.
- A bank-transfer destination is required whenever cancellation or early checkout produces a refund.
- Extension retains an assigned physical room or is rejected; it never moves the guest automatically. An unassigned booking remains unassigned until check-in.
- Added nights increase outstanding balance; checkout stays blocked while that balance is positive.
