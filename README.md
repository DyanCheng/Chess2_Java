# Chess2 Java

Ứng dụng Chess2 viết bằng Java/Spring Boot: cờ PvE/PvP/local, HP, thẻ kỹ năng, shop, tiền tệ, puzzle và leaderboard. Source được tổ chức dạng monorepo; frontend tách khỏi server nhưng vẫn được đóng gói vào cùng một ứng dụng để triển khai.

## Cấu trúc

```
apps/
  server/                        # Spring Boot, API, JWT, domain và service
    src/main/java/com/chess2/
    src/main/resources/db/migration/  # Flyway migrations cho schema chess
  web/                           # player UI và cổng quản trị /admin/
    assets/                      # CSS, JavaScript, font và artwork
    pixel-chess-v1/              # sprite runtime kèm attribution
assets/source/                   # bộ Pixel chess v1.2 nguyên gốc
tests/e2e/                       # Playwright desktop/mobile
docs/                            # tài liệu dự án
Dockerfile                       # image dùng chung local, CI và Vercel
.github/workflows/ci.yml         # Maven verify, Playwright và Docker build
```

Phiên đăng nhập được lưu dưới khóa `chess2.session.v2`. Tài khoản khách dùng một `guestKey` ổn định theo trình duyệt, vì vậy bấm “Chơi nhanh” lần sau sẽ quay về đúng tài khoản cũ thay vì ghi đè/tạo tài khoản khác.

## Frontend và database

Frontend web nằm trong `apps/web/`. Maven copy thư mục này vào `static/` khi build và Spring Boot phục vụ tại `/`;
không cần chạy frontend server riêng. Giao diện dùng JWT API để đăng ký/đăng nhập, tạo ván PVE/PVP/local,
dùng HP và thẻ, quản lý kho/Deck trước trận, mua trong shop, xem nhật ký ván đấu, giải puzzle và xem leaderboard.

Database sử dụng Supabase PostgreSQL schema `chess`. Migration nằm ở
`apps/server/src/main/resources/db/migration/`; cấu hình Flyway đã hỗ trợ project CHESS đã khởi tạo schema
trước đó, đồng thời vẫn tự chạy migration trên database hoàn toàn mới.

## Pixel chess assets

