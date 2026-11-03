# HANBAY HOTEL

## Smart Hotel Reservation & Property Management System

---

## 1. Project Overview

HANBAY HOTEL là hệ thống **Smart Hotel Reservation & Property Management System (PMS)** được xây dựng nhằm số hóa toàn bộ quy trình vận hành của một khách sạn, từ khâu tìm kiếm phòng, đặt phòng trực tuyến đến quản lý vận hành nội bộ.

Khác với các hệ thống Hotel Booking thông thường, HANBAY HOTEL mô phỏng đầy đủ nghiệp vụ khách sạn bao gồm:
- Online Reservation
- Front Desk Management
- Room Assignment
- Housekeeping
- Dynamic Pricing
- Payment Management
- Hotel Administration

Hệ thống phục vụ cả khách hàng và nhân viên khách sạn trong cùng một nền tảng.

---

## 2. Objectives

### Business Objectives

- Đơn giản hóa quy trình đặt phòng.
- Hạn chế overbooking.
- Tối ưu quy trình Check-in / Check-out.
- Hỗ trợ vận hành khách sạn.
- Tăng doanh thu nhờ Dynamic Pricing.

### Technical Objectives

- RESTful API.
- Modular Monolith Architecture.
- JWT Authentication & Authorization.
- Payment Gateway Integration.
- Concurrency Control.
- Dockerized Deployment.
- AI Assistant với Spring AI.

---

## 3. Target Users

### Guest

- Search Room
- Booking
- Payment
- Booking History
- Cancellation

### Receptionist

- Dashboard
- Check-in
- Check-out
- Room Assignment
- Housekeeping

### Manager

- Room Management
- Room Type Management
- Dynamic Pricing
- Reports

---

## 4. Core Modules

### Customer Portal
- Authentication
- Room Search
- Booking
- Payment

### PMS
- Reception Dashboard
- Room Assignment
- Housekeeping
- Room Status

### Administration
- Room Types
- Physical Rooms
- Pricing
- Reports

### AI Assistant
- Room Recommendation
- Hotel Policy Assistant

---

## 5. Core Technical Challenges

### Room Availability
Tính phòng trống bằng **Overlapping Booking** thay vì trạng thái hiện tại.

### Booking Lifecycle

```text
PENDING
 ↓
CONFIRMED
 ↓
CHECK-IN
 ↓
CHECKED_OUT

Hoặc

PENDING
 ↓
CANCELLED

Hoặc khi quá hạn thanh toán

PENDING
 ↓
EXPIRED
```

### Concurrency Control
Ngăn nhiều khách đặt cùng một phòng tại cùng thời điểm.

### Dynamic Pricing
Tính giá theo từng đêm dựa trên lịch giá.

### Housekeeping State Machine

```text
AVAILABLE
 ↓
OCCUPIED
 ↓
DIRTY
 ↓
CLEANING
 ↓
AVAILABLE
```

---

## 6. Technology Stack

### Backend
- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA
- Spring AI
- PostgreSQL
- Flyway
- Docker

### Frontend
- React
- TypeScript
- Vite
- React Router
- TanStack Query
- Axios
- Ant Design
- React Hook Form
- Zod

### DevOps
- Docker Compose
- GitHub Actions
- Nginx

---

## 7. Expected Outcomes

- RESTful API
- Responsive Web Application
- Swagger Documentation
- Docker Deployment
- AI Integration
- Production-ready Architecture

---

## 8. Future Enhancements

- Multi-hotel Management
- Loyalty Program
- Revenue Management
- OTA Integration
- Mobile Application

---

## 9. Team Vision

HANBAY HOTEL hướng tới việc mô phỏng một hệ thống PMS thu nhỏ với các bài toán thực tế:

- Availability Calculation
- Overlapping Booking
- Booking Lifecycle
- Concurrency Control
- Dynamic Pricing
- Housekeeping Workflow

Mục tiêu của nhóm là xây dựng một hệ thống có kiến trúc rõ ràng, nghiệp vụ sát thực tế và có khả năng mở rộng trong tương lai.
