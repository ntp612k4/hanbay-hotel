---
documentId: HANBAY-CHECK-IN-POLICY
title: Check-in Policy
category: CHECK_IN
source: hotel-booking/src/main/resources/knowledge-base/check-in-policy.md
version: 1.1
hotel: HANBAY HOTEL
language: vi
---

# HANBAY HOTEL — Chính sách check-in

## 1. Mục đích

Mô tả điều kiện gán phòng vật lý và check-in.

## 2. Phạm vi

Áp dụng cho thao tác nghiệp vụ của `RECEPTIONIST` trên booking đã xác nhận.

## 3. Quy định

### 3.1 Giờ nhận phòng

Mốc check-in mặc định dùng để tính thời hạn hủy và mức hoàn tiền là **14:00** theo múi giờ
`Asia/Ho_Chi_Minh`. Endpoint check-in hiện chỉ kiểm tra ngày hiện tại trùng `checkInDate`,
không chặn nhận phòng trước 14:00 trong cùng ngày. Không suy diễn chính sách phí nhận sớm.

### 3.2 Điều kiện booking

Chỉ booking `CONFIRMED` được check-in. Ngày hiện tại theo clock/timezone khách sạn phải
trùng `checkInDate` của booking.

Booking `PENDING` chưa được xác nhận nên không được check-in, kể cả khách muốn trả tiền
lúc checkout. Booking đã xác nhận rồi gia hạn có thể còn công nợ nhưng vẫn `CONFIRMED`;
service check-in không kiểm tra lại `paidAmount` hoặc yêu cầu công nợ bằng 0. Booking đó
có thể check-in nếu đúng ngày, đúng phòng và đúng quyền; khoản nợ còn lại phải được quyết toán.

### 3.3 Điều kiện phòng vật lý

Phòng được chọn phải tồn tại, thuộc đúng room type của booking và đang `AVAILABLE`.

### 3.4 Phân quyền và trạng thái sau check-in

Chỉ `RECEPTIONIST` được gọi check-in endpoint. Thành công sẽ gán phòng, chuyển booking sang
`CHECKED_IN` và chuyển phòng từ `AVAILABLE` sang `OCCUPIED`.

Tài khoản chỉ có `MANAGER` hoặc `ADMIN` không được check-in nếu không có role `RECEPTIONIST`.
Những vai trò này có thể xem danh sách khách đến. Gửi lại check-in đã thành công bị từ chối
với `BOOKING_ALREADY_CHECKED_IN`, không gán thêm một phòng.

## 4. Quy trình

1. Receptionist chọn booking `CONFIRMED` trong đúng ngày nhận phòng.
2. Hệ thống liệt kê phòng `AVAILABLE` đúng room type.
3. Backend khóa booking và phòng, rồi kiểm tra lại mọi điều kiện.
4. Booking và phòng được cập nhật trong cùng transaction.

## 5. Các trường hợp không hợp lệ

- Booking không `CONFIRMED`, đã check-in hoặc không đúng ngày.
- Phòng khác room type, không tồn tại hoặc không còn `AVAILABLE`.
- Người gọi không có role `RECEPTIONIST`.

## 6. Ví dụ

Booking Deluxe `CONFIRMED` hôm nay chỉ có thể được gán một phòng Deluxe đang
`AVAILABLE`; không thể gán phòng Suite hoặc phòng `MAINTENANCE`.

## 7. Source of Truth

- `CheckInController.java`, `CheckInServiceImpl.java`
- `Booking.java`, `Room.java`
