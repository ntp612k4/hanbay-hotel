# Giới hạn đăng nhập

- Mỗi IP: mặc định 30 yêu cầu đăng nhập trong 60 giây, kể cả request không hợp lệ.
- Mỗi cặp tài khoản/IP: 5 lần sai trong 900 giây; đăng nhập thành công xóa bộ đếm của cặp đó. Không khóa toàn bộ tài khoản theo lỗi từ một IP khác.
- HTTP 429 trả `AUTH_RATE_LIMITED` và `Retry-After` theo số giây còn lại. Frontend hỗ trợ cả số giây và HTTP date; khi quay lại tab, thời gian chờ được cập nhật theo đồng hồ.
- Redis dùng bộ đếm có TTL. Mỗi instance duy trì cửa sổ cục bộ để tiếp tục giới hạn khi Redis gián đoạn và phục hồi. Cửa sổ cục bộ có tối đa 10.000 key, tự dọn key hết hạn; khi đầy, yêu cầu tạo key mới nhận 429. Khi Redis ngừng hoạt động, giới hạn chỉ được đảm bảo riêng trên từng instance.

## Proxy tin cậy

Ứng dụng dùng Tomcat native forwarding, không dùng `ForwardedHeaderFilter` tin mọi caller. `TRUSTED_PROXY_REGEX` chỉ được đặt thành địa chỉ IP của reverse proxy thực sự; mặc định chỉ tin loopback. Không dùng `.*` hoặc toàn bộ dải client.

Compose trong repository đặt frontend nginx ở `172.30.67.10` trên subnet `172.30.67.0/24` và cấu hình backend chỉ tin địa chỉ đó. Nếu subnet này trùng mạng hiện có, đổi đồng thời subnet, IP frontend và regex backend. Khi triển khai bằng cấu hình khác, cần cấu hình IP proxy tương ứng. Nginx ghi đè `X-Forwarded-For` bằng địa chỉ kết nối và loại bỏ `Forwarded` do client gửi. Mô hình này giả định một reverse proxy; nếu thêm CDN/load balancer, phải cấu hình chuỗi proxy tin cậy tương ứng trước khi bật traffic.

Kiểm tra khi triển khai: hai client khác IP có bộ đếm riêng; thay đổi header `Forwarded`/`X-Forwarded-For` từ client không tạo IP mới; HTTP 429 có `Retry-After`; Redis restart không xóa bộ đếm cục bộ còn hiệu lực. Không đưa dữ liệu người dùng thật vào test.
