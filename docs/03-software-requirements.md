# 03. Software Requirements Specification (SRS)

# HANBAY HOTEL
## Smart Hotel Reservation & Property Management System

---

# 1. Purpose

Tài liệu này mô tả các yêu cầu chức năng và phi chức năng của hệ thống HANBAY HOTEL, làm cơ sở cho việc phân tích, thiết kế, phát triển và kiểm thử.

---

# 2. Scope

Hệ thống cung cấp:

- Đặt phòng trực tuyến
- Quản lý khách sạn (PMS)
- Quản lý phòng và hạng phòng
- Thanh toán
- Quản lý lễ tân
- Quản lý buồng phòng
- Dynamic Pricing
- AI Assistant

---

# 3. Stakeholders

| Stakeholder | Vai trò |
|-------------|----------|
| Guest | Đặt phòng |
| Receptionist | Check-in/Check-out |
| Housekeeping Staff | Cập nhật trạng thái phòng |
| Manager | Quản trị hệ thống |
| System Administrator | Quản lý hệ thống |

---

# 4. Functional Requirements

## Guest

### FR-01 Search Available Rooms
- Nhập check-in, check-out.
- Nhập số khách.
- Hiển thị các hạng phòng còn trống.

### FR-02 View Room Details
- Hình ảnh.
- Tiện nghi.
- Chính sách.
- Giá.

### FR-03 Booking
- Chọn phòng.
- Nhập thông tin khách.
- Chọn dịch vụ bổ sung.
- Tạo booking trạng thái PENDING.

### FR-04 Payment
- Thanh toán online.
- Cập nhật booking khi webhook thành công.

### FR-05 Booking History
- Xem lịch sử.
- Hủy booking theo chính sách.

---

## Receptionist

### FR-06 Reception Dashboard
- Danh sách khách check-in hôm nay.
- Danh sách khách check-out hôm nay.

### FR-07 Room Assignment
- Gán phòng vật lý.
- Chỉ hiển thị phòng AVAILABLE.

### FR-08 Check-in
- Xác nhận nhận phòng.
- Chuyển trạng thái booking sang CHECK_IN.

### FR-09 Check-out
- Tính chi phí phát sinh.
- Hoàn tất trả phòng.
- Chuyển phòng sang DIRTY.

---

## Housekeeping

### FR-10 Housekeeping
- Xem phòng DIRTY.
- Bắt đầu dọn phòng: `DIRTY -> CLEANING`.
- Hoàn tất dọn phòng: `CLEANING -> AVAILABLE`.
- Chỉ Housekeeper được thực hiện hai chuyển trạng thái này.

---

## Manager

### FR-11 Room Type Management
CRUD hạng phòng.

### FR-12 Physical Room Management
CRUD phòng vật lý.
Cho phép bảo trì.

### FR-13 Pricing Management
Thiết lập:
- Giá cơ bản.
- Phụ thu cuối tuần.
- Giá ngày lễ.

### FR-14 Reports
- Doanh thu.
- Tỷ lệ lấp đầy.
- Booking.

### FR-15 Add-on Service Management
- Xem và tìm kiếm toàn bộ dịch vụ bổ sung, bao gồm dịch vụ đã ngừng hoạt động.
- Tạo dịch vụ bổ sung với mã duy nhất, tên, mô tả và giá hiện hành.
- Cập nhật tên, mô tả và giá; mã dịch vụ không thay đổi sau khi tạo.
- Kích hoạt hoặc ngừng kích hoạt dịch vụ thay cho xóa vật lý.
- Dịch vụ ngừng hoạt động không được chọn cho booking mới nhưng không làm thay đổi add-on đã snapshot trong booking cũ.

---

## AI

### FR-15 AI Room Recommendation
Đề xuất hạng phòng phù hợp.

### FR-16 AI Policy Assistant
Trả lời câu hỏi về chính sách khách sạn.

---

# 5. Non-functional Requirements

## Security
- JWT Authentication
- RBAC
- Mã hóa mật khẩu bằng BCrypt

## Performance
- Tìm kiếm phòng < 2 giây
- Hỗ trợ tối thiểu 100 người dùng đồng thời

## Reliability
- Không overbooking
- Đảm bảo tính nhất quán dữ liệu

## Scalability
- Thiết kế Modular Monolith
- Có khả năng tách microservices trong tương lai

## Maintainability
- RESTful API
- Coding Convention
- Swagger
- Flyway

---

# 6. Business Rules

BR-01 Không cho phép booking khi không còn phòng.

BR-02 Booking PENDING quá 15 phút sẽ tự động chuyển sang EXPIRED.

BR-03 Chỉ phòng AVAILABLE mới được gán cho khách.

BR-04 Sau Check-out phòng chuyển sang DIRTY.

BR-05 Chỉ Housekeeping hoàn tất dọn phòng mới chuyển AVAILABLE.

BR-06 Giá phòng được tính theo từng đêm.

---

# 7. Assumptions

- Một booking chỉ thuộc một khách hàng.
- Một phòng vật lý chỉ phục vụ một booking tại cùng thời điểm.
- Thanh toán được xác nhận qua webhook.
- AI chỉ hỗ trợ tư vấn, không tự động tạo booking.

---

# 8. Constraints

- Spring Boot Backend.
- React Frontend.
- PostgreSQL Database.
- Docker Deployment.
- Thời gian phát triển: 11 tuần.
- Nhóm phát triển: 5 thành viên.

---

# 9. Acceptance Criteria

Dự án được xem là hoàn thành khi:

- Hoàn thành toàn bộ chức năng MVP.
- API hoạt động đúng theo tài liệu.
- Demo được toàn bộ luồng:
  Search -> Booking -> Payment -> Check-in -> Check-out -> Housekeeping.
- Không xảy ra overbooking trong bài kiểm thử concurrency.
- Hệ thống triển khai thành công bằng Docker Compose.
