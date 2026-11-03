# HTTP fallback cho Sprint Review

Happy path của Sprint Review phải chạy bằng UI HANBAY và SePay Test Mode. Các file `.http` trong thư mục này dùng để chuẩn bị dữ liệu, kiểm tra ngoại lệ hoặc làm fallback khi tích hợp ngoài gặp sự cố.

## Thứ tự chạy đề xuất

1. `room.http` hoặc `HAN-24-room-price-search.http`: kiểm tra catalog/search và lấy room type còn trống.
2. `booking.http` request 1: tạo booking `PENDING` và lưu `bookingId`, `bookingAccessToken`.
3. `booking.http` request 2: fallback tạo payment và lưu `paymentId`, `paymentCode`, `paymentAmount`.
4. `HAN-33-payment-result.http`: kiểm tra payment status bằng access token của booking.
5. `sepay-webhook.http` request 1: fallback webhook đủ tiền. Request 2 kiểm tra duplicate.
6. `booking-history.http`: login guest, xem/filter/paginate lịch sử và các validation error.
7. `booking-cancellation.http`: login guest, preview/cancel; login manager, lấy và confirm refund.
8. `today-arrivals.http`: dùng JWT nhân sự để kiểm tra danh sách khách đến hôm nay, phân trang và phân quyền.
9. `manager-dashboard-report.http`: dùng JWT Manager/Admin để kiểm tra dashboard report và validation date range.

## Ma trận coverage

| Flow | File/request |
|---|---|
| Search room, pricing, invalid date/guest/page | `room.http`, `HAN-24-room-price-search.http` |
| Create pending booking | `booking.http` request 1 |
| Invalid stay dates, exceeded capacity | `booking.http` request 4–5 |
| Create/resume payment request | `booking.http` request 2 |
| Payment status: guest token, owner JWT, invalid ID/token | `HAN-33-payment-result.http` |
| Webhook đủ tiền | `sepay-webhook.http` request 1 |
| Duplicate retry | `sepay-webhook.http` request 2 |
| Partial/over/late payment | `sepay-webhook.http` request 3–5 |
| Transfer out, wrong account/code | `sepay-webhook.http` request 6–9 |
| Invalid/missing API key, malformed payload/enum | `sepay-webhook.http` request 10–13 |
| Monitoring và anonymous denial | `sepay-webhook.http` request 14–16 |
| Booking history/filter/pagination/security | `booking-history.http` |
| Cancellation preview/cancel/refund confirmation | `booking-cancellation.http` |
| Today arrivals/pagination/security | `today-arrivals.http` |
| Manager revenue/occupancy/booking report | `manager-dashboard-report.http` |

## Biến phải thay trước khi chạy

- Các giá trị `replace-with-*` phải được thay bằng tài khoản/fixture local; không commit secret thật.
- `sepay-webhook.http` mặc định khớp fallback của `application-local.yml`. Nếu backend chạy bằng env SePay thật, thay `webhookApiKey`, `accountNumber`, `gateway`.
- Mỗi lần chạy webhook phải dùng event ID/reference mới, trừ request duplicate số 2 cố ý dùng lại event ID request 1.
- Partial, overpayment và late payment cần ba payment fixture riêng. Không dùng lại payment đã `SUCCESS` của happy path.
- Cancellation 100%/50%/denied dưới 24 giờ cần booking fixture có check-in tương ứng; file HTTP không tự sửa thời gian database.

## Quy tắc an toàn khi demo

- Không chạy toàn bộ file webhook bằng một lần “Run all”. Chạy từng scenario và kiểm tra prerequisite.
- Không chiếu hoặc commit API key, JWT, password và dữ liệu ngân hàng thật.
- Không dùng request HTTP để thay happy path của guest nếu UI và SePay Test Mode đang hoạt động.
