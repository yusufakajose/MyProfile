Testing guide

What we test
- Validators and sanitization (unit): DTO constraints, custom validators, and LinkService input normalization
- Controllers (standalone): one happy-path per endpoint, plus a handful of 400 validation cases
- Security boundary: one WebMvc slice test to assert unauthenticated requests to /api/links return 401
- Integrations: two DB-backed flows exercising click tracking, QR endpoints, and analytics

How to run
- Unit tests (default):
  mvn -DskipITs test
- Integration tests (start Testcontainers):
  mvn -P it verify

CI suggestion
- Run unit tests on every PR: mvn -DskipITs test
- Run integration tests on label or nightly: mvn -P it verify

Notes
- We prefer standalone controller tests to avoid full security/wiring; a single slice test covers the 401 boundary
- We avoid duplicating the same scenario in multiple layers and skip trivial getters/setters
- JaCoCo enforces a modest coverage threshold to keep signal high


