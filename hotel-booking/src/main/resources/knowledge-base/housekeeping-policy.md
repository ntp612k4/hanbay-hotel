---
documentId: HANBAY-HOUSEKEEPING-POLICY
title: Housekeeping Policy
category: HOUSEKEEPING
source: hotel-booking/src/main/resources/knowledge-base/housekeeping-policy.md
version: 1.0
hotel: HANBAY HOTEL
language: vi
---

# HANBAY HOTEL — Chính sách housekeeping

## 1. Mục đích

Mô tả state machine dọn phòng sau check-out.

## 2. Phạm vi

Áp dụng cho `HOUSEKEEPER` và các phòng `DIRTY` hoặc `CLEANING`.

## 3. Quy định

### 3.1 Bắt đầu dọn

Chỉ `HOUSEKEEPER` được bắt đầu dọn. Transition duy nhất hợp lệ là
`DIRTY -> CLEANING`.

### 3.2 Hoàn tất dọn

Chỉ `HOUSEKEEPER` được hoàn tất dọn. Transition duy nhất hợp lệ là
`CLEANING -> AVAILABLE`.

### 3.3 Giới hạn scope

Housekeeping không tạo task, assignment, schedule, audit history hoặc Manager/Admin override.
Không có direct `DIRTY -> AVAILABLE` hoặc endpoint “mark cleaned”.

## 4. Quy trình

1. Check-out thành công đưa phòng sang `DIRTY`.
2. Housekeeper chọn “Bắt đầu dọn”; backend khóa row và chuyển sang `CLEANING`.
3. Housekeeper chọn “Hoàn tất dọn”; backend khóa row và chuyển sang `AVAILABLE`.

## 5. Các trường hợp không hợp lệ

- Start-cleaning từ `AVAILABLE`, `OCCUPIED`, `CLEANING` hoặc `MAINTENANCE`.
- Complete-cleaning từ `AVAILABLE`, `OCCUPIED`, `DIRTY` hoặc `MAINTENANCE`.
- Role ngoài `HOUSEKEEPER` thực hiện mutation.

## 6. Ví dụ

Phòng vừa check-out ở `DIRTY` phải được start thành `CLEANING` trước; không thể complete
trực tiếp về `AVAILABLE`.

## 7. Source of Truth

- `Room.java`, `HousekeepingServiceImpl.java`, `HousekeepingController.java`
