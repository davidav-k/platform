# Docker Infrastructure

Docker Compose at the repository root manages the current local MVP stack.
Application Dockerfiles live next to each implemented Java module.

## Current Services

| Service | Purpose |
| --- | --- |
| `postgres` | Separate user, task, notification, and audit databases |
| `redis` | Independently running Redis container; not used for JWT storage |
| `kafka` | Task, user, and notification event broker |
| `mailhog` | Local SMTP capture |
| `zipkin` | Local tracing infrastructure container |
| `config-server` | Native Spring Cloud Config repository |
| `eureka-server` | Service discovery |
| `user-service` | User management and authentication |
| `task-service` | Task lifecycle, ownership, assignment, status changes, and outbox event publishing |
| `notification-service` | Notification persistence, Kafka task-event consumption, and notification audit-event publishing |
| `audit-service` | Kafka task/user/notification-event consumption, audit persistence, read-only API, service discovery, and health reporting |
| `gateway` | External API entry point |
| `frontend` | Vue production build served by nginx on host port `5173` |

The frontend image uses a Node build stage and an nginx runtime stage. Its API
Gateway URL is injected at image build time through `VITE_API_BASE_URL`.

Task notifications are delivered through `task-service` outbox events, Kafka
topic `platform.task-events`, and the notification-service Kafka consumer.
Audit Service independently consumes task events from that topic and user
events from `platform.user-events`, using its own consumer group and event IDs
for idempotent audit persistence.
Notification Service separately publishes notification creation events from
its local outbox to `platform.notification-events`; Audit Service consumes
that topic with its own listener and persists normalized audit records.

## Running The Project

From the repository root:

```bash
cp .env.example .env
docker compose --env-file .env -f compose.yml up -d --build
./scripts/check-local-stack.sh
```

The complete stack exposes the frontend at `http://localhost:5173` and API
Gateway at `http://localhost:8080`.

For frontend development with Vite instead of the container:

```bash
cd frontend/vue-frontend
npm install
npm run dev
```

See [Health checks](../../doc/operations/health-checks.md) and
[Environment variables](../../doc/configuration/env-variables.md). Frontend
configuration and routes are documented in the
[frontend README](../../frontend/vue-frontend/README.md).
