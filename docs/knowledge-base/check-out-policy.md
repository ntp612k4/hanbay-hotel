---
documentId: HANBAY-CHECK-OUT-POLICY
title: Check-out Policy
category: CHECK_OUT
source: docs/knowledge-base/check-out-policy.md
version: 1.2
hotel: HANBAY HOTEL
language: vi
---

# HANBAY HOTEL — Chính sách check-out

## 1. Mục đích

Mô tả điều kiện hoàn tất lưu trú và trạng thái phòng sau check-out.

## 2. Phạm vi

Áp dụng cho booking đã `CHECKED_IN` và có phòng vật lý được gán.

## 3. Quy định

### 3.1 Điều kiện booking và phòng

Booking phải ở `CHECKED_IN`, có assigned room, và phòng đó phải ở `OCCUPIED`.
Booking `PENDING` chưa được check-in không thể dùng checkout để thanh toán rồi kết thúc
lưu trú. Thu tại checkout dành cho booking đã nhận phòng; không phải chế độ trả sau toàn
bộ tiền của một booking mới chưa xác nhận.

### 3.2 Công nợ và phí phát sinh

Outstanding amount cuối cùng được tính sau khi cộng phí phát sinh/đền bù và tiền thu thêm tại quầy:
`max(totalAmount + additionalChargeAmount - paidAmount - collectedAmount, 0)`. Nếu còn lớn hơn 0,
check-out bị từ chối. Nếu `additionalChargeAmount > 0`, lễ tân phải nhập lý do phát sinh.

Số tiền thu thêm không được vượt `max(totalAmount + additionalChargeAmount - paidAmount, 0)`.
Phí và số tiền thu thêm nằm trong khoảng 0–9.999.999.999 đồng. Tiền mặt nhập vào là số thực nhận
sau khi trả tiền thừa; không nhập lại khoản chuyển khoản được webhook ghi nhận.
Khoản thu tạo chứng từ `CASH-…` và giao dịch thanh toán thành công, có người thu và thời điểm thu,
trong cùng giao dịch cơ sở dữ liệu với booking và phòng. Nếu lỗi, tất cả thay đổi được hoàn tác.

Luồng này dành cho ngày trả phòng dự kiến hoặc muộn hơn. Khách trả sớm dùng luồng trả phòng sớm
để áp dụng chính sách hoàn tiền.

Công nợ có thể phát sinh do gia hạn sau khi booking đã xác nhận hoặc phí kiểm tra phòng.
Booking đang lưu trú vẫn `CHECKED_IN` khi còn nợ, không đổi thành `PENDING`. Với checkout
thường, thanh toán/thu tại quầy phải làm công nợ cuối cùng bằng 0 trước khi chuyển `CHECKED_OUT`.

### 3.3 Phân quyền và trạng thái sau check-out

`RECEPTIONIST`, `MANAGER` hoặc `ADMIN` được gọi endpoint. Thành công chuyển booking sang
`CHECKED_OUT` và phòng từ `OCCUPIED` sang `DIRTY`.

## 4. Quy trình

1. Backend khóa booking và assigned room.
2. Kiểm tra trạng thái booking và phòng.
3. Ghi nhận phí phát sinh/đền bù nếu có và số tiền thu thêm tại quầy.
4. Kiểm tra outstanding balance cuối cùng.
5. Booking chuyển `CHECKED_OUT`.
6. Phòng chuyển `DIRTY` để chờ housekeeping.

## 5. Các trường hợp không hợp lệ

- Booking không `CHECKED_IN` hoặc đã `CHECKED_OUT`.
- Không có assigned room hoặc phòng không `OCCUPIED`.
- Outstanding amount cuối cùng lớn hơn 0.
- Có phí phát sinh nhưng không có lý do phát sinh.
- Người gọi không có role được phép.

## 6. Ví dụ

Booking tổng 2.000.000 VND, đã thanh toán đủ, nhưng khi kiểm tra phòng phát sinh phí mất thẻ
100.000 VND. Lễ tân ghi nhận `additionalChargeAmount = 100000`, lý do mất thẻ phòng và
`collectedAmount = 100000`; outstanding cuối cùng bằng 0 nên có thể check-out.

## 7. Source of Truth

- `docs/04-system-architecture.md`
- `docs/06-api-specification.md`
- `docs/07-business-workflows.md`
- `CheckOutController.java`, `CheckOutServiceImpl.java`
- `Booking.java`, `Room.java`
