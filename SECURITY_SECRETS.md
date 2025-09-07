# Secrets Management (Local & CI)

- Use environment variables for secrets. Never hardcode secrets in code or commit them.
- Backend envs:
  - `JWT_SECRET`: 32+ random bytes. Rotate if leaked.
  - `SPRING_DATA_REDIS_HOST`, `SPRING_DATA_REDIS_PORT` for Redis.
  - `OTEL_EXPORTER_OTLP_ENDPOINT` for telemetry (optional).
  - `DB_*` for Hikari tuning (optional).
- Docker Compose: store local-only secrets in a `.env` file that is not committed.
- CI (GitHub Actions): store secrets in GitHub repository secrets and map them to env vars.
- Rotation policy:
  - JWT secret: rotating requires logout of all users; schedule during maintenance.
  - Webhook secrets: rotate in app UI and downstream receivers concurrently.
- Auditing:
  - Monitor access logs and failed login metrics; alert on spikes.
- Local development: use non-sensitive dummy values; never reuse production secrets.


