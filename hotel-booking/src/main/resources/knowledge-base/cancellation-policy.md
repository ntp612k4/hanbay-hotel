---
documentId: HANBAY-CANCELLATION-POLICY
title: Cancellation Policy
category: CANCELLATION
source: hotel-booking/src/main/resources/knowledge-base/cancellation-policy.md
version: 1.1
hotel: HANBAY HOTEL
language: vi
---

# HANBAY HOTEL — Chính sách hủy đặt phòng

## 1. Mục đích

Mô tả trạng thái được hủy và mức refund đã được backend xác nhận.

## 2. Phạm vi

Áp dụng cho booking `PENDING` hoặc `CONFIRMED`. Mốc thời gian dùng
`Asia/Ho_Chi_Minh` và giờ check-in cấu hình mặc định 14:00.

## 3. Quy định

### 3.1 Booking PENDING

Booking `PENDING` có thể hủy. Refund là `NOT_REQUIRED` vì policy không coi payment chưa
hoàn tất là khoản cần hoàn theo cancellation flow.
Nếu booking `PENDING` đã nhận một phần tiền, nhánh hủy này không tự tạo khoản hoàn cho số
đã nhận dở. Cần liên hệ nhân viên đối soát, không thông báo số tiền đó đã tự động được hoàn.

### 3.2 Booking CONFIRMED

- Còn ít nhất 48 giờ trước check-in: refund 100% nếu payment `SUCCESS`.
- Còn từ 24 giờ đến dưới 48 giờ: refund 50% nếu payment `SUCCESS`.
- Còn dưới 24 giờ: không cho phép hủy.

Nếu payment không ở `SUCCESS`, cancellation được phép trong cửa sổ tương ứng nhưng không
tạo refund. Refund của payment thành công có trạng thái ban đầu `PENDING`.

Code tính số tiền hoàn bằng phần trăm trên `booking.totalAmount`, làm tròn xuống VND;
không trực tiếp nhân `paidAmount`. Với booking đã gia hạn hoặc có nhiều khoản thanh toán/
hoàn tiền, không tự suy luận số hoàn từ tổng tiền đã trả: dùng kết quả kiểm tra điều kiện
hủy của booking cụ thể và liên hệ nhân viên đối soát nếu số tiền không khớp thực nhận.

Khi policy tạo refund, guest phải cung cấp destination `BANK_TRANSFER`: tên ngân hàng, tên
chủ tài khoản và số tài khoản. Không yêu cầu thông tin này khi refund là `NOT_REQUIRED`.
Backend tự xác định refund amount; client không được gửi amount. Quy tắc penalty 20% của
early checkout không áp dụng cho cancellation.

Refund `PENDING` chưa có nghĩa tiền đã về tài khoản. Quy trình hiện tại cho `MANAGER` xác
nhận sau đối soát bằng mã tham chiếu ngân hàng, không tự chuyển tiền ra hoặc đợi callback refund.

### 3.3 Trạng thái khác

Booking ngoài `PENDING` và `CONFIRMED` không được hủy bằng cancellation policy hiện tại.

## 4. Quy trình

1. Backend kiểm tra ownership/access của booking.
2. Policy kiểm tra trạng thái booking và thời gian còn lại đến check-in.
3. Nếu hợp lệ, booking chuyển `CANCELLED`.
4. Payment thành công tạo refund `PENDING` theo phần trăm policy.
5. Refund applicable yêu cầu thông tin nhận tiền hợp lệ và dùng idempotency key riêng theo booking.

## 5. Các trường hợp không hợp lệ

- Booking `CHECKED_IN`, `CHECKED_OUT`, `CANCELLED` hoặc `EXPIRED`.
- Booking `CONFIRMED` còn dưới 24 giờ trước check-in.
- Người gọi không có quyền truy cập booking.

## 6. Ví dụ

Booking đã thanh toán đủ được hủy còn 30 giờ trước mốc check-in thì refund bằng 50% tổng
tiền booking và bắt đầu ở trạng thái `PENDING`.

## 7. Source of Truth

- `StandardBookingCancellationPolicy.java`
- `BookingCancellationProperties.java`, `application.yaml`
- `BookingServiceImpl.java`
- `RefundManagementServiceImpl.java`, `PaymentMonitoringController.java`
