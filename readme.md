# Platform

Microservice-based Task Management Platform built with Spring Boot.

> Current focus: MVP stabilization. User, task, notification, Kafka/outbox, Gateway, and frontend flows are operational for local development.

## Implemented

### Services

- user-service
  - Registration
  - Authentication
  - JWT access/refresh flow
  - MFA
  - Profile and account lifecycle management
- task-service
  - Create, get, list, update, status change, assignment, and soft delete
  - Ownership/RBAC, filtering, and pagination
  - JWT-based authentication
  - Flyway-managed PostgreSQL schema
  - Transactional outbox events for task creation, assignment, and status changes
- notification-service
  - Create, get, and list notifications with filtering and pagination
  - Kafka consumer for task domain events
  - In-app notification creation from task outbox events
  - Idempotent event consumption through `event_consumption_log`
  - JWT-based authentication
  - Flyway-managed PostgreSQL schema
  - Docker Compose and API Gateway integration
- api-gateway
  - Routing
  - JWT validation
  - Service discovery integration
- config-server
- eureka-server
- Vue 3 frontend
  - Cookie-based login, profile loading, and session restoration
  - Guarded task list, details, create, edit, status, assignment, and soft-delete flows
  - Guarded notification list and details flows
  - Shared loading, empty, error, retry, and Not Found UX

### Infrastructure

- PostgreSQL
- Redis
- Kafka
- MailHog
- Zipkin
- Docker Compose
- Transactional outbox publisher in task-service
- Kafka consumer in notification-service

## Planned

- Audit service
- Kubernetes deployment
- Prometheus/Grafana monitoring

## Technology Stack

- Java 17
- Spring Boot 3.4.x
- Spring Cloud 2024.x
- Spring Security
- PostgreSQL and Flyway
- Redis
- Apache Kafka
- Docker Compose
- Vue 3, Vite, Vue Router, and native Fetch API
- JUnit 5, Mockito, H2, and Testcontainers

## Project Structure

```text
backend/
|-- user-service
|-- task-service
`-- notification-service

infrastructure/
|-- api-gateway
|-- config-server
`-- eureka-server

frontend/
`-- vue-frontend

config/
doc/
```

## Quick Start

```bash
cp .env.example .env
docker compose --env-file .env -f compose.yml up -d --build
```

API Gateway: `http://localhost:8080`
Frontend: `http://localhost:5173`

Core local ports:

| Component | Port |
| --- | --- |
| API Gateway | `8080` |
| User Service | `8085` |
| Task Service | `8086` |
| Notification Service | `8087` |
| Frontend | `5173` |
| Config Server | `8888` |
| Eureka Server | `8761` |
| PostgreSQL | `5432` |
| Redis | `6379` |
| Kafka host listener | `9092` |
| MailHog SMTP/UI | `1025` / `8025` |
| Zipkin | `9411` |

Verify startup:

```bash
./scripts/check-local-stack.sh
```

Windows:

```powershell
.\scripts\check-local-stack.ps1
```

For frontend development with Vite instead of the nginx container:

```bash
cd frontend/vue-frontend
npm install
npm run dev
```

See the [frontend README](frontend/vue-frontend/README.md) for routes, features,
configuration, limitations, and the MVP verification checklist.

## Notification Flow

Task notifications use only the transactional outbox and Kafka path:

```text
Frontend -> API Gateway -> task-service -> TaskEntity + outbox_events
  -> Kafka topic platform.task-events
  -> notification-service -> notifications
  -> Frontend GET /api/notifications
```

Task-service writes outbox events for `TASK_CREATED`, `TASK_ASSIGNED`, and
`TASK_STATUS_CHANGED`. Notification-service consumes those events and creates
in-app notifications when the payload contains a recipient. For task creation,
notification-service creates an `IN_APP` `TASK_CREATED` notification only when
the event payload contains `assigneeUserId`. Task-service does not call
notification-service directly for task notifications.

Key local variables are documented in
[Environment variables](doc/configuration/env-variables.md):

- `OUTBOX_PUBLISHER_ENABLED`
- `OUTBOX_PUBLISHER_ADAPTER`
- `OUTBOX_PUBLISHER_KAFKA_BOOTSTRAP_SERVERS`
- `OUTBOX_PUBLISHER_KAFKA_TOPIC`
- `NOTIFICATION_KAFKA_ENABLED`
- `NOTIFICATION_KAFKA_TOPIC`
- `KAFKA_TASK_EVENTS_TOPIC`

Use the Postman collection in `doc/postman` for Gateway-only MVP verification,
including the Kafka-backed `TASK_CREATED` notification check. The collection
uses `notificationWaitMillis` to allow asynchronous outbox/Kafka processing
before reading `/api/notifications`.

## Documentation

- Architecture - `doc/architecture.md`
- Development workflow - `doc/development-workflow.md`
- Authentication flow - `doc/security/auth-flow.md`
- Database migration strategy - `doc/database/migration-strategy.md`
- Service boundaries - `doc/architecture/service-boundaries.md`
- Environment variables - `doc/configuration/env-variables.md`
- Kafka notification verification - `doc/kafka-notification-e2e-verification.md`
- Postman collection guide - `doc/postman/postman_README.md`
- Technical debt - `doc/technical-debt.md`
- Frontend MVP - `frontend/vue-frontend/README.md`

## Status

The project is under active MVP development.
