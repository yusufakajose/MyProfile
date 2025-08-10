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
- [ ] CSV exports: per-link timeseries and per-link variants (to mirror UI)
- [x] Rate limiting for public routes with headers (limit/remaining/window, Retry-After) + admin metrics endpoint
- [x] Programmatic DB indexes at boot (`IndexInitializer`) for links and aggregates
- [x] Webhooks: config + HMAC-SHA256 signing, delivery logs, resend; scheduled retries with exponential backoff, DLQ listing and resend-all
- [x] Public profile 404 handling
- [~] Flyway: enabled with baseline; continue adding migrations for new features
- [ ] Geo analytics (country/region) daily aggregates + APIs + CSV
- [x] A/B tests & weighted rotation for links (link variants with weights)
- [ ] Custom domains (starter version: domain mapping + host-based routing)
- [~] Observability: structured logs + health/metrics done (Actuator + Prometheus); add trace IDs
- [ ] Testability: Testcontainers integration tests (Postgres+Redis+RabbitMQ), contract tests, smoke tests
- [ ] Admin UX APIs: webhook failures view, queues, rate-limit overviews beyond raw counters

## Frontend
- [x] Auth flow (login/register), token storage, axios interceptor, 401 redirect guard
- [x] Protected routes via `ProtectedRoute`
- [x] Link Manager: create, edit, delete, drag/drop reorder, activate/deactivate, alias, schedule, tags; search, status filter, sort; CSV export
- [x] Analytics Dashboard: summary, timeseries (incl. per-link), top links, referrers/devices charts + CSV exports
- [x] Public Profile page with theme colors; friendly error state; click tracking
- [x] Webhook Settings: configure URL/active; list recent + DLQ; resend / resend-all
- [~] Centralize API base URL via `REACT_APP_API_URL` (client in place; env added; removed hardcoded usage in key components)
- [ ] Minimal public layout variant (hide dashboard header on public routes)
- [ ] Theming presets (light/dark); later reintroduce non-buggy banner
- [ ] Better empty/error/skeleton states across analytics + public profile
- [ ] Admin view (rate-limit metrics, webhook failures)

## Data & Migrations
- [x] Boot-time index initialization (dev convenience)
- [ ] Re-enable Flyway with baseline for existing DB; add migrations for links/tags/aggregates/webhooks/indexes
- [ ] Validate indexes with EXPLAIN on key queries and lock in via migration

## Rate Limiting & Security
- [x] Sliding-window limiter (Redis ZSET) for `/r/**` and `/api/public/**` with headers and admin metrics
- [ ] Per-route limits as config; surfaced in admin UI
- [ ] Hardened input validation across controllers (`@Valid` + constraints)

## Webhooks
- [x] `link.click` event dispatch with signed HMAC
- [x] Delivery logs with payload capture
- [x] Manual resend; scheduled retries (exponential backoff), DLQ list & resend-all
- [ ] Add jitter to retry delays; cap attempts per destination/day; poison-queue guard
- [ ] Signed timestamp + replay protection window on receiver docs

## DevEx / CI/CD
- [x] Git initialized; `.gitignore` excludes build artifacts/env/IDE
- [ ] CI (build → test → package Docker images for backend/frontend)
- [ ] Docker: serve frontend via Nginx; pass API URL via env; verify CORS; one-command compose
- [ ] Developer docs: local `.env` setup; `start.sh` reads env; troubleshooting

## Testing
- [ ] Integration tests: public profile fetch, click tracking, cache behavior, analytics endpoints (JWT + 401/403)
- [ ] Worker tests with Testcontainers for daily upserts + unique visitor increments
- [ ] Frontend tests for Link Manager, Analytics Dashboard, Webhook Settings
- [ ] Rate limiter tests; Flyway migration tests

## Cleanup
- [ ] Remove temporary `/member-login` alias; provide canonical login route
- [ ] 404 route and redirect unknown `/u/:username` to friendly page

---

## Prioritized Next Steps (Easy wins first)
1) Re-enable Flyway safely
   - [ ] Add baseline migration for existing schema; include webhooks/aggregates/tags indexes
   - [ ] Set `spring.jpa.hibernate.ddl-auto=validate`, `spring.flyway.enabled=true`
   - [ ] Verify boot with clean DB and with existing DB (baseline)
2) Frontend polish
   - [ ] Use minimal layout for public profile routes
   - [ ] Sweep for any remaining absolute URLs; ensure `REACT_APP_API_URL` used everywhere
3) Observability basics
   - [ ] Structured JSON logs; request/trace IDs; health/liveness endpoints documented
4) Tests (incremental)
   - [ ] Worker upsert happy-path with Testcontainers
   - [ ] Analytics endpoints 200/401/403 integration tests
5) CI bootstrap
   - [ ] GitHub Actions: backend build + tests; frontend build; artifact upload
6) Geo analytics (starter)
   - [ ] IP → country via lightweight DB/service; daily aggregate and charts/CSV

---

## TODO: Geo analytics follow-ups (local dev + ops)
- [ ] Local enablement and docs
  - [ ] Provide script to fetch MaxMind GeoLite2 Country DB and set env (Fish + Bash): `GEOIP_ENABLED=true`, `GEOIP_DB_PATH=/abs/path/GeoLite2-Country.mmdb`
  - [ ] On boot, log a clear warning and expose actuator info if GeoIP is disabled
- [ ] Admin/metrics
  - [ ] Add `/api/admin/metrics/geo` to report GeoIP status, last processed click time, and top countries (last 24h)
- [ ] Simulation utilities
  - [ ] CLI script to generate sample clicks using `X-Forwarded-For` with well-known public IPs (e.g., 8.8.8.8, 1.1.1.1) via `/r/{id}` or `/r/a/{alias}`
  - [ ] Note: LAN/private IPs (e.g., 192.168.x.x) won’t resolve to a country
- [ ] Worker reliability
  - [ ] Ensure RabbitMQ is started in dev and add a smoke test that verifies geo aggregates increment after a simulated click
  - [ ] Add a health indicator/metric for the analytics worker (queue depth, last consume timestamp)

## Notes
- Historical phase documents (PHASE2/3/5/6) and standards remain as archives/reference. Update only this `ROADMAP.md` going forward.
