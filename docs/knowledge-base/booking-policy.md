---
documentId: HANBAY-BOOKING-POLICY
title: Booking Policy
category: BOOKING
source: docs/knowledge-base/booking-policy.md
version: 1.1
hotel: HANBAY HOTEL
language: vi
---

# HANBAY HOTEL — Chính sách đặt phòng

## 1. Mục đích

Mô tả quy tắc tạo booking, giữ inventory, sức chứa và xác nhận booking trong hệ thống demo.

## 2. Phạm vi

Áp dụng cho booking phòng được tạo qua HANBAY HOTEL. Không mô tả đặt phòng ngoài hệ thống.

## 3. Quy định

### 3.1 Khoảng lưu trú

Khoảng lưu trú là `[checkInDate, checkOutDate)`: ngày check-in được tính, ngày check-out
không được tính. Check-out phải sau check-in và check-in không được ở quá khứ.

### 3.2 Sức chứa

Booking phải có ít nhất một người lớn. Số trẻ em không được âm. Room type chỉ phù hợp khi
số người lớn không vượt `capacityAdults`, số trẻ em không vượt `capacityChildren`, và
tổng số khách không vượt `maxGuests`.

API hiện nhận số người lớn và số trẻ em, không nhận tuổi/ngày sinh từng khách để tự phân loại.
Knowledge base không suy diễn ngưỡng tuổi, miễn phí dịch vụ, ăn sáng hoặc phụ thu cho trẻ em.

### 3.3 Availability

Availability được tính theo room type và booking overlap, không chỉ theo trạng thái tức thời
của một phòng vật lý. Hai khoảng trùng khi booking hiện có bắt đầu trước check-out yêu cầu và
kết thúc sau check-in yêu cầu.

Booking `CONFIRMED`, `CHECKED_IN` và booking `PENDING` chưa hết hạn chiếm inventory.

### 3.4 Booking PENDING

Booking mới ở trạng thái `PENDING` và giữ inventory tối đa 15 phút. Sau hạn, scheduler có
thể chuyển booking chưa được xác nhận sang `EXPIRED`.

### 3.5 Xác nhận booking

Booking `PENDING` còn hạn được chuyển sang `CONFIRMED` khi hệ thống ghi nhận đủ tiền qua
payment flow. Booking hết hạn không được xác nhận bằng payment đến trễ.

### 3.6 Trạng thái booking và công nợ

Trạng thái booking mô tả vòng đời đặt/lưu trú, không thay thế trạng thái thanh toán.
`PENDING` là booking mới chờ xác nhận, chưa được check-in và không phải chế độ trả sau.
Luồng đặt hiện tại yêu cầu thanh toán đủ khoản ban đầu trong thời hạn để xác nhận booking;
chưa có lựa chọn đặt chưa trả tiền ban đầu rồi nhận phòng và trả toàn bộ khi checkout.

Sau khi đã xác nhận, gia hạn có thể làm tăng tổng tiền và tạo công nợ nhưng booking vẫn
`CONFIRMED` hoặc `CHECKED_IN`, không quay về `PENDING`. Ví dụ đã trả 2.000.000 VND,
gia hạn thêm 500.000 VND thì công nợ là 500.000 VND. Khoản này có thể thanh toán bổ sung
hoặc quyết toán tại checkout. Booking `CONFIRMED` vẫn phải đáp ứng ngày/phòng/quyền khi check-in.

### 3.7 Sửa booking đang chờ

Chỉ sửa booking `PENDING` còn hạn với quyền chủ booking hoặc booking access token hợp lệ.
Hệ thống kiểm tra lại ngày, sức chứa, tồn kho và tính lại snapshot giá/phòng/dịch vụ.
Sửa booking không cộng thêm 15 phút; hạn giữ chỗ ban đầu được giữ nguyên.

## 4. Quy trình

1. Guest chọn ngày, occupancy và room type còn capacity/inventory.
2. Backend kiểm tra lại availability trong transaction và tạo booking `PENDING`.
3. Booking giữ inventory trong tối đa 15 phút.
4. Thanh toán đủ khi booking còn hiệu lực chuyển booking sang `CONFIRMED`.
5. Booking chưa xác nhận khi hết hạn chuyển sang `EXPIRED`.

## 5. Các trường hợp không hợp lệ

- Check-in ở quá khứ hoặc check-out không sau check-in.
- Không có người lớn, số trẻ em âm hoặc vượt capacity của room type.
- Inventory không còn đủ do booking overlap.
- Cố xác nhận booking không còn `PENDING` hoặc đã hết hạn.

## 6. Ví dụ

Booking tạo lúc 10:00 ở trạng thái `PENDING` có hạn giữ đến 10:15. Nếu chưa được xác nhận
khi hết hạn, booking có thể chuyển `EXPIRED` và không tiếp tục giữ inventory.

## 7. Source of Truth

- `docs/02-business-requirements.md`
- `docs/03-software-requirements.md`
- `docs/04-system-architecture.md`
- `docs/06-api-specification.md`
- `docs/07-business-workflows.md`
- `BookingFactory.java`, `BookingRepository.java`, `Booking.java`
- `BookingServiceImpl.java`, `RoomType.java`
