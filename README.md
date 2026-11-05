# Chess² — Chess app

A Java 21 / Spring Boot application with a browser-based chess interface and a
REST API for demo accounts, games against a basic bot or another player on the
same device, and an ELO leaderboard. Online multiplayer is not included.

## Run

Install JDK 21 and Maven 3.9+, then from the repository root run:

```powershell
cd backend
mvn spring-boot:run
```

Open `http://localhost:8080` in your browser to use the chess interface. The API
is available under `http://localhost:8080/api`. Data is kept in memory and
resets when the server stops. Passwords are hashed; accounts and bearer tokens
are intended for local demos, not production authentication.

## Run with Docker

With Docker Desktop (or Docker Engine and the Compose plugin) running, execute
this command from the repository root:

```powershell
docker compose up --build
```

Open `http://localhost:8082` in your browser to use the chess interface. The API
is available under `http://localhost:8082/api`. Compose maps to host port 8082
by default; to choose another port in PowerShell, set `$env:CHESS2_PORT` before
running Compose. Press `Ctrl+C` to stop it, or run `docker compose down` from
the repository root. Data is kept in memory and resets when the container
stops.

## Main endpoints

- `POST /api/auth/register` and `POST /api/auth/login` with
  `{"username":"player1","password":"secret1"}`
- `GET /api/me` with `Authorization: Bearer <token>`
- `GET /api/lobby` lists available match modes (`BOT`, `LOCAL`)
- `POST /api/matches` with `{"mode":"BOT"}` or `{"mode":"LOCAL"}`
- `GET /api/matches/{id}` gets the board, pieces, turn, and move history
- `POST /api/matches/{id}/moves` with `{"from":"e2","to":"e4"}`
- `POST /api/matches/{id}/resign` ends the current game
- `GET /api/leaderboard` lists players by ELO

For `LOCAL`, submit moves alternately for White and Black. `BOT` plays a
simple random move, preferring captures. The demo chess engine supports piece
movement and queen promotion, but does not implement check, checkmate,
castling, or en passant.

## Test

```powershell
cd backend
mvn test
```
chạy maven $env:JAVA_HOME = [Environment]::GetEnvironmentVariable('JAVA_HOME', 'User')
$env:MAVEN_HOME = [Environment]::GetEnvironmentVariable('MAVEN_HOME', 'User')
$env:Path = "$env:MAVEN_HOME\bin;$env:JAVA_HOME\bin;$env:Path"
mvn spring-boot:run

```
chạy ứng dụng trên cổng 8083:
$env:SERVER_PORT = '8083'
mvn spring-boot:run
```

## Docker, Redis, and Railway deployment

If you do not already have a local `.env`, copy `.env.example` to `.env` for
Compose overrides; do not overwrite an existing file. Then run
`docker compose up --build` from the repository root. Compose starts the
backend and Redis only; Supabase is an external service and is not included.
The Redis password is optional locally and can be set with `REDIS_PASSWORD`.
The leaderboard cache uses `CACHE_TTL` (default `10m`). Account, bearer-token,
and game data remain in memory and are not persisted by Redis.

The GitHub Actions workflow runs tests, packages the JAR, builds the Docker
image, and deploys on pushes to `main` (pull requests only run validation).
Before enabling deployments, add the `RAILWAY_TOKEN` repository secret and
the `RAILWAY_SERVICE_ID`, `RAILWAY_ENVIRONMENT_ID`, and
`BACKEND_HEALTH_URL` repository variables under **Settings → Secrets and
variables → Actions**. The Railway service must build from `backend/` and use
its `Dockerfile`; configure `REDIS_HOST`, `REDIS_PORT`, and optional
`REDIS_PASSWORD` to point at a Railway-accessible Redis service, and set
`SPRING_CACHE_TYPE=redis` and `CACHE_TTL=10m`.

This repository currently has no PostgreSQL driver or persistence layer: its
accounts and games are in-memory demo data. Supabase variables alone do not
make the application use Supabase. Do not deploy as a persistent production
backend until a database model and persistence design are explicitly approved
and implemented. The browser UI is served by Spring Boot from
`backend/src/main/resources/static`; there is no separate Vercel frontend to
configure or deploy. The app has no WebSocket endpoint.