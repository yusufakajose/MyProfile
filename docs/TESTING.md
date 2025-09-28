## Worker Integration Tests

### Analytics Worker Upsert Happy Path
- Location: `backend/src/test/java/com/linkgrove/api/integration/AnalyticsWorkerIntegrationTest.java`
- Purpose: Validate the analytics worker processes a click event end-to-end (RabbitMQ ➝ worker ➝ Postgres aggregates) using Testcontainers-provisioned infrastructure.
- Coverage: click count increment, unique visitor dedupe, referrer/device/source aggregate upserts, and cache invalidation on the primary link record.
- Execution tips:
  - The test waits up to ~10 seconds for asynchronous processing; ensure Docker resources are available so RabbitMQ listeners start quickly.
  - Uses `TestPasswordUtil.strong()` for user bootstrap; no additional secrets required.
- To run individually:
  ```bash
  mvn -pl backend -Dit.test=AnalyticsWorkerIntegrationTest -Pit integration-test
  ```
  Or execute the entire integration test suite with the `it` profile.

### Debugging Failures
