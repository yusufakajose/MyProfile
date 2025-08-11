# LinkGrove (MyProfile)

[![CI](https://github.com/yusufakajose/MyProfile/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/yusufakajose/MyProfile/actions/workflows/ci.yml)

LinkGrove is a full‑stack link profiles and analytics platform. Create a public profile with links, generate QR codes (PNG/SVG) with caching and ETags, and view analytics by referrer, device, country, source, and A/B variants. Built with Spring Boot 3 + Java 21 on the backend and React + Nginx on the frontend, backed by PostgreSQL, Redis, and RabbitMQ.

## Features

- QR codes: PNG/SVG, custom colors, logo overlay, robust caching (ETag/304), HEAD support
- Public profiles: SEO meta for social previews, SPA routing via Nginx
- Analytics: referrers, devices, countries, sources, per‑link and per‑variant timeseries
- Rate limiting: sliding window with `X-RateLimit-*` headers and 429 handling
- Webhooks: delivery + retry with DLQ tracking
- Security: JWT auth, role‑based endpoints, centralized error handling
- CI/CD: GitHub Actions, pinned container images, Testcontainers‑based tests

## Tech stack

- Backend: Java 21, Spring Boot 3, Spring Data JPA, Flyway, Micrometer
- Data: PostgreSQL, Redis, RabbitMQ
- Frontend: React, Material‑UI, Nginx
- Tooling: Docker/Compose, Testcontainers, GitHub Actions

## Quick start (Docker Compose)

Prereqs: Docker and the Compose plugin

```bash
bash infra/docker/start.sh
```

- App: http://localhost:3001
- API proxied at: `http://localhost:3001/api`
- Stop & remove containers/volumes:

```bash
bash infra/docker/stop.sh
```

Default demo accounts (seeded):

- Admin: `admin` / `admin123`
- Demo: `demo` / `password`

## Configuration

You can override configuration via environment variables (recommended using `infra/docker/.env`). Key variables:

- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- `SPRING_REDIS_HOST`, `SPRING_REDIS_PORT`
- `SPRING_RABBITMQ_HOST`, `SPRING_RABBITMQ_PORT`
- `JWT_SECRET` (required in production)
- `CORS_ALLOWED_ORIGINS` (e.g. `http://localhost:3001`)
- `PUBLIC_BASE_URL` (used for public URLs/meta)
- `REACT_APP_API_URL` (frontend build‑time; use `/api` with Nginx proxy)

See `backend/src/main/resources/application.yml` and `infra/docker/docker-compose.yml` for defaults.

## Development (without Docker)

Backend:

```bash
cd backend
./mvnw -B -DskipITs=false test
./mvnw spring-boot:run
# API at http://localhost:8080/api
```

Frontend:

```bash
cd frontend
REACT_APP_API_URL=http://localhost:8080/api npm start
# App at http://localhost:3000
```

## Testing & build

- Backend unit/integration tests (Testcontainers):

```bash
cd backend
./mvnw -B -DskipITs=false test
```

- Frontend build (ESLint enforced):

```bash
cd frontend
CI=true npm run build
```

## API

Notable endpoints:

- Auth: `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/health`
- Public profile: `GET /api/public/{username}`, `GET /api/public/meta/{username}`
- Links (auth): `GET /api/links`, `POST /api/links`, updates/deletes
- Analytics (auth): `GET /api/analytics/...` (overview, timeseries, referrers, devices, countries, sources, variants)
- Redirect/QR: `/r/{id}`, `/r/a/{alias}`, `/r/{id}/qr.png|svg` (+ HEAD)

## Observability

- Health: `GET /api/auth/health`
- Metrics (Prometheus): `GET /actuator/prometheus`

## CI/CD

- GitHub Actions workflow at `.github/workflows/ci.yml`
- Pinned container images across Dockerfiles/Compose/Testcontainers for reproducible builds

## Troubleshooting

- 502 via Nginx proxy: ensure backend is healthy; `docker compose ps` and retry
- Redis connection errors: start Redis service; if you upgraded Redis and see `Can't handle RDB format`, run `bash infra/docker/stop.sh` to remove volumes and restart (this wipes Redis data)
- Frontend build warnings failing CI: run `CI=true npm run build` locally to reproduce and fix ESLint

## Roadmap & contributing

- See `ROADMAP.md` for planned work
- Contributions welcome: open an issue/PR. Please keep code clear and tested.

## License

Copyright (c) 2025. All rights reserved. If you intend to open‑source this project, add a `LICENSE` file (e.g., MIT) and update this section.

