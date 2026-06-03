# Task Service

## Status

Infrastructure skeleton. Service starts, registers in Eureka and loads configuration from Config Server.
Business logic is not yet implemented.

## Purpose

Manages tasks within the Task Management Platform (TMP).
Exposed via API Gateway at `/api/tasks/**`.

`task-service` will own tasks, assignments, statuses, comments, and task history.
It must not access the `user-service` database directly.

The HTTP API contract is defined in [Task service API contract](../../doc/api/task-service-contract.md).
Service boundary and gateway alignment notes are in [Service boundaries](../../doc/architecture/service-boundaries.md).

## Stack

- Java 17
- Spring Boot 3.4.0
- Spring Cloud 2024.0.1
- PostgreSQL 16.x
- Flyway
- Maven

## Database Migrations

Flyway owns task-service schema evolution. Versioned migrations live under
`src/main/resources/db/migration`; the initial MVP baseline is
`V1__task_service_baseline.sql`.

Runtime database settings are served by Config Server from
`config/task-service-dev.yml`. The task-service database name defaults to
`tasks_db` through `TASK_POSTGRES_DB`.

## API

### `POST /api/v1/tasks`

Creates an MVP task through `CreateTaskUseCase`.

- Request DTO: `CreateTaskRequest`
- Response DTO: `CreateTaskResponse` in the standard `data.task` envelope
- Temporary creator input: `X-Created-By-User-Id` header with a public user UUID

### `GET /api/v1/tasks/{taskId}`

Returns one task by public task UUID through `GetTaskUseCase`.

- Response DTO: `TaskResponse` in the standard `data.task` envelope
- Missing tasks return `404 NOT_FOUND`

### `GET /api/v1/tasks`

Returns a paginated task list through `ListTasksUseCase`.

- Optional filters: `status`, `priority`, `assigneeUserId`, `createdByUserId`
- Pagination: `page` defaults to `0`, `size` defaults to `20`
- Sorting: `sort` defaults to `createdAt,desc`
- Response shape: standard envelope with `data.items` and `data.page`

## Local Startup

### Prerequisites

Running infrastructure services:
- Config Server (`http://localhost:8888`)
- Eureka Server (`http://localhost:8761`)

### Run via Maven

```bash
CONFIG_SERVER_URI=http://localhost:8888 \
ACTIVE_PROFILE=dev \
mvn -f backend/task-service/pom.xml spring-boot:run
```

### Run via Docker Compose

```bash
docker compose --env-file .env -f compose.yml up -d --build task-service
```

### Health check

```
GET http://localhost:8086/actuator/health
```

### Build and test

```bash
mvn -B -f backend/task-service/pom.xml test
```
