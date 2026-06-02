# Platform Architecture

## Purpose

Platform is an MVP-stage Task Management Platform. The current runnable system
stabilizes user management and shared infrastructure before task and
notification services are implemented.

For aggregate ownership and future service rules, see
[Service boundaries](architecture/service-boundaries.md).

## Implemented Runtime

The Docker Compose stack currently runs:

| Component | Responsibility | Port |
| --- | --- | --- |
| `user-service` | Users, roles, authentication, JWT issuance and validation, MFA, profiles, and account lifecycle | `8085` |
| `api-gateway` | External entry point, JWT early rejection, routing, CORS, and circuit breaker fallback | `8080` |
| `config-server` | Spring Cloud Config native repository mounted from `./config` | `8888` |
| `eureka-server` | Service registration and discovery | `8761` |
| PostgreSQL 16.1 | User-service persistence | `5432` |
| Redis 7 | Independently running and health-checked; not integrated into user-service | `6379` |
| MailHog | Local SMTP capture and UI | `1025`, `8025` |
| Zipkin | Local tracing infrastructure container | `9411` |

The implemented request path for user APIs is:

```text
client -> api-gateway /api/users/** -> user-service /api/v1/user/**
```

The gateway validates access JWTs as an early rejection layer. `user-service`
validates JWTs again and owns authorization decisions. See
[Authentication flow](security/auth-flow.md).

## Partially Configured

- API Gateway reserves `/api/tasks/**` and rewrites it to `/api/v1/task/**`,
  but no runnable `task-service` module or container exists.
- Redis and Zipkin run locally, but user-service does not currently integrate
  with Redis and tracing integration is not documented as complete.
- `frontend/vue-frontend` contains documentation only. There is no frontend
  application manifest or source tree in the repository.

## Planned Services

The following directories are documentation shells:

| Service | Future ownership |
| --- | --- |
| `task-service` | Tasks, assignments, task statuses, comments, and task history |
| `notification-service` | System notifications, preferences, email delivery requests, and delivery tracking |

Their future contracts are documented in:

- [Task service API contract](api/task-service-contract.md)
- [Notification service API contract](api/notification-service-contract.md)

## Planned Architecture

The following items are roadmap direction, not implemented functionality:

- synchronous REST integration for future MVP services
- later event-driven communication and Kafka integration
- audit service
- OpenAI-backed task automation
- Vue 3 frontend implementation
- Kubernetes and Helm deployment configuration
- Prometheus and Grafana monitoring stack

## Data Ownership

- `user-service` owns its PostgreSQL schema.
- Flyway migrations are authoritative for the user-service schema.
- Hibernate uses `ddl-auto=validate`; it does not create or update schema.
- Future services must own separate schemas or databases.
- Services must not access another service's database directly.

See [Database migration strategy](database/migration-strategy.md).

## Local Development

Start and verify the current stack from the repository root:

```bash
cp .env.example .env
docker compose --env-file .env -f compose.yml up -d --build
./scripts/check-local-stack.sh
```

See [Health checks](operations/health-checks.md) and
[Environment variables](configuration/env-variables.md) for details.
