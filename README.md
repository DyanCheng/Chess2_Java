# Chess2 Java

## Pixel chess assets

Giao diện dùng các sprite trong `backend/src/main/resources/static/pixel-chess-v1/`.
Asset gốc **Pixel chess v1.2** bởi **Dani Maccari** (https://dani-maccari.itch.io/),
được sử dụng theo tệp `LICENSE-AND-ATTRIBUTION.txt` đi kèm. Không tách riêng,
đóng gói lại hoặc phân phối lại bộ asset này.

Backend Java cho Arcane Chess mobile. Dự án thay thế demo dữ liệu trong bộ nhớ bằng Spring Boot, JWT, Spring Security, JPA/Hibernate và Supabase Postgres.

## Phạm vi hiện tại

- Đăng ký/đăng nhập bằng BCrypt và JWT; phân quyền `PLAYER`/`ADMIN`.
- PvE, PvP lobby, local game; server kiểm tra lượt và nước đi cơ bản, lưu board bền vững.
- HP và thẻ chức năng, kho thẻ, shop dùng gold, puzzle và bảng xếp hạng.
- STOMP WebSocket thông báo cập nhật game ở `/ws` và topic `/topic/games/{gameId}`.
- Flyway tạo schema `chess` tách biệt khỏi bảng `public` của Supabase cũ.
- Docker Compose và Dockerfile cho Vercel container deployment.

## Chạy cục bộ

1. Tạo `backend/.env` hoặc file `.env` ở repository từ `.env.example` và điền `SUPABASE_DB_URL`, `JWT_SECRET`.
2. Lấy chuỗi JDBC **Session pooler** của Supabase; không dùng Transaction pooler cho Hibernate/JPA.
3. Chạy `docker compose up --build`.
4. Health check: `http://localhost:8080/actuator/health`.

Hoặc chạy test không cần Supabase:

```powershell
cd backend
mvn verify
```

## Vercel

Vercel sẽ build `Dockerfile.vercel` khi repository được import như một Container project. Cấu hình Environment Variables cho Preview và Production:

- `SUPABASE_DB_URL`: JDBC Session pooler có `sslmode=require`.
- `JWT_SECRET`: secret Base64 tối thiểu 32 byte.
- `CORS_ALLOWED_ORIGINS`: URL Vercel của mobile-web/demo, phân tách bằng dấu phẩy.
- `DB_POOL_SIZE`: mặc định `4`.

Kết nối GitHub repository trong Vercel để mỗi push/PR tạo deployment Preview và push vào `main` tạo Production. GitHub Actions thực hiện Maven verify và Docker build trước; Vercel là lớp CD.

Không đặt database password hoặc JWT secret trong GitHub Actions, `.env.example`, source code hay mobile client.

## Supabase migration

Migration nằm tại `backend/src/main/resources/db/migration/V1__create_chess_schema.sql`. Spring Boot sẽ chạy migration ở schema `chess` khi khởi động với quyền database phù hợp. Kiểm tra đúng project Supabase trước khi khởi động Production: schema này độc lập với `public` của app React Native cũ.

## Lưu ý realtime

Game state luôn được ghi database trước khi phát thông báo WebSocket. Client phải tải lại `GET /api/games/{id}` sau reconnect. Nếu Vercel scale nhiều instance, cần bổ sung pub/sub ngoài bộ nhớ (ví dụ Supabase Realtime Broadcast) trước khi quảng bá PvP đa instance.
