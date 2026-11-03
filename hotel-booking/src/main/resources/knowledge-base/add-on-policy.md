---
documentId: HANBAY-ADD-ON-POLICY
title: Add-on Policy
category: ADD_ON
source: hotel-booking/src/main/resources/knowledge-base/add-on-policy.md
version: 1.0
hotel: HANBAY HOTEL
language: vi
---

# HANBAY HOTEL — Chính sách add-on

## 1. Mục đích

Mô tả dịch vụ bổ sung được chọn cùng booking và cách bảo toàn giá lịch sử.

## 2. Phạm vi

Áp dụng cho add-on catalog và add-on selection của booking.

## 3. Quy định

### 3.1 Add-on active

Chỉ add-on đang active được chọn cho booking mới. Add-on inactive không được chọn mới nhưng
không làm thay đổi dữ liệu đã snapshot trong booking cũ.

### 3.2 Quantity

Quantity cho mỗi add-on phải từ 1 đến 20. Cùng một add-on không được lặp thành nhiều selection
trong một request booking.

### 3.3 Giá

Backend lấy giá catalog hiện hành và snapshot `quantity`, `unitPrice`, `totalPrice` vào
booking. Thay đổi giá catalog sau đó không sửa snapshot của booking cũ.

## 4. Quy trình

1. Guest xem catalog add-on active.
2. Guest chọn add-on và quantity hợp lệ.
3. Backend kiểm tra active/duplicate/quantity và lấy giá hiện hành.
4. Booking lưu snapshot giá và số lượng.

## 5. Các trường hợp không hợp lệ

- Add-on không tồn tại hoặc inactive.
- Quantity nhỏ hơn 1 hoặc lớn hơn 20.
- Cùng add-on xuất hiện lặp trong request.
- Client tự quyết định giá thay cho catalog backend.

## 6. Ví dụ

Guest chọn add-on giá 200.000 VND với quantity 2; booking snapshot unit price 200.000 VND và
total 400.000 VND. Giá catalog đổi sau đó không sửa booking này.

## 7. Source of Truth

- `AddOnSelectionRequest.java`, `BookingServiceImpl.java`
- `BookingAddOn.java`, `AddOnManagementServiceImpl.java`
