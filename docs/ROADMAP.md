# LinkGrove – Canonical Roadmap

- Legend: [x] done, [ ] pending, [~] in progress
- This file supersedes ad-hoc plans across the repo (e.g., PHASE6_PLAN.md). Keep this as the single source of truth.

## Backend
- [x] JWT auth, roles; secured analytics endpoints
- [x] Link CRUD + reorder; alias; schedule (start/end); active toggle; tags (M:N) + filtering/search/pagination/sort
- [x] Redirects: support `/r/{id}` and custom alias `/r/a/{alias}`; enforce schedule + active state
- [x] Analytics pipeline: RabbitMQ click events → daily aggregates (clicks, unique)
- [x] Unique visitors de-dup (Redis sets with TTL)
- [x] Referrer/device daily aggregates + APIs
- [x] Analytics APIs: summary, timeseries, per-link timeseries, top-links, referrers, devices
- [x] CSV exports: timeseries, top-links, referrers, devices
- [x] CSV exports: per-link timeseries and per-link variants (to mirror UI)
- [x] Rate limiting for public routes with headers (limit/remaining/window, Retry-After) + admin metrics endpoint
- [x] Programmatic DB indexes at boot (`IndexInitializer`) for links and aggregates
- [x] Webhooks: config + HMAC-SHA256 signing, delivery logs, resend; scheduled retries with exponential backoff, DLQ listing and resend-all
- [x] Public profile 404 handling
- [~] Flyway: enabled with baseline; continue adding migrations for new features
- [~] Geo analytics (country) daily aggregates + APIs + CSV (enabled when `GEOIP_ENABLED=true` and DB mounted)
- [x] A/B tests & weighted rotation for links (link variants with weights)
- [ ] Custom domains (starter version: domain mapping + host-based routing)
- [x] Observability: structured JSON logs + request IDs; health/metrics (Actuator + Prometheus)
  - [x] Add distributed trace IDs (OTLP to Jaeger) and document
- [ ] Testability: Testcontainers integration tests (Postgres+Redis+RabbitMQ), contract tests, smoke tests
- [ ] Admin UX APIs: webhook failures view, queues, rate-limit overviews beyond raw counters

### Deferred/Backlog (from recent work)
- [ ] Document HEAD support for QR endpoints in backend README and API summary
  - Describe HEAD /r/{id}/qr.png and /r/{id}/qr.svg (and alias variants)
  - Headers returned: ETag, Cache-Control, X-RateLimit-*
  - 200 vs 304 behavior with If-None-Match
- [ ] Allowlist logo hostnames for QR logos (hardening)
  - Config: qr.logo.allowedHosts (comma-separated), qr.logo.allowedTlds (optional)
  - Enforce alongside https/content-type/size checks; return structured 400 JSON on violation
  - Unit + integration tests for allowed vs blocked hosts
- [x] Deterministic 429 test for QR endpoints (low-threshold profile)
  - Override ratelimit in test properties; assert 429 + Retry-After reliably
- [x] Frontend: per-link Sources CSV export button wiring
  - Implement download logic for per-link Sources export in `AnalyticsDashboard.js`
- [ ] Nginx: expand social bot UA map
  - Add additional known bot UAs and verify routing to meta endpoint

## Frontend
- [x] Auth flow (login/register), token storage, axios interceptor, 401 redirect guard
- [x] Protected routes via `ProtectedRoute`
- [x] Link Manager: create, edit, delete, drag/drop reorder, activate/deactivate, alias, schedule, tags; search, status filter, sort; CSV export
- [x] Analytics Dashboard: summary, timeseries (incl. per-link), top links, referrers/devices charts + CSV exports
- [x] Public Profile page with theme colors; friendly error state; click tracking
- [x] Webhook Settings: configure URL/active; list recent + DLQ; resend / resend-all
- [x] Centralize API base URL via `REACT_APP_API_URL` (interceptor + silent refresh wired; default to relative `/api`)
- [x] Minimal public layout variant (hide dashboard header on public routes)
- [x] Theming presets (light/dark); later reintroduce non-buggy banner
- [x] Better empty/error/skeleton states across analytics + public profile
  - [x] Public profile: retryable error state and empty state
  - [x] Analytics dashboard: retryable error state + empty state guidance
- [ ] Admin view (rate-limit metrics, webhook failures, geo status)

## Data & Migrations
- [x] Boot-time index initialization (dev convenience)
- [x] Re-enable Flyway with baseline for existing DB (baseline-version=0); add migrations through V15
- [ ] Validate indexes with EXPLAIN on key queries and lock in via migration

