---
documentId: HANBAY-ROOM-POLICY
title: Room Policy
category: ROOM
source: docs/knowledge-base/room-policy.md
version: 1.2
hotel: HANBAY HOTEL
language: vi
---

# HANBAY HOTEL — Chính sách phòng và availability

## 1. Mục đích

Phân biệt room type, phòng vật lý, availability và trạng thái vận hành.

## 2. Phạm vi

Áp dụng cho catalog room type, tìm phòng trống và vòng đời phòng vật lý trong hệ thống demo.

## 3. Quy định

### 3.1 Room type và phòng vật lý

Room type mô tả hạng phòng, giá cơ sở, sức chứa và tiện nghi. Phòng vật lý có số phòng, tầng,
room type và trạng thái vận hành riêng.

### 3.2 Availability theo khoảng ngày

Khả dụng để đặt được tính theo inventory room type trừ các booking active bị overlap.
Trạng thái hiện tại của một phòng vật lý không thay thế phép tính booking overlap.

### 3.3 Trạng thái phòng

Các trạng thái là `AVAILABLE`, `OCCUPIED`, `DIRTY`, `CLEANING`, `MAINTENANCE`, `OUT_OF_SERVICE`.
Nguồn cung để tính tồn kho gồm `AVAILABLE`, `OCCUPIED`, `DIRTY`, `CLEANING`; loại các phòng
`MAINTENANCE` và `OUT_OF_SERVICE` khỏi số phòng có thể bán. Check-in chỉ chọn phòng `AVAILABLE`.
Luồng lưu trú/housekeeping canonical:

`AVAILABLE -> OCCUPIED -> DIRTY -> CLEANING -> AVAILABLE`.

Maintenance là nhánh riêng: `AVAILABLE -> MAINTENANCE -> AVAILABLE`.

### 3.4 Maintenance

Chỉ phòng `AVAILABLE` được bắt đầu maintenance. Chỉ phòng `MAINTENANCE` được kết thúc
maintenance. Thao tác maintenance hiện dành cho `MANAGER`.

Quản lý tạo phiếu bảo trì cho phòng trống. Phiếu đi qua `PENDING` (chờ xử lý),
`IN_PROGRESS` (đang xử lý), rồi `COMPLETED` (hoàn thành). Phòng chuyển sang bảo trì
ngay khi tạo phiếu và chỉ trở lại trống khi hoàn tất phiếu. Mỗi phòng chỉ có một phiếu mở.
API đổi trạng thái phòng trực tiếp không được đóng bảo trì nếu phòng còn phiếu chưa hoàn tất.

## 4. Quy trình

1. Search tính availability theo room type, ngày và occupancy.
2. Check-in chọn một phòng vật lý `AVAILABLE` đúng room type.
3. Check-out đưa phòng `OCCUPIED` về `DIRTY`.
4. Housekeeping đưa `DIRTY` qua `CLEANING` về `AVAILABLE`.

## 5. Các trường hợp không hợp lệ

- Đồng nhất “room type còn inventory” với “mọi phòng vật lý đều AVAILABLE”.
- Bắt đầu maintenance trên phòng không `AVAILABLE`.
- Kết thúc maintenance trên phòng không `MAINTENANCE`.
- Bypass các domain transition bằng một trạng thái tự suy diễn.

## 6. Ví dụ

Một Deluxe có thể còn inventory cho tháng sau dù một phòng Deluxe cụ thể đang `OCCUPIED`
hôm nay; availability đặt phòng được tính theo overlap của khoảng ngày yêu cầu.

## 7. Source of Truth

- `docs/01-project-overview.md`
- `docs/03-software-requirements.md`
- `docs/04-system-architecture.md`
- `docs/07-business-workflows.md`
- `Room.java`, `RoomStatus.java`, `RoomType.java`
- `AvailabilityServiceImpl.java`, `RoomMaintenanceServiceImpl.java`
