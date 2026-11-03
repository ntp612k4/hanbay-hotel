---
documentId: HANBAY-PAYMENT-POLICY
title: Payment Policy
category: PAYMENT
source: hotel-booking/src/main/resources/knowledge-base/payment-policy.md
version: 1.1
hotel: HANBAY HOTEL
language: vi
---

# HANBAY HOTEL — Chính sách thanh toán

## 1. Mục đích

Mô tả cách payment được tạo, xác nhận và đối soát trong hệ thống demo HANBAY HOTEL.

## 2. Phạm vi

Áp dụng cho payment gắn với booking và webhook chuyển khoản SePay đã triển khai.

## 3. Quy định

### 3.1 Payment request

Payment request phục vụ booking `PENDING` còn hạn hoặc booking `CONFIRMED`/`CHECKED_IN`
còn công nợ. Amount bằng `max(totalAmount - paidAmount, 0)`, currency `VND`, payment mới
ở trạng thái `PENDING`. Không tạo payment request khi công nợ bằng 0 hoặc booking ở trạng
thái khác. Payment chờ còn hạn và cùng amount có thể được tái sử dụng.

Thời hạn payment theo cấu hình; với booking còn hạn giữ chỗ, hạn payment không vượt hạn
booking. Payment `PENDING` không có nghĩa booking cũng `PENDING`: payment bổ sung có thể
thuộc booking đã xác nhận hoặc đang lưu trú.

### 3.2 Xác nhận thanh toán

Webhook phải vượt xác thực API key, là giao dịch tiền vào đúng tài khoản cấu hình và chứa mã
payment hợp lệ. Event/provider transaction được chống xử lý trùng.

Các giao dịch tiền vào hợp lệ của cùng payment được cộng dồn. Với thanh toán ban đầu, khi
số tiền nhận đạt payment amount, payment vẫn `PENDING` và booking vẫn `PENDING` còn hạn
tại lúc xử lý webhook, payment thành `SUCCESS` và booking thành `CONFIRMED`.

Với payment công nợ bổ sung do hệ thống tạo cho booking `CONFIRMED`/`CHECKED_IN`, webhook
ghi nhận tiền và hoàn tất payment khi đủ amount; không đổi booking về `PENDING` và không
thực hiện check-in thay lễ tân.

### 3.3 Thiếu tiền

Với booking mới `PENDING`, chuyển thiếu tiền không đủ để xác nhận hoặc check-in; tiền đã
nhận được ghi nhận nhưng payment chưa hoàn thành. Không có luồng cho booking mới bỏ qua
thanh toán ban đầu rồi trả toàn bộ ở checkout.

Với booking đã `CONFIRMED`/`CHECKED_IN`, chuyển thiếu khoản bổ sung chỉ làm giảm công nợ,
không đưa booking quay lại `PENDING`. Công nợ còn lại phải được quyết toán ở checkout.

### 3.4 Thừa tiền và payment đến trễ

Phần tiền vượt payment amount tạo refund `PENDING`. Giao dịch đến khi booking/payment không
còn đủ điều kiện xác nhận cũng tạo refund `PENDING` theo event; hệ thống không dùng giao dịch
đến trễ để khôi phục booking hết hiệu lực.

### 3.5 Giới hạn phương thức

Hệ thống hỗ trợ chuyển khoản theo QR/payment request và ghi nhận tiền mặt thực thu tại
checkout. Khoản thu tại quầy giải quyết công nợ còn lại, ví dụ tiền gia hạn hoặc phí phát
sinh; không phải lựa chọn bỏ qua thanh toán ban đầu cho booking `PENDING`.

Nhân viên ghi số thực thu sau trả tiền thừa; không nhập lại khoản đã được webhook ghi nhận.
Checkout thường yêu cầu `max(totalAmount + additionalChargeAmount - paidAmount - collectedAmount, 0)`
bằng 0 và không cho thu vượt công nợ. Khoản thu được lưu chứng từ `CASH-…`, giao dịch
`PAYMENT / SUCCESS`, người thu và thời điểm thu trong transaction checkout.
Trả sớm còn bù trừ giá trị đêm chưa dùng theo chính sách trả phòng sớm trước khi thu/hoàn.
Chưa có tích hợp máy POS hoặc tự động thu tiền mặt.

### 3.6 Refund destination

Cancellation và early checkout có refund sử dụng `BANK_TRANSFER`. Thông tin tối thiểu gồm
tên ngân hàng, tên chủ tài khoản và số tài khoản. Hệ thống không thu thập OTP, mật khẩu ngân
hàng, CVV hoặc PIN. Refund mới ở `PENDING`; trạng thái này không có nghĩa provider đã hoàn
tiền thành công. Role `MANAGER` xác nhận khoản hoàn đang `PENDING` bằng mã tham chiếu
ngân hàng sau khi đối soát; hệ thống ghi nhận refund và giao dịch hoàn `SUCCESS`.
Luồng này chưa tự chuyển tiền ra ngân hàng hoặc nhận callback hoàn tiền của provider.

## 4. Quy trình

1. Tạo payment request cho booking `PENDING` còn hạn hoặc `CONFIRMED`/`CHECKED_IN` còn nợ.
2. Guest chuyển khoản với đúng payment code.
3. Webhook được xác thực và deduplicate.
4. Backend cộng dồn giao dịch hợp lệ.
5. Đủ tiền cho booking ban đầu còn hạn: payment `SUCCESS`, booking `CONFIRMED`.
6. Thừa tiền hoặc đến trễ: tạo refund `PENDING` tương ứng.
7. Khoản bổ sung cho booking đã xác nhận/đang ở được ghi nhận mà không đổi trạng thái lưu trú.
8. Công nợ còn lại và phí phát sinh có thể quyết toán tại checkout theo điều kiện thu tại quầy.

## 5. Các trường hợp không hợp lệ

- Booking `PENDING` đã hết hạn, booking không thuộc trạng thái được thanh toán hoặc không còn nợ.
- Người gọi không phải chủ booking và không có booking access token hợp lệ.
- Webhook sai API key, giao dịch tiền ra, sai tài khoản hoặc không có payment code.
- Payment code không tồn tại.
- Số tiền nhận chưa đủ để xác nhận booking.

## 6. Ví dụ

Booking tổng 1.000.000 VND nhận 600.000 VND thì chưa được xác nhận. Nếu sau đó nhận thêm
400.000 VND trong thời hạn, tổng tiền đủ và booking có thể thành `CONFIRMED`.

Booking đã `CHECKED_IN`, tổng mới sau gia hạn 2.500.000 VND, đã trả 2.000.000 VND:
có thể tạo payment 500.000 VND hoặc thu đúng khoản còn thiếu khi checkout. Booking không
trở về `PENDING` chỉ vì còn 500.000 VND công nợ.

## 7. Source of Truth

- `PaymentServiceImpl.java`, `PaymentWebhookServiceImpl.java`
- `Payment.java`, `PaymentStatus.java`, `PaymentProperties.java`
- `CounterPaymentService.java`, `CheckOutServiceImpl.java`, `StayModificationServiceImpl.java`
- `RefundManagementServiceImpl.java`, `PaymentMonitoringController.java`
