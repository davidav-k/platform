# Platform Architecture

## Purpose

Platform is an MVP-stage Task Management Platform. The current runnable system
delivers user management, task management, notifications, and shared
infrastructure.

For aggregate ownership and future service rules, see
[Service boundaries](architecture/service-boundaries.md).

For the MVP decision on account deactivation, retained cross-service user
references, and future lifecycle propagation, see
[User deletion and deactivation policy](architecture/user-deletion-policy.md).

For the proposed transactional outbox model, candidate domain events, broker
decision, Kafka adapter design, and incremental migration phases, see
[Outbox pattern design](architecture/outbox-pattern-design.md).

## Implemented Runtime

The Docker Compose stack currently runs:

| Component | Responsibility | Port |
| --- | --- | --- |
| `user-service` | Users, roles, authentication, JWT issuance and validation, MFA, profiles, and account lifecycle | `8085` |
| `task-service` | Task lifecycle, ownership, assignment, status changes, filtering, pagination, and soft delete | `8086` |
| `notification-service` | Notification persistence, create, get, list, filtering, pagination, and task-assignment notification integration | `8087` |
| `api-gateway` | External entry point, JWT early rejection, routing, CORS, and circuit breaker fallback | `8080` |
| `frontend` | Vue 3 production bundle served by nginx with SPA route fallback | `5173` |
| `config-server` | Spring Cloud Config native repository mounted from `./config` | `8888` |
| `eureka-server` | Service registration and discovery | `8761` |
| PostgreSQL 16.1 | User, task, and notification persistence in separate databases | `5432` |
| Redis 7 | Independently running and health-checked; not integrated into user-service | `6379` |
| MailHog | Local SMTP capture and UI | `1025`, `8025` |
| Zipkin | Local tracing infrastructure container | `9411` |

The implemented request paths are:

```text
client -> api-gateway /api/users/**  -> user-service  /api/v1/user/**
client -> api-gateway /api/tasks/**  -> task-service  /api/v1/tasks/**
client -> api-gateway /api/notifications/** -> notification-service /api/v1/notifications/**
```

The Vue 3 frontend runs through nginx in Docker Compose or through Vite during
frontend development. It uses only external API Gateway routes and never
connects directly to a backend service. See the
[frontend README](../frontend/vue-frontend/README.md) for implemented pages and
startup instructions.

The gateway validates access JWTs as an early rejection layer. Downstream services
validate JWTs again and own authorization decisions. See
[Authentication flow](security/auth-flow.md).

## Partially Configured

- Redis and Zipkin run locally, but user-service does not currently integrate
  with Redis and tracing integration is not documented as complete.

The notification API contract is documented in
[Notification service API contract](api/notification-service-contract.md).

## Planned Architecture

The following items are roadmap direction, not implemented functionality:

- later event-driven communication using the proposed outbox pattern; Kafka is
  selected as the future platform domain-event broker but is not implemented
- audit service
- OpenAI-backed task automation
- Kubernetes and Helm deployment configuration
- Prometheus and Grafana monitoring stack

## Data Ownership

- `user-service` owns its PostgreSQL schema (`users_db` by default).
- `task-service` owns its PostgreSQL schema (`tasks_db` by default).
- `notification-service` owns its PostgreSQL schema (`notifications_db` by default).
- Flyway migrations are authoritative for all three service schemas.
- Hibernate uses `ddl-auto=validate`; it does not create or update schema.
- Future services must also own separate schemas or databases.
- Services must not access another service's database directly.

See [Database migration strategy](database/migration-strategy.md).

## Local Development

See [Configuration management](architecture/configuration-management.md) for startup modes,
[Health checks](operations/health-checks.md) for verification, and
[Environment variables](configuration/env-variables.md) for required variables.
