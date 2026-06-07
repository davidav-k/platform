# Task Service

## Status

MVP task persistence, create, get, and list endpoints are implemented. The
service starts, registers in Eureka, and loads configuration from Config Server.

## Purpose

Manages tasks within the Task Management Platform (TMP).
Exposed via API Gateway at `/api/tasks` and `/api/tasks/**`, which route to
the internal `/api/v1/tasks` endpoints through Eureka service discovery.

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

## Authentication

`POST /api/v1/tasks`, `GET /api/v1/tasks`, and `GET /api/v1/tasks/{taskId}`
require a valid access JWT. The service accepts the same `Authorization: Bearer`
header and `access-token` cookie used by the Gateway and validates the JWT
independently.

Task ownership is server controlled. `createdByUserId` is derived from the JWT
subject and ignored if a client includes it in the request payload.

Use a real access token issued by `user-service`; do not commit or log tokens.

```bash
ACCESS_TOKEN="<access-jwt>"
```

Bearer token example:

```bash
curl -i \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  http://localhost:8080/api/tasks
```

Cookie example:

```bash
curl -i \
  --cookie "access-token=${ACCESS_TOKEN}" \
  http://localhost:8080/api/tasks
```

Unauthenticated requests return `401`:

```bash
curl -i http://localhost:8080/api/tasks
```

Example error body:

```json
{
  "code": 401,
  "status": "UNAUTHORIZED",
  "message": "Authentication is required"
}
```

## API

External clients should call task-service through API Gateway:

- Gateway route: `http://localhost:8080/api/tasks`
- Internal service route: `http://localhost:8086/api/v1/tasks`

### `POST /api/v1/tasks`

Creates an MVP task through `CreateTaskUseCase`.

- Request DTO: `CreateTaskRequest`
- Response DTO: `CreateTaskResponse` in the standard `data.task` envelope
- Authentication: required
- Ownership: `createdByUserId` is populated from the authenticated JWT subject
- Gateway route: `POST /api/tasks`

Gateway example:

```bash
curl -i -X POST http://localhost:8080/api/tasks \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Implement login",
    "description": "Create login functionality",
    "priority": "HIGH",
    "assigneeUserId": "11111111-1111-1111-1111-111111111111"
  }'
```

Response body shape:

```json
{
  "code": 201,
  "status": "CREATED",
  "message": "Task created successfully.",
  "data": {
    "task": {
      "taskId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
      "title": "Implement login",
      "description": "Create login functionality",
      "status": "NEW",
      "priority": "HIGH",
      "assigneeUserId": "11111111-1111-1111-1111-111111111111",
      "createdByUserId": "22222222-2222-2222-2222-222222222222",
      "createdAt": "2026-06-03T10:15:30Z"
    }
  }
}
```

`createdByUserId` is ignored if sent by the client:

```bash
curl -i -X POST http://localhost:8080/api/tasks \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Ownership is server controlled",
    "createdByUserId": "00000000-0000-0000-0000-000000000000"
  }'
```

The stored `createdByUserId` is still the authenticated JWT subject.

### `GET /api/v1/tasks/{taskId}`

Returns one task by public task UUID through `GetTaskUseCase`.

- Response DTO: `TaskResponse` in the standard `data.task` envelope
- Authentication: required
- Missing tasks return `404 NOT_FOUND`
- Gateway route: `GET /api/tasks/{taskId}`

Gateway example:

```bash
TASK_ID="<task-uuid>"

curl -i \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  "http://localhost:8080/api/tasks/${TASK_ID}"
```

Response body shape:

```json
{
  "code": 200,
  "status": "OK",
  "message": "Task retrieved successfully.",
  "data": {
    "task": {
      "taskId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
      "title": "Implement login",
      "description": "Create login functionality",
      "status": "NEW",
      "priority": "HIGH",
      "assigneeUserId": "11111111-1111-1111-1111-111111111111",
      "createdByUserId": "22222222-2222-2222-2222-222222222222",
      "createdAt": "2026-06-03T10:15:30Z",
      "updatedAt": "2026-06-03T10:15:30Z"
    }
  }
}
```

### `GET /api/v1/tasks`

Returns a paginated task list through `ListTasksUseCase`.

- Optional filters: `status`, `priority`, `assigneeUserId`, `createdByUserId`
- Pagination: `page` defaults to `0`, `size` defaults to `20`
- Sorting: `sort` defaults to `createdAt,desc`
- Response shape: standard envelope with `data.items` and `data.page`
- Authentication: required
- Gateway route: `GET /api/tasks`

Gateway examples:

```bash
curl -i \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  "http://localhost:8080/api/tasks?page=0&size=20"
```

```bash
curl -i \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  "http://localhost:8080/api/tasks?status=NEW&priority=HIGH&assigneeUserId=11111111-1111-1111-1111-111111111111&page=0&size=20"
```

Response body shape:

```json
{
  "code": 200,
  "status": "OK",
  "message": "Tasks retrieved successfully.",
  "data": {
    "items": [
      {
        "taskId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
        "title": "Implement login",
        "description": "Create login functionality",
        "status": "NEW",
        "priority": "HIGH",
        "assigneeUserId": "11111111-1111-1111-1111-111111111111",
        "createdByUserId": "22222222-2222-2222-2222-222222222222",
        "createdAt": "2026-06-03T10:15:30Z",
        "updatedAt": "2026-06-03T10:15:30Z"
      }
    ],
    "page": {
      "number": 0,
      "size": 20,
      "totalElements": 1,
      "totalPages": 1
    }
  }
}
```

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