## Rate Limiting & Security
- [x] Sliding-window limiter (Redis ZSET) for `/r/**` and `/api/public/**` with headers and admin metrics
- [ ] Per-route limits as config; surfaced in admin UI; unify X-RateLimit-* headers for QR/public
- [ ] Hardened input validation across controllers (`@Valid` + constraints)
  - [x] Dev endpoints hardening: default dev token endpoints OFF; conditional permit based on env
  - [ ] QR logo host allowlist

## Webhooks
- [x] `link.click` event dispatch with signed HMAC
- [x] Delivery logs with payload capture
- [x] Manual resend; scheduled retries (exponential backoff), DLQ list & resend-all
- [ ] Add jitter to retry delays; cap attempts per destination/day; poison-queue guard
- [ ] Signed timestamp + replay protection window on receiver docs

## DevEx / CI/CD
- [x] Git initialized; `.gitignore` excludes build artifacts/env/IDE
- [ ] CI (build → test → package Docker images for backend/frontend)
- [ ] Docs: Update root README CI badge URL (replace OWNER/REPO after pushing to GitHub)
- [x] Docker: serve frontend via Nginx; CSP nonce injection; API proxied to backend
  - [ ] Pass API URL via env consistently (`REACT_APP_API_URL`) and verify CORS for non-proxy setups
- [ ] Developer docs: local `.env` setup; `start.sh` reads env; troubleshooting
  - [ ] Add GeoIP setup docs (`GEOIP_ENABLED`, `GEOIP_DB_PATH`) and compose volume mount
  - [ ] Default `ENABLE_DEV_TOKEN_ENDPOINTS` to false in compose

## Testing
- [ ] Integration tests: public profile fetch, click tracking, cache behavior, analytics endpoints (JWT + 401/403)
- [ ] Worker tests with Testcontainers for daily upserts + unique visitor increments
- [ ] Frontend tests for Link Manager, Analytics Dashboard, Webhook Settings
- [ ] Rate limiter tests; Flyway migration tests; QR low-threshold 429 test

## Cleanup
- [ ] Remove temporary `/member-login` alias; provide canonical login route
  - [ ] Update e2e and components to use canonical route
  - [ ] Stop tracking Playwright HTML reports; ignore `e2e/playwright-report/`
- [ ] 404 route and redirect unknown `/u/:username` to friendly page

---

## Prioritized Next Steps (Easy wins first)
1) Flyway verification (post-baseline)
   - [x] Verify boot with clean DB and pre-existing DB (baseline=0) in docker-compose
2) Frontend polish
   - [x] Use minimal layout for public profile routes (header hidden already; extract layout)
   - [x] Sweep for any remaining absolute URLs; default to relative `/api` in dev
3) Observability basics
   - [ ] Add distributed tracing to Jaeger; document endpoints and correlation via `X-Request-Id`
   - [x] Propagate `X-Request-Id` from Nginx to backend for end-to-end correlation
   - [x] Gate HSTS to HTTPS only in Nginx
4) Tests (incremental)
   - [ ] Worker upsert happy-path with Testcontainers
   - [x] Analytics endpoints + QR headers integration tests stable in CI
5) CI bootstrap
   - [ ] GitHub Actions: backend build + tests; frontend build; artifact upload
   - [ ] Lint to ensure reports and build artifacts aren’t committed
6) Geo analytics (starter)
   - [ ] Enable via env + mount; add basic e2e
   - [x] Expose `/api/admin/metrics/geo` (reports GeoIP enabled flag)

---

## TODO: Geo analytics follow-ups (local dev + ops)
- [ ] Local enablement and docs
  - [ ] Provide script to fetch MaxMind GeoLite2 Country DB and set env (Fish + Bash): `GEOIP_ENABLED=true`, `GEOIP_DB_PATH=/abs/path/GeoLite2-Country.mmdb`
  - [ ] On boot, log a clear warning and expose actuator info if GeoIP is disabled
- [ ] Admin/metrics
  - [x] Add `/api/admin/metrics/geo` to report GeoIP status (enabled flag)
  - [ ] Extend with last processed click time and top countries (last 24h)
- [ ] Simulation utilities
  - [ ] CLI script to generate sample clicks using `X-Forwarded-For` with well-known public IPs (e.g., 8.8.8.8, 1.1.1.1) via `/r/{id}` or `/r/a/{alias}`
  - [ ] Note: LAN/private IPs (e.g., 192.168.x.x) won’t resolve to a country
- [ ] Worker reliability
  - [ ] Ensure RabbitMQ is started in dev and add a smoke test that verifies geo aggregates increment after a simulated click
  - [ ] Add a health indicator/metric for the analytics worker (queue depth, last consume timestamp)

## Notes
- Historical phase documents (PHASE2/3/5/6) and standards remain as archives/reference. Update only this `ROADMAP.md` going forward.
