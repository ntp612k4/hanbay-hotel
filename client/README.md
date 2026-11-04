# HANBAY HOTEL — Frontend

Frontend React và TypeScript dùng Vite, React Router, TanStack Query,
Ant Design, React Hook Form và Zod.

## Chạy local

Hướng dẫn khởi động Docker, backend và ngrok/SePay nằm trong
[README chính](../README.md#chạy-demo-local-trên-windows).

Yêu cầu Node.js `^20.19.0 || >=22.12.0` theo Vite đang dùng.
Trong thư mục `client`, chuẩn bị cấu hình nếu chưa có:

```powershell
if (-not (Test-Path -LiteralPath '.env.local')) {
    Copy-Item -LiteralPath '.env.example' -Destination '.env.local'
}
```

Nội dung cấu hình:

```dotenv
VITE_API_BASE_URL=/api/v1
VITE_APP_NAME=Hotel Management
```

Cài dependencies lần đầu hoặc sau khi lockfile thay đổi:

```powershell
npm ci
```

Bật backend tại `http://localhost:8081`, rồi chạy frontend trong terminal riêng:

```powershell
npm run dev -- --port 5173 --strictPort
```

Mở `http://localhost:5173`. Vite proxy `/api` và `/actuator` sang backend
`http://localhost:8081`; cấu hình ở `vite.config.ts`. Cổng `5173` phải trống.
Giữ terminal mở trong lúc demo và dùng `Ctrl+C` để dừng.

## Kiểm tra và build

```powershell
npm run test:run
npm run lint
npm run build
```

`npm run build` tạo thư mục `dist`; lệnh demo ở trên sử dụng Vite dev server.
Thông tin truy cập MinIO, pgAdmin và webhook SePay xem README chính.
