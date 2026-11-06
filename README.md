# Chess2 Java

Ứng dụng Chess2 viết lại bằng Java 21 và Spring Boot: cờ PvE/PvP/local, HP, thẻ kỹ năng, shop, tiền tệ, puzzle và leaderboard. Frontend được Spring Boot phục vụ trực tiếp, nên chỉ có một ứng dụng để triển khai.

## Cấu trúc

```
backend/                         # Vercel Root Directory và ứng dụng triển khai
  src/main/java/com/chess2/      # API, bảo mật JWT, domain và service
  src/main/resources/
    db/migration/                # Flyway migrations cho schema chess
    static/
      index.html                 # khung trang
      assets/css/app.css         # giao diện responsive
      assets/js/                 # state, API client, render và điều phối UI
      pixel-chess-v1/            # sprite kèm attribution
docs/                            # tài liệu dự án
.github/workflows/ci.yml         # Maven verify và Docker build
```

Phiên đăng nhập được lưu dưới khóa `chess2.session.v2`. Tài khoản khách dùng một `guestKey` ổn định theo trình duyệt, vì vậy bấm “Chơi nhanh” lần sau sẽ quay về đúng tài khoản cũ thay vì ghi đè/tạo tài khoản khác.

## Frontend và database

Frontend web nằm trong `backend/src/main/resources/static/` và được Spring Boot phục vụ tại `/`;
không cần chạy frontend server riêng. Giao diện dùng JWT API để đăng ký/đăng nhập, tạo ván PVE/PVP/local,
dùng HP và thẻ, mua trong shop, giải puzzle và xem leaderboard.

Database sử dụng Supabase PostgreSQL schema `chess`. Migration nằm ở
`backend/src/main/resources/db/migration/`; cấu hình Flyway đã hỗ trợ project CHESS đã khởi tạo schema
trước đó, đồng thời vẫn tự chạy migration trên database hoàn toàn mới.

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

### Demo tức thì, không cần Supabase

Yêu cầu JDK 21 (cùng phiên bản với CI/CD và Docker image).

```powershell
cd backend
mvn -Dspring-boot.run.profiles=demo spring-boot:run
```

Mở `http://localhost:8080`, tạo hai tài khoản để trình diễn quick-match PvP hoặc một tài khoản để chơi PvE.
Profile `demo` dùng H2 trong bộ nhớ, tự seed thẻ, shop và puzzle; dữ liệu được xóa khi dừng ứng dụng.

### Chạy với Supabase

1. Tạo file `.env` ở gốc repository từ `.env.example` và điền `SUPABASE_DB_URL`, `JWT_SECRET`. `docker compose` sẽ tự nạp file này.
2. Lấy chuỗi JDBC **Session pooler** từ Supabase Dashboard → **Connect** → **Session pooler** → **JDBC**; không dùng Project URL (`https://<project-ref>.supabase.co`) hay Transaction pooler cho Hibernate/JPA. Thêm `sslmode=require` nếu chuỗi chưa có.
3. Chạy `docker compose up --build`.
4. Health check: `http://localhost:8080/actuator/health`.

Khi chạy `mvn spring-boot:run` trực tiếp, Spring Boot **không tự đọc file `.env`**. Hãy nạp `SUPABASE_DB_URL` và `JWT_SECRET` vào environment của terminal trước khi chạy, hoặc chạy bằng Docker Compose.

Hoặc chạy test không cần Supabase:

```powershell
cd backend
mvn verify
```

## Vercel

Trong Vercel, đặt **Root Directory = `backend`**. Vercel sẽ dùng `backend/Dockerfile`; không cần `vercel.json` hay Dockerfile ở gốc repository. Cấu hình Environment Variables cho Preview và Production:

- `SUPABASE_DB_URL`: JDBC Session pooler có `sslmode=require`.
- `JWT_SECRET`: secret Base64 tối thiểu 32 byte.
- `CORS_ALLOWED_ORIGINS`: URL Vercel của mobile-web/demo, phân tách bằng dấu phẩy.
- `DB_POOL_SIZE`: mặc định `4`.

Sau khi thay đổi biến môi trường, cần tạo deployment mới; biến không áp dụng ngược cho deployment cũ. Kết nối GitHub repository trong Vercel để mỗi push/PR tạo deployment Preview và push vào `main` tạo Production. GitHub Actions thực hiện Maven verify và Docker build trước; Vercel là lớp CD.

Không đặt database password hoặc JWT secret trong GitHub Actions, `.env.example`, source code hay mobile client.

## Supabase migration

Migration nằm tại `backend/src/main/resources/db/migration/`. Spring Boot sẽ chạy migration ở schema `chess` khi khởi động với quyền database phù hợp. Kiểm tra đúng project Supabase trước khi khởi động Production: schema này độc lập với `public` của app React Native cũ.

## Lưu ý realtime

Game state luôn được ghi database trước khi phát thông báo WebSocket. Client phải tải lại `GET /api/games/{id}` sau reconnect. Nếu Vercel scale nhiều instance, cần bổ sung pub/sub ngoài bộ nhớ (ví dụ Supabase Realtime Broadcast) trước khi quảng bá PvP đa instance.
