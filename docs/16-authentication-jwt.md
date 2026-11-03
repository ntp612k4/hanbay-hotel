# Authentication và JWT

Tài liệu này mô tả đúng chức năng xác thực hiện có.

## Mô hình tài khoản

Khách có tài khoản thật để đăng nhập, quản lý hồ sơ và đặt phòng của chính mình. Tài khoản khách không có vai trò nhân viên. Các vai trò nhân viên hợp lệ chỉ gồm:

- `ADMIN`
- `MANAGER`
- `RECEPTIONIST`
- `HOUSEKEEPER`

Tài khoản khách không cần một role nghiệp vụ; role chỉ dành cho nhân sự và quản trị hệ thống.

## API hiện có

| Method | Path | Mô tả |
| --- | --- | --- |
| POST | `/api/v1/auth/register` | Đăng ký tài khoản khách, không gán staff role |
| POST | `/api/v1/auth/login` | Đăng nhập bằng email hoặc số điện thoại |
| POST | `/api/v1/auth/refresh-token` | Thay refresh token cũ và cấp access token mới |

Logout hiện xóa session tại client. API revoke refresh token sẽ được triển khai cùng task quản lý phiên; tài liệu không công bố endpoint chưa tồn tại.

## Phản hồi đăng nhập

```json
{
  "success": true,
  "message": "Đăng nhập thành công",
  "data": {
    "accessToken": "<jwt>",
    "refreshToken": "<opaque-token>",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": "uuid",
      "fullName": "Nguyễn Văn A",
      "roles": []
    }
  },
  "timestamp": "2026-08-08T06:00:00Z"
}
```

Access token chứa `iss`, `sub`, `roles`, `iat`, `exp`. Refresh token là chuỗi ngẫu nhiên; database chỉ lưu SHA-256 hash. Mỗi lần refresh sẽ revoke token cũ và tạo token mới.

## Phân quyền dựa trên vai trò

Vai trò nghiệp vụ được lưu tại `roles.code` và ánh xạ bằng enum `RoleCode`. Bốn mã vai trò hợp lệ là `ADMIN`, `MANAGER`, `RECEPTIONIST` và `HOUSEKEEPER`.

Khi phát hành access token, backend lấy các vai trò từ `user.getRoles()`, chuyển `role.code` thành chuỗi và ghi danh sách đã sắp xếp vào JWT claim `roles`. Tài khoản khách không có vai trò nhân viên vẫn có claim `roles` với danh sách rỗng.

Sau khi JWT được xác thực, `JwtAuthenticationFilter` ánh xạ từng giá trị trong claim `roles` thành một `SimpleGrantedAuthority` có tiền tố `ROLE_`. Ví dụ, vai trò `MANAGER` được ánh xạ thành quyền `ROLE_MANAGER`. Spring Security sử dụng các quyền này khi đánh giá quy tắc phân quyền của API.

Protected API nhận access token qua header:

```http
Authorization: Bearer <access_token>
```

- `401 Unauthorized` với mã `AUTH_UNAUTHORIZED`: yêu cầu chưa được xác thực do access token bị thiếu, không hợp lệ hoặc đã hết hạn.
- `403 Forbidden` với mã `AUTH_FORBIDDEN`: JWT đã được xác thực thành công nhưng người dùng không có vai trò hoặc quyền mà API cho phép.

Vai trò được phép của từng API được quy định trong [`06-api-specification.md`](06-api-specification.md). Tài liệu không mặc định quan hệ phân cấp vai trò; một vai trò chỉ được phép khi đặc tả của API liệt kê vai trò đó.

## Client session bootstrap

1. Access token còn hạn: phục hồi session.
2. Access token hết hạn nhưng còn refresh token: gọi refresh API.
3. Refresh thành công: lưu token pair mới trong cùng loại storage.
4. Refresh thất bại hoặc hết hạn: xóa session và chuyển về login khi truy cập route bảo vệ.

`remember=true` dùng `localStorage`; ngược lại dùng `sessionStorage`.

## Cấu hình

- `JWT_ISSUER`
- `JWT_SECRET`
- `JWT_ACCESS_TOKEN_EXPIRATION_SECONDS`
- `JWT_REFRESH_TOKEN_EXPIRATION_SECONDS`

Production bắt buộc cung cấp secret riêng đủ mạnh.
