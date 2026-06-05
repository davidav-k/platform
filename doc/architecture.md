# Platform Architecture

## Purpose

Platform is an MVP-stage Task Management Platform. The current runnable system
delivers user management, task management, and shared infrastructure. The
notification service remains unimplemented.

For aggregate ownership and future service rules, see
[Service boundaries](architecture/service-boundaries.md).

## Implemented Runtime

The Docker Compose stack currently runs:

| Component | Responsibility | Port |
| --- | --- | --- |
| `user-service` | Users, roles, authentication, JWT issuance and validation, MFA, profiles, and account lifecycle | `8085` |
| `task-service` | Task persistence, create, get, and list with JWT-based authentication | `8086` |
| `api-gateway` | External entry point, JWT early rejection, routing, CORS, and circuit breaker fallback | `8080` |
| `config-server` | Spring Cloud Config native repository mounted from `./config` | `8888` |
| `eureka-server` | Service registration and discovery | `8761` |
| PostgreSQL 16.1 | User-service and task-service persistence (separate databases) | `5432` |
| Redis 7 | Independently running and health-checked; not integrated into user-service | `6379` |
| MailHog | Local SMTP capture and UI | `1025`, `8025` |
| Zipkin | Local tracing infrastructure container | `9411` |

The implemented request paths are:

```text
client -> api-gateway /api/users/**  -> user-service  /api/v1/user/**
client -> api-gateway /api/tasks/**  -> task-service  /api/v1/tasks/**
```

The gateway validates access JWTs as an early rejection layer. Downstream services
validate JWTs again and own authorization decisions. See
[Authentication flow](security/auth-flow.md).

## Partially Configured

- Redis and Zipkin run locally, but user-service does not currently integrate
  with Redis and tracing integration is not documented as complete.
- `frontend/vue-frontend` contains documentation only. There is no frontend
  application manifest or source tree in the repository.

## Planned Services

The following directories are documentation shells:

| Service | Future ownership |
| --- | --- |
| `notification-service` | System notifications, preferences, email delivery requests, and delivery tracking |

Its future contract is documented in:

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

- `user-service` owns its PostgreSQL schema (`users_db` by default).
- `task-service` owns its PostgreSQL schema (`tasks_db` by default).
- Flyway migrations are authoritative for both service schemas.
- Hibernate uses `ddl-auto=validate`; it does not create or update schema.
- Future services must own separate schemas or databases.
- Services must not access another service's database directly.

See [Database migration strategy](database/migration-strategy.md).

## Local Development

See [Configuration management](architecture/configuration-management.md) for startup modes,
[Health checks](operations/health-checks.md) for verification, and
[Environment variables](configuration/env-variables.md) for required variables.
