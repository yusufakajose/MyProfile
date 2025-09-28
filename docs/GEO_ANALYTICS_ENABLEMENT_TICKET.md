# Ticket: Geo Analytics Enablement

## Summary
Enable Geo analytics end-to-end so country-level metrics are accurate in hosted environments. This work covers configuration, test coverage, and operational guidance.

## Problem Statement
Geo aggregation is currently guarded by feature flags (`GEOIP_ENABLED`, `GEOIP_DB_PATH`). Without additional wiring, most environments run with geo analytics disabled and there is no automated signal to validate the feature.

## Scope & Deliverables

1. **Environment Wiring**
   - Provision MaxMind GeoLite2 Country database for staging/prod.
   - Update Docker Compose / Kubernetes manifests to mount the `.mmdb` file at a consistent path.
   - Ensure `GEOIP_ENABLED=true` and `GEOIP_DB_PATH` point to the mounted volume.
   - Surface configuration status via Secrets/ConfigMaps (document required keys).

2. **Automated Coverage**
   - Add an integration-style worker test (Testcontainers) that injects a click with a known public IP and asserts `LinkGeoDailyAggregate` upserts.
   - Expand Playwright e2e to verify geo analytics appears when feature flag is enabled.
   - Consider a lightweight smoke check that fails fast if the GeoIP DB is missing at boot.

3. **Operational Notes**
   - Document data refresh cadence for the GeoLite2 database (monthly) and scripts to fetch updates.
   - Provide runbook steps for rotating the DB and verifying the backend picks up the new file (e.g., restart guidance, log messages to expect).
   - Capture known limitations (private IPs resolve to null, IPv6 support).

## Non-Goals
- Building a UI toggle for Geo analytics.
- Shipping paid MaxMind databases or license management.
- Creating dashboards beyond existing `/api/admin/metrics/geo` endpoint.

## Dependencies
- `infra/geo/GeoLite2-Country.mmdb` artifact checked into the repository for local dev.
- `GeoIpService` initialization in the backend.

## Acceptance Criteria
- [ ] All environments that require Geo analytics have the env vars set and database mounted.
- [ ] Backend logs a clear info message confirming GeoIP enabled state, and warnings escalate to alerts when disabled in production.
- [ ] Test coverage ensures geo aggregates increment (integration) and API surfaces the data (e2e).
- [ ] Documentation (docs/geo-analytics.md or similar) describes setup, maintenance, and troubleshooting.

