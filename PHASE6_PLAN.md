## Phase 6 Plan — Next Steps and Status

- **Legend**: [x] done, [ ] pending, [~] in progress/blockers

### Backend
- [x] Persist daily aggregates for timeseries (`link_click_daily_aggregate`), JPA repo, UPSERT
- [x] Worker upserts aggregates on click (RabbitMQ consumer)
- [x] `/api/analytics/dashboard/timeseries?days=N` returns contiguous series from DB
- [x] JWT security with roles; secured `/api/analytics/**` for `ROLE_USER`
- [x] Admin-only route `/api/admin/health` (requires `ROLE_ADMIN`)
- [x] Redis-based rate limiting for `/r/**` and `/api/public/**` (sliding window)
- [x] Idempotent public click tracking (per link+IP, 5s)
- [x] Controllers rely on `GlobalExceptionHandler`; removed catch‑all blocks
- [x] Flyway added with migration `V1__create_analytics_tables.sql`
- [~] Flyway runtime stabilization: resolve "Unsupported Database" and baseline errors
  - [ ] Verify `flyway-database-postgresql` is on runtime classpath
  - [ ] Confirm `spring.flyway.baseline-on-migrate=true` is in effect when starting via `mvn spring-boot:run`
  - [ ] If schema pre-exists, either add `V0__baseline.sql` or set `baseline-version` explicitly
- [ ] Secure `LinkController` with Spring Security (replace `X-Username` header), align with roles
- [ ] Add richer analytics: unique visitors (session/IP de-dupe), referrers, devices
- [ ] Add DB indexes for analytics queries (username, day) — validate with `EXPLAIN`
- [ ] Backfill job to compute aggregates from historical clicks (optional)

#### Exports (planned)
- [ ] Add CSV export endpoints to mirror per-link analytics views:
  - `/api/analytics/export/timeseries/by-link?linkId=...&days=...`
  - `/api/analytics/export/variants/by-link?linkId=...&days=...`
  - Ensure results match UI tables and include headers; reuse existing service methods.

### Frontend
- [ ] Auth flow: login/register UI, token storage, axios interceptor, 401 handling
- [ ] Gate analytics routes behind auth; redirect unauthenticated users to login
- [ ] Configure API base URL via env (development, docker, production)
- [ ] Timeseries date range picker (custom from/to), export CSV
- [ ] Admin-only view (simple system health page)

### Observability & Hardening
- [ ] Structured JSON logging; trace correlation for request → worker → DB
- [ ] Add rate-limit headers (X-RateLimit-Remaining, Retry-After) to public endpoints
- [ ] Input validation review (`@Valid` + constraints) across controllers
- [ ] Secret management: move JWT secret, DB creds to env/compose

### Testing
- [ ] Integration tests for secured analytics with JWT (happy path + 401/403)
- [ ] Worker test: click event → link count + daily aggregate upsert
- [ ] Rate limiter tests (allow, burst, block, expiry)
- [ ] Flyway migration test (clean DB → migrate → app boots)

### CI/CD & DevEx
- [ ] Docker-compose: end-to-end stack (backend, frontend, postgres, redis, rabbitmq)
- [ ] Make targets/scripts for dev: `make up`, `make logs`, `make seed`
- [ ] GitHub Actions: build, test, package, docker images, smoke tests

### Data & Migrations
- [x] `V1__create_analytics_tables.sql` (daily aggregates table + indexes)
- [ ] Add `V2__indexes.sql` (username+day composite, user/day only)
- [ ] Baseline strategy for existing DBs (documented + automated)

### Documentation
- [ ] Update README for security (how to obtain token), rate limiting behavior, and env vars
- [ ] API docs for analytics and admin routes (request/response, auth requirements)
- [ ] Runbooks: resolving Flyway issues, resetting caches, replaying DLQ

---

### Immediate Blockers (Top Priority)
1) Flyway boot errors
- Action: start via packaged JAR (ensures same classpath), confirm `flyway-database-postgresql` present, baseline existing schema.
- Fallback: temporarily set `spring.flyway.enabled=false` to unblock dev, then re‑enable after baseline.

2) Replace `X-Username` headers with authenticated principal in `LinkController`
- Action: migrate endpoints to use `Authentication` and authorize by role.

3) Frontend auth wiring
- Action: login/register pages, token storage, axios interceptor, route guards.

---

### Quick Commands (reference)
- Build JAR: `cd backend && ./mvnw -DskipTests clean package`
- Run JAR: `java -jar backend/target/linkgrove-api-0.0.1-SNAPSHOT.jar`
- Register user: `POST /api/auth/register`
- Login: `POST /api/auth/login` → use `Authorization: Bearer <token>` for analytics routes
