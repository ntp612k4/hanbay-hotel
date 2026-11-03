# 07. Business Workflows

# HANBAY HOTEL
## Business Workflow Document

---

# 1. Room Search Flow

```mermaid
flowchart TD
    A[Guest enters check-in/check-out date] --> B[System validates date range]
    B --> C[Calculate total rooms by room type]
    C --> D[Find overlapped bookings]
    D --> E[Calculate available rooms]
    E --> F[Calculate dynamic price]
    F --> G[Return available room types]
```

---

# 2. Booking & Payment Flow

```mermaid
flowchart TD
    A[Guest selects room type] --> B[Create PENDING booking]
    B --> C[Set expired_at = now + 15 minutes]
    C --> D[Create payment request]
    D --> E{Payment success?}
    E -->|Yes| F[Payment SUCCESS]
    F --> G[Booking CONFIRMED]
    E -->|No| H[Payment FAILED]
    H --> I[Booking remains PENDING until timeout]
```

---

# 3. Booking Expiration Flow

```mermaid
flowchart TD
    A[Spring Scheduler runs every 1 minute] --> B[Find expired PENDING bookings]
    B --> C[Update booking to EXPIRED]
    C --> D[Release room availability]
```

---

# 4. Check-in Flow

```mermaid
flowchart TD
    A[Guest arrives] --> B[Receptionist searches booking]
    B --> C[System verifies CONFIRMED booking]
    C --> D[Show AVAILABLE physical rooms]
    D --> E[Receptionist assigns room]
    E --> F[Room becomes OCCUPIED]
    F --> G[Booking becomes CHECKED_IN]
```

---

# 5. Check-out Flow

```mermaid
flowchart TD
    A[Guest requests check-out] --> B[Receptionist inspects room]
    B --> C[Record damage or lost item charges if any]
    C --> D[Receptionist checks final outstanding balance]
    D --> E[Guest pays remaining amount at front desk]
    E --> F[Booking becomes CHECKED_OUT]
    F --> G[Room becomes DIRTY]
```

---

# 6. Housekeeping Flow

```mermaid
flowchart TD
    A[Room status DIRTY] --> B[Housekeeper starts cleaning]
    B --> C[Room status CLEANING]
    C --> D[Housekeeper completes cleaning]
    D --> E[Room becomes AVAILABLE]
```

Trong MVP, `CHECKED_OUT` là trạng thái kết thúc của booking; không có trạng thái
`COMPLETED` riêng. Check-out chỉ được thực hiện sau khi backend ghi nhận công nợ
cuối cùng bằng `0`. Lễ tân có thể ghi nhận phí phát sinh/đền bù sau khi kiểm tra
phòng và số tiền thu thêm tại quầy trong request check-out; hệ thống chưa tích
hợp máy POS hoặc nhà cung cấp thanh toán riêng cho khoản thu tại quầy. Khoản thu tiền mặt
được lưu thành chứng từ `CASH-…` với giao dịch `PAYMENT / SUCCESS`, người thu và thời điểm thu.
Số tiền thực nhận không vượt công nợ sau khi cộng phí đền bù. Webhook và checkout cùng khóa
booking trước khi cập nhật số tiền đã trả; gửi checkout trùng không tạo thêm khoản thu.

---

# 7. Dynamic Pricing Flow

```mermaid
flowchart TD
    A[Guest selects stay period] --> B[Loop through each night]
    B --> C[Find price rate for each date]
    C --> D[Apply weekend/holiday surcharge]
    D --> E[Calculate total amount]
```

---

# 8. Cancellation Flow

```mermaid
flowchart TD
    A[Guest requests cancellation] --> B[System checks cancellation policy]
    B --> C{Allowed?}
    C -->|Yes| D[Cancel booking]
    D --> E[Update payment/refund status]
    C -->|No| F[Reject cancellation]
```
# 9. Trả phòng sớm và gia hạn lưu trú

Trả phòng sớm khóa booking và phòng, tính khoản hoàn từ snapshot từng đêm. Phí hư hỏng/phát sinh và công nợ được bù trừ vào khoản hoàn; nếu còn phải thu thì lễ tân chỉ được thu đúng phần chênh lệch tại quầy. Khi có khoản hoàn, hệ thống tạo yêu cầu chuyển khoản `PENDING`; sau đó atomically chuyển booking sang `CHECKED_OUT` và phòng sang `DIRTY`. Gia hạn khóa booking, rồi khóa hạng phòng và phòng đã gán (nếu có); hệ thống kiểm tra lại khả dụng trước khi lưu các snapshot và tổng tiền mới.
