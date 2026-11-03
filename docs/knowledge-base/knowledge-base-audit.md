# HANBAY HOTEL — Knowledge Base Audit

Đối chiếu code main `918185b9`, ngày 09/09/2026. Đây là audit mã nguồn, không phải kết quả
kiểm thử RAG/provider hoặc cấu hình VPS.

| Chủ đề | Hành vi hiện tại | Điều chỉnh corpus / giới hạn |
|---|---|---|
| Trạng thái booking và thanh toán | PENDING ban đầu phải được xác nhận trước check-in; gia hạn giữ CONFIRMED/CHECKED_IN | Không đồng nhất còn nợ với booking PENDING; chưa có pay-later ban đầu |
| Thanh toán bổ sung | PaymentService cho CONFIRMED/CHECKED_IN còn nợ; webhook xử lý additional-balance | Bỏ nội dung cũ “chỉ hỗ trợ PENDING” |
| Thu tại checkout | CounterPaymentService ghi CASH receipt và transaction | Mô tả tiền thực thu, không thu vượt nợ; không có POS |
| Trả sớm | 80% unused nights được bù với công nợ/phí | Không hứa hoàn nguyên 80% trong mọi trường hợp |
| Hủy sau gia hạn | Cancellation policy nhân totalAmount, không paidAmount | Không hứa công thức theo thực trả; cần kiểm tra riêng nhiều payment/refund |
| Pending chuyển thiếu rồi hủy | Nhánh hủy pending không tự tạo refund cho tiền nhận dở | Hướng dẫn đối soát; cần hoàn thiện luồng nghiệp vụ, không coi đã tự hoàn |
| Mốc 14:00 | Dùng trong cancellation; check-in chỉ kiểm tra ngày | Không hứa endpoint chặn trước giờ 14:00 |
| Tuổi trẻ em | Booking API nhận counts, không tuổi từng khách | Bỏ khẳng định tự phân loại <=12 trong API |
| Quyền | Check-in RECEPTIONIST; xác nhận refund MANAGER | Không suy diễn ADMIN có mọi quyền |
| RAG | Loader, embeddings, pgvector, checksum ingestion và retriever đã tồn tại | Audit cũ “RAG chưa tồn tại” không còn đúng; mặc định RAG vẫn tắt |
| Authentication và logging | Chat cần đăng nhập, rate limit; audit SQL redact | Redis/fallback vẫn chứa chat; không hứa không lưu nội dung |
| Sources | Tool trả title/category/section từ retrieved metadata | Có nguồn chưa bảo đảm tài liệu luôn mới; cần rebuild/restart sau đổi corpus |
| Version | Có front matter version và checksum | Chưa có lịch hiệu lực hoặc workflow duyệt tự động |
| Retrieval | Chunk theo ###; top-k và threshold cấu hình | Quy tắc cần truy xuất đặt dưới ###; phải kiểm chứng semantic retrieval với provider thực |

Bản hot-fix chỉ đồng bộ tài liệu với code. Các giới hạn code ở trên vẫn cần xử lý riêng;
không mở check-in cho PENDING để thay thế thiết kế xác nhận trả sau.
