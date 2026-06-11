# Docker Infrastructure

Docker Compose at the repository root manages the current local MVP stack.
Application Dockerfiles live next to each implemented Java module.

## Current Services

| Service | Purpose |
| --- | --- |
| `postgres` | Separate user, task, and notification databases |
| `redis` | Independently running Redis container; not used for JWT storage |
| `mailhog` | Local SMTP capture |
| `zipkin` | Local tracing infrastructure container |
| `config-server` | Native Spring Cloud Config repository |
| `eureka-server` | Service discovery |
| `user-service` | User management and authentication |
| `task-service` | Task lifecycle, ownership, assignment, and status changes |
| `notification-service` | Notification persistence and delivery state |
| `gateway` | External API entry point |

The Vue frontend is implemented under `frontend/vue-frontend`, but it is not a
Compose service. Run it separately with Vite after the backend stack is ready.

## Running The Project

From the repository root:

```bash
cp .env.example .env
docker compose --env-file .env -f compose.yml up -d --build
./scripts/check-local-stack.sh
```

Then start the frontend:

```bash
cd frontend/vue-frontend
npm install
npm run dev
```

See [Health checks](../../doc/operations/health-checks.md) and
[Environment variables](../../doc/configuration/env-variables.md). Frontend
configuration and routes are documented in the
[frontend README](../../frontend/vue-frontend/README.md).
