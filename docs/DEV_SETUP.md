# Developer Setup Guide

This guide expands on the README with practical steps for running LinkGrove locally, configuring environment variables, and enabling optional features like Geo analytics and tracing.

## 1. Prerequisites

- Docker + Docker Compose plugin (for the bundled stack)
- Java 21 (Temurin recommended) if running the backend locally
- Node 18+ if running the frontend locally
- Optional: `jq`, `curl`, `openssl` for scripting

## 2. Environment Files

### 2.1 Docker Compose (`infra/docker/.env`)

The helper scripts `infra/docker/start.sh` and `stop.sh` read variables from `infra/docker/.env`. Copy the template below to get started:

```env
# Database
POSTGRES_PORT=5432
POSTGRES_USER=linkgrove
POSTGRES_PASSWORD=linkgrove
POSTGRES_DB=linkgrove

# Redis & RabbitMQ
REDIS_PORT=6379
RABBIT_PORT=5672

# App URLs
PUBLIC_BASE_URL=http://localhost:3001
CORS_ALLOWED_ORIGINS=http://localhost:3001

# JWT secrets (override in production)
JWT_SECRET=change-me-please-for-local-testing
```

> Tip: Secrets set in this file are exported into the container environment when you run `bash infra/docker/start.sh`.

### 2.2 Backend (`backend/.env`)

When running `./mvnw spring-boot:run` without Docker, create `backend/.env` (or export variables in your shell):

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/linkgrove
SPRING_DATASOURCE_USERNAME=linkgrove
SPRING_DATASOURCE_PASSWORD=linkgrove
SPRING_DATA_REDIS_HOST=localhost
SPRING_RABBITMQ_HOST=localhost

JWT_SECRET=$(openssl rand -hex 64)
PUBLIC_BASE_URL=http://localhost:3001
CORS_ALLOWED_ORIGINS=http://localhost:3001

# Optional observability
OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317
MANAGEMENT_TRACING_SAMPLING_PROBABILITY=1.0

# Optional Geo analytics
GEOIP_ENABLED=false
GEOIP_DB_PATH=/absolute/path/to/GeoLite2-Country.mmdb
```

Load the file before starting the backend:

```bash
cd backend
export $(grep -v '^#' .env | xargs)
./mvnw spring-boot:run
```

### 2.3 Frontend (`frontend/.env.local`)

Create `frontend/.env.local` to point the React app at the backend:

```env
REACT_APP_API_URL=http://localhost:8080/api
```

## 3. Running the stack

### 3.1 Docker Compose (recommended)

```bash
bash infra/docker/start.sh   # boots Postgres, Redis, RabbitMQ, backend, frontend
# App: http://localhost:3001 | API proxied at http://localhost:3001/api

bash infra/docker/stop.sh    # stop and clean (removes volumes)
```

`start.sh` sources `infra/docker/.env`, so update that file whenever you need to change ports or secrets.

### 3.2 Local services

1. Start infrastructure (Docker Compose, or your own Postgres/Redis/RabbitMQ).
2. Backend: `./mvnw -B -DskipITs=false test` then `./mvnw spring-boot:run`.
3. Frontend: `REACT_APP_API_URL=http://localhost:8080/api npm start`.

## 4. Optional Features

### 4.1 Geo Analytics Enablement

1. Download MaxMind GeoLite2 Country DB (requires free account):
   ```bash
   curl -L -o infra/geo/GeoLite2-Country.mmdb \
     "https://download.maxmind.com/app/geoip_download?edition_id=GeoLite2-Country&license_key=<KEY>&suffix=tar.gz"
   tar -xzf GeoLite2-Country.mmdb.tar.gz --strip-components=1 -C infra/geo
   ```
2. Set `GEOIP_ENABLED=true` and `GEOIP_DB_PATH=/abs/path/to/GeoLite2-Country.mmdb` in your environment.
3. Restart the backend and watch logs for `GeoIP database loaded`.
4. Run a smoke test (e.g., Playwright `npm test -- geo-analytics`) to verify daily geo aggregates update.

### 4.2 Tracing quickstart

Follow `docs/OTLP_TRACING.md` to run Jaeger locally and configure `OTEL_EXPORTER_OTLP_ENDPOINT` / `MANAGEMENT_TRACING_SAMPLING_PROBABILITY` variables. The backend will emit spans automatically once those env vars are set.

## 5. Testing Cheat Sheet

| Area      | Command |
|-----------|---------|
| Backend unit/integration | `cd backend && ./mvnw -B -P it verify`
| Frontend build & lint     | `cd frontend && CI=true npm run build`
| Playwright E2E            | `cd e2e && npm ci && npm test`
| GitHub Actions (CI)       | `.github/workflows/ci.yml` (see workflow for full matrix)

> The CI workflow uploads backend jars, frontend builds, and Playwright reports as artifacts. Our `lint` job ensures `backend/target`, `frontend/build`, and `*.log` files never land in git.

## 6. Troubleshooting

- **Backend fails to start with tracing enabled**: ensure only one OTLP propagator bean exists (`TracingConfig` now marks ours as `@Primary`).
- **Geo analytics shows null countries**: confirm the GeoLite DB exists at `GEOIP_DB_PATH` and the IPs you test are public (not `192.168.x.x`).
- **Docker start script exits immediately**: run `bash -x infra/docker/start.sh` and check missing envs; compose logs are in `backend.log` or `frontend.log`.
- **Playwright flake**: rerun with `DEBUG=pw:api npm test` to capture verbose browser logs; the CI job uploads reports automatically.

For deeper tracing, see `docs/OTLP_TRACING.md`. For CI details, check the badge in `README.md` or GitHub Actions -> `CI` workflow.

