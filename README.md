# MyProfile

[![CI](https://github.com/OWNER/REPO/actions/workflows/ci.yml/badge.svg)](https://github.com/OWNER/REPO/actions/workflows/ci.yml)

See `.github/workflows/ci.yml` for details.

## Run locally with Docker Compose

- Prereqs: Docker and Docker Compose plugin installed
- Start all services (Postgres, Redis, RabbitMQ, backend, frontend):

```bash
bash infra/docker/start.sh
```

- App opens at `http://localhost:3001` (Nginx serves frontend and proxies `/api` to backend)
- Stop and remove containers/volumes:

```bash
bash infra/docker/stop.sh
```

- Override defaults by exporting envs before running or by creating `infra/docker/.env`.