Giao diện dùng các sprite runtime trong `apps/web/pixel-chess-v1/`; bộ nguồn gốc nằm tại `assets/source/`.
Asset gốc **Pixel chess v1.2** bởi **Dani Maccari** (https://dani-maccari.itch.io/),
được sử dụng theo tệp `LICENSE-AND-ATTRIBUTION.txt` đi kèm. Không tách riêng,
đóng gói lại hoặc phân phối lại bộ asset này.

Backend Java cho Arcane Chess mobile. Dự án thay thế demo dữ liệu trong bộ nhớ bằng Spring Boot, JWT, Spring Security, JPA/Hibernate và Supabase Postgres.

## Phạm vi hiện tại

- Đăng ký/đăng nhập bằng BCrypt và JWT; phân quyền `PLAYER`/`ADMIN`.
- PvE, PvP lobby, local game; server kiểm tra lượt và nước đi cơ bản, lưu board bền vững.
- HP và 15 hiệu ứng thẻ, kho thẻ, Deck tối đa 3 loại được snapshot theo ván, shop dùng gold, puzzle và bảng xếp hạng.
- Move Log lưu từng nước đi, thời gian, quân bị bắt, thẻ và hiệu ứng vào bảng `game_move_logs`; chỉ người tham gia ván được xem.
- Flyway tạo schema `chess` tách biệt khỏi bảng `public` của Supabase cũ.
- Docker Compose và Dockerfile cho Vercel container deployment.
- PvP realtime qua WebSocket có JWT handshake, kiểm tra quyền theo ván, Ping/Pong heartbeat, reconnect và REST recovery polling.

## Chạy cục bộ

### Demo tức thì, không cần Supabase

Yêu cầu JDK 17 trở lên; CI/CD và Docker image dùng Java 21.

```powershell
cd apps/server
mvn -Dspring-boot.run.profiles=demo spring-boot:run
```


mvn -f .\apps\server\pom.xml "-Dspring-boot.run.profiles=demo" "-Dspring-boot.run.arguments=--server.port=8088" spring-boot:run

Mở `http://localhost:8080`, tạo hai tài khoản để trình diễn quick-match PvP hoặc một tài khoản để chơi PvE.
Profile `demo` dùng H2 trong bộ nhớ, tự seed thẻ, shop và puzzle; dữ liệu được xóa khi dừng ứng dụng.

### Chạy với Supabase

1. Tạo file `.env` ở gốc repository và điền `SUPABASE_DB_URL`, `JWT_SECRET`. `docker compose` sẽ tự nạp file này; không commit secret.
2. Lấy chuỗi JDBC **Session pooler** từ Supabase Dashboard → **Connect** → **Session pooler** → **JDBC**; không dùng Project URL (`https://<project-ref>.supabase.co`) hay Transaction pooler cho Hibernate/JPA. Thêm `sslmode=require` nếu chuỗi chưa có.
3. Chạy `docker compose up --build`.
4. Health check: `http://localhost:8080/actuator/health`.

Build và chạy riêng image demo không cần Supabase:

```powershell
docker build -t chess2-java:local .
docker run --rm -e SPRING_PROFILES_ACTIVE=demo -p 8080:8080 chess2-java:local
```

Image chạy bằng Java 21 với UID/GID `10001`, không chứa source, test, `.env` hoặc bộ asset gốc.

Khi chạy `mvn spring-boot:run` trực tiếp, Spring Boot **không tự đọc file `.env`**. Hãy nạp `SUPABASE_DB_URL` và `JWT_SECRET` vào environment của terminal trước khi chạy, hoặc chạy bằng Docker Compose.

Hoặc chạy test không cần Supabase:

```powershell
cd apps/server
mvn verify
```

## Vercel

Vercel build từ gốc repository bằng `Dockerfile` dùng chung với local và CI. Image build `apps/server`, đóng gói `apps/web` vào JAR và chạy bằng Java 21. `vercel.json` đặt framework là `container` để toàn bộ traffic được route đến Spring Boot. Cấu hình Environment Variables cho Preview và Production:

- `SUPABASE_DB_URL`: JDBC Session pooler có `sslmode=require`.
- `JWT_SECRET`: secret Base64 tối thiểu 32 byte.
- `ADMIN_USERNAME`, `ADMIN_EMAIL`, `ADMIN_PASSWORD`: tài khoản quản trị được tạo hoặc nâng quyền khi khởi động. Mật khẩu phải có ít nhất 12 ký tự; không commit giá trị thật vào Git.
- `CORS_ALLOWED_ORIGINS`: URL Vercel của mobile-web/demo, phân tách bằng dấu phẩy.
- `DB_POOL_SIZE`: mặc định `4`.
- `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`, `MAIL_FROM`: SMTP cho email quên mật khẩu.
- `PUBLIC_APP_URL`: URL public của app, ví dụ `https://chess2java.vercel.app/`.

Sau khi thay đổi biến môi trường, cần tạo deployment mới; biến không áp dụng ngược cho deployment cũ. Kết nối GitHub repository trong Vercel để mỗi push/PR tạo deployment Preview và push vào `main` tạo Production. GitHub Actions thực hiện Maven verify và Docker build trước; Vercel là lớp CD.

WebSocket hub hiện lưu subscription trong từng Spring instance. Cấu hình này phù hợp local/demo và vẫn có REST recovery tối đa 2 giây; trước khi chạy nhiều instance hoặc kiểm thử 10.000 CCU, cần thay tín hiệu nội bộ bằng shared pub/sub để mọi instance cùng nhận sự kiện trận đấu.

Không đặt database password hoặc JWT secret trong GitHub Actions, `.env.example`, source code hay mobile client.

## Supabase migration

Migration nằm tại `apps/server/src/main/resources/db/migration/`. Spring Boot sẽ chạy migration ở schema `chess` khi khởi động với quyền database phù hợp. Kiểm tra đúng project Supabase trước khi khởi động Production: schema này độc lập với `public` của app React Native cũ.

## Lưu ý realtime

Client dùng native WebSocket `/ws/games` để nhận tín hiệu thay đổi theo thời gian thực, với JWT handshake guard, quyền theo người tham gia ván, Ping/Pong heartbeat và reconnect. Sau tín hiệu, client tải trạng thái authoritative bằng `GET /api/games/{id}`; REST polling 2 giây chỉ là lớp phục hồi. Chưa tuyên bố độ trễ dưới 50 ms hoặc khả năng đa instance cho tới khi có shared pub/sub và load test trên staging.

Trạng thái kiểm chứng của các yêu cầu bảo mật, ACID, fault tolerance và khả năng mở rộng nằm tại [`docs/non-functional-requirements.md`](docs/non-functional-requirements.md).
