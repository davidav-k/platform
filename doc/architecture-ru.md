# Архитектура Platform

## Назначение

Platform - MVP-stage Task Management Platform. Текущая локальная система
содержит user management, task management, notifications, Gateway, frontend и
инфраструктуру Docker Compose.

## Реализованный runtime

| Component | Responsibility | Port |
| --- | --- | --- |
| `user-service` | Users, roles, authentication, JWT, MFA, profiles, account lifecycle | `8085` |
| `task-service` | Task lifecycle, ownership, assignment, status changes, filtering, pagination, soft delete, task outbox events | `8086` |
| `notification-service` | Notification persistence, create/get/list API, Kafka task event consumer, idempotency log | `8087` |
| `api-gateway` | External entry point, JWT early rejection, routing, CORS, circuit breaker fallback | `8080` |
| `frontend` | Vue 3 bundle served by nginx with SPA fallback | `5173` |
| `config-server` | Spring Cloud Config native repository from `./config` | `8888` |
| `eureka-server` | Service registration and discovery | `8761` |
| PostgreSQL 16.1 | Separate user/task/notification databases | `5432` |
| Redis 7 | Running and health-checked; not integrated into services yet | `6379` |
| Kafka 3.7.1 | Task event transport for Kafka-backed notifications | `9092` |
| MailHog | Local SMTP capture and UI | `1025`, `8025` |
| Zipkin | Local tracing infrastructure container | `9411` |

## HTTP routes

```text
client -> api-gateway /api/users/** -> user-service /api/v1/user/**
client -> api-gateway /api/tasks/** -> task-service /api/v1/tasks/**
client -> api-gateway /api/notifications/** -> notification-service /api/v1/notifications/**
```

Frontend использует только Gateway routes и не обращается к backend services
напрямую.

## Task notification flow

```text
Frontend
  -> Gateway
  -> Task Service
  -> TaskEntity + OutboxEvent
  -> Kafka platform.task-events
  -> Notification Service
  -> NotificationEntity
  -> Frontend GET /api/notifications
```

`task-service` не вызывает `notification-service` напрямую для task-created
notifications. Уведомления создаются из Kafka events.

## Частично настроено

- Redis и Zipkin запускаются локально, но интеграция сервисов с ними не
  считается завершённой.
- Notification preferences entity существует, но публичного preferences API
  нет.

## Planned

- audit service
- OpenAI-backed task automation
- Kubernetes/Helm deployment
- Prometheus/Grafana monitoring

## Data ownership

- `user-service` владеет `users_db`.
- `task-service` владеет `tasks_db`.
- `notification-service` владеет `notifications_db`.
- Flyway migrations являются источником схемы.
- Hibernate использует `ddl-auto=validate`.
- Сервисы не обращаются напрямую к базам данных других сервисов.
