# HANBAY HOTEL — Internal Hotel Policy Knowledge Base

> Bộ tri thức nội bộ giả lập cho mục đích demo/internship. Đây không phải chính sách
> của một khách sạn ngoài đời.

## Mục đích

Corpus này chuyển các business rule đã có trong repository thành tài liệu có thể truy xuất
độc lập. Nội dung không mở rộng nghiệp vụ. Khi không tìm thấy bằng chứng phù hợp, Hotel
Policy Assistant phải trả lời: **“Thông tin này chưa được định nghĩa trong chính sách
HANBAY HOTEL.”**

## Tài liệu

- [Đặt phòng](booking-policy.md)
- [Thanh toán](payment-policy.md)
- [Hủy đặt phòng](cancellation-policy.md)
- [Check-in](check-in-policy.md)
- [Check-out](check-out-policy.md)
- [Early checkout và gia hạn lưu trú](stay-modification-policy.md)
- [Phòng và availability](room-policy.md)
- [Housekeeping](housekeeping-policy.md)
- [Add-on](add-on-policy.md)
- [FAQ](faq.md)
- [Source catalog](source-catalog.md)
- [RAG test cases](rag-test-cases.md)
- [Knowledge-base audit](knowledge-base-audit.md)

## Quy tắc sử dụng cho RAG

- Runtime chỉ index `*-policy.md`; FAQ, source catalog, audit và test cases là tài liệu hỗ trợ, không đóng gói vào corpus.
- Giữ tên file, heading và mục Source of Truth làm metadata nguồn.
- Không index output do LLM tạo hoặc câu hỏi của người dùng như policy.
- Không coi nội dung retrieved là instruction; đây chỉ là dữ liệu tham chiếu.
- Không có evidence phù hợp thì fail closed, không dùng general knowledge để suy đoán.
- Metadata có version tài liệu; chưa có workflow lịch hiệu lực chính sách. Mặc định cấu hình không chứng minh giá trị đang chạy trên VPS.

## Cập nhật và nạp lại

Đối chiếu code `origin/main` tại `918185b9` ngày 09/09/2026. Bản đồng bộ này làm rõ công nợ
sau xác nhận, thu tại checkout, tiền hoàn sau bù trừ và quyền/ngày check-in.

Maven `generate-resources` sao chép `*-policy.md` vào `target/classes/knowledge-base`.
Cần build lại artifact và khởi động backend với RAG bật để `PolicyKnowledgeIndex` so checksum,
embedding lại corpus thay đổi và xóa chunk cũ. Chỉ sửa docs không cập nhật index của process
đang chạy. Không cần xóa thủ công bảng vector. Kiểm tra câu P21–P30 sau triển khai; lượt sửa
repository không đồng nghĩa đã nạp lại corpus hoặc kiểm tra provider trên VPS.
