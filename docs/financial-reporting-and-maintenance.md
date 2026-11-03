# Thu tiền, hoàn tiền và chi phí bảo trì

Booking ban đầu chỉ được xác nhận sau khi thanh toán đủ. Không thu lại tiền phòng
đã thanh toán tại checkout. Sau khi xác nhận, gia hạn hoặc khoản phát sinh được nhập
khi kiểm tra phòng có thể làm tăng tổng phải trả. Trường tiền mặt tại quầy chỉ nhận
số thực thu cho phần còn phải trả sau khi trừ tiền đã trả và khoản hoàn khi trả sớm.
Không ghi nhận chuyển khoản thủ công vào trường tiền mặt. Checkout bị chặn nếu còn
nợ, thu vượt số dư hoặc khoản phí chưa có lý do. Luồng QR riêng cho khoản phí vừa
nhập trong form checkout chưa được triển khai; hiện khoản này được tất toán bằng tiền mặt.

## Dashboard

Khôi phục cách trình bày ưu tiên tiền và lượt đặt phòng từ commit `89fa240f`, giữ
hợp đồng ngày báo cáo hiện tại: `from` bao gồm, `to` không bao gồm, theo giờ Việt Nam.

- Tiền đã thu: các giao dịch `PAYMENT/SUCCESS` trong kỳ (ngân hàng và tiền mặt).
- Tiền đã hoàn: các giao dịch `REFUND/SUCCESS`, theo ngày hoàn thực tế.
- Tiền thu sau hoàn (`netRevenue` cũ): đã thu trừ đã hoàn. Bao gồm cả tiền trả trước.
- Chi phí bảo trì: tổng chi phí hiện hành có `expense_date` trong kỳ; chỉ dùng VND.
- Còn lại sau bảo trì: tiền thu sau hoàn trừ chi phí bảo trì đã ghi nhận.

Đây là số thu/chi được ghi nhận, không phải lợi nhuận kế toán hay doanh thu phân bổ
cho từng đêm lưu trú. Chi phí vận hành khác chưa được hệ thống quản lý. Yêu cầu bảo
trì hoàn thành trong kỳ mà chưa nhập chi phí được cảnh báo; không tự coi là chi phí 0.
Giữ các trường occupancy của API cũ để tương thích, nhưng không ưu tiên trên UI.

## Ghi chi phí bảo trì

Manager mở chi tiết yêu cầu đã bắt đầu hoặc đã hoàn thành, nhập tổng số tiền thực
chi (0 nếu không mất phí và có giải thích), ngày chi và nội dung. Mỗi yêu cầu có một
bản tổng hợp chi phí hiện hành; khi điều chỉnh nhập tổng mới, không nhập số cộng thêm.
Ngày chi không trước ngày tạo yêu cầu và không sau hôm nay. Nếu cần theo dõi nhiều
lần thanh toán vào nhiều ngày cho cùng một lần sửa chữa, cần mở rộng thành sổ chi tiết
chi tiền; màn hình hiện tại ghi tổng chi phí với một ngày chi cho mỗi yêu cầu.

`GET/PUT /api/v1/maintenance-requests/{id}/cost`; PUT yêu cầu `amount`, `expenseDate`,
`note`, `expectedRevision` (0 khi chưa có). Khóa yêu cầu trong transaction và so sánh
revision để ngăn hai người ghi đè. Mỗi phiên bản lưu bất biến trong
`maintenance_cost_history`; báo cáo chỉ cộng `maintenance_costs`, tránh cộng cả lịch sử.
`GET .../cost/history` trả người ghi nhận, thời gian và nội dung từng lần sửa.
Dữ liệu bảo trì cũ được giữ nguyên với chi phí chưa biết, không tạo chi phí giả.

## Hoàn tiền

API danh sách yêu cầu hoàn trả thêm `bankName`, `accountHolderName`, `accountNumber`
từ thông tin khách đã cung cấp. UI hiển thị trong danh sách và hộp xác nhận, giữ số 0
đầu tài khoản. Yêu cầu cũ hoặc hoàn tiền tự động chưa có người nhận sẽ cảnh báo cần
liên hệ xác minh. Chỉ xác nhận đã hoàn sau khi chuyển tiền thực sự và có mã giao dịch.

## Xác minh production 2026-09-08

Release `finance-20260908`, database đã sao lưu tại
`/opt/hotelapp/backups/20260908T074818Z/database.dump` trước migration.
Backend `verify`: 607 test qua. Frontend: 230 test qua trong lượt chạy toàn bộ;
test mới về ngân hàng hoàn tiền qua khi chạy lại sau khi bổ sung mock ResizeObserver
(231 test được xác minh tổng cộng). Lint, formatting và build qua.

Đã kiểm tra qua HTTPS: nhập chi phí khi đủ trạng thái; từ chối revision cũ;
lưu hai phiên bản điều chỉnh; báo thiếu chi phí; đối chiếu số dư với Dashboard.
Trình duyệt thật: đăng nhập Manager, Dashboard mới, cột tài khoản hoàn tiền và
form nhập chi phí lưu đúng số tiền. Đã xóa tài khoản, dữ liệu kiểm thử và thu hồi token.
MinIO Console nội bộ trả HTTP 200; DNS `minio.phuocanh.me` trỏ `163.61.183.153`.
Việc cấp TLS và bật virtual host ngoài cùng cần chạy script bằng sudo trên VPS.
