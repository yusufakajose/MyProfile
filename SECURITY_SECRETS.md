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
  - JWT secret: prefer soft-rotation using previous secrets overlap.
  - Webhook secrets: rotate in app UI and downstream receivers concurrently.
- Auditing:
  - Monitor access logs and failed login metrics; alert on spikes.
- Local development: use non-sensitive dummy values; never reuse production secrets.

## JWT secret rotation

- Env vars:
  - `JWT_SECRET`: current signing key (hex/random 32+ bytes)
  - `JWT_PREVIOUS_SECRETS`: comma-separated list of prior secrets accepted for verification
- Steps:
  1. Generate a new secret and deploy with both `JWT_SECRET` (new) and `JWT_PREVIOUS_SECRETS` (old).
  2. Keep overlap for a short window (e.g., 7 days) to allow old tokens to expire naturally.
  3. Remove old entries from `JWT_PREVIOUS_SECRETS` after the window.

### Helper script

```bash
openssl rand -hex 64
```

Example `.env` entries:

```bash
JWT_SECRET=$(openssl rand -hex 64)
JWT_PREVIOUS_SECRETS="<old-hex-secret>"
```


