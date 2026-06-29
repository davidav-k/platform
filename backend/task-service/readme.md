# Task Service

## Status

MVP task persistence, create, get, list, and partial update endpoints are implemented. The
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

`POST /api/v1/tasks`, `GET /api/v1/tasks`, `GET /api/v1/tasks/{taskId}`,
`PATCH /api/v1/tasks/{taskId}`, `PATCH /api/v1/tasks/{taskId}/status`,
`PATCH /api/v1/tasks/{taskId}/assignee`, and `DELETE /api/v1/tasks/{taskId}`
require a valid access JWT. The service accepts the same `Authorization: Bearer`
header and `access-token` cookie used by the Gateway and validates the JWT
independently.

Task ownership is server controlled. `createdByUserId` is derived from the JWT
subject and ignored if a client includes it in the request payload.

For task reads, updates, and status changes, `ROLE_ADMIN` and `ROLE_SUPER_ADMIN`
can access all tasks. Other authenticated users can access only tasks they
created or are assigned to. Deletion is restricted to administrators and the
task creator. Assignment changes are also restricted to administrators and the
task creator; assignment alone grants neither reassign nor delete permission. Inaccessible
task identifiers return the same `404 NOT_FOUND` response as missing tasks.

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

When calling task-service through API Gateway, unauthenticated requests are rejected by the Gateway before reaching task-service and return `401`.

```bash
curl -i http://localhost:8080/api/tasks
```

Example error body:

```json
{
  "error": "Unauthorized",
  "message": "Missing or invalid token"
}
```

## Notification Integration

Task mutations persist domain changes and write task events to `outbox_events`
in the same transaction. The outbox publisher sends events to Kafka topic
`platform.task-events`, and notification-service consumes them to create
in-app notifications when the event payload contains a notification recipient.

Implemented task events:

| Use case | Outbox event |
| --- | --- |
| Create task | `TASK_CREATED` |
| Assign/reassign/unassign task | `TASK_ASSIGNED` |
| Change task status | `TASK_STATUS_CHANGED` |

`TASK_CREATED` creates an `IN_APP` `TASK_CREATED` notification only when the
created task payload contains `assigneeUserId`. Task-service does not call
notification-service directly for task notifications.

Configuration variables:

- `OUTBOX_PUBLISHER_ENABLED` (default `true`)
- `OUTBOX_PUBLISHER_ADAPTER` (default `kafka`)
- `OUTBOX_PUBLISHER_KAFKA_BOOTSTRAP_SERVERS` (default `kafka:9092`)
- `OUTBOX_PUBLISHER_KAFKA_TOPIC` (default `platform.task-events`)

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
- Event side effect: writes `TASK_CREATED` to `outbox_events`

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
- Authorization: admin roles can read any task; other users must be the creator or assignee
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
- Authorization: admin roles see all tasks; other users see only tasks they created or are assigned to
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

### `PATCH /api/v1/tasks/{taskId}`

Partially updates a task through `UpdateTaskUseCase`.

- Editable fields: `title`, `description`, `priority`, `assigneeUserId`
- Omitted fields remain unchanged
- `description` and `assigneeUserId` may be cleared with `null`
- Status changes are handled separately and are not accepted
- Admin roles can update any task; other users must be the creator or assignee
- Changing `assigneeUserId` requires an admin role or task creator permission
- Missing or inaccessible tasks return `404 NOT_FOUND`
- Gateway route: `PATCH /api/tasks/{taskId}`

Gateway example:

```bash
curl -i -X PATCH "http://localhost:8080/api/tasks/${TASK_ID}" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Updated task title",
    "priority": "HIGH"
  }'
```

### `PATCH /api/v1/tasks/{taskId}/status`

Changes a task status through `ChangeTaskStatusUseCase`.

- Request field: required `status` with one of `NEW`, `IN_PROGRESS`, `DONE`, `CANCELLED`
- All valid enum-to-enum changes are allowed for the MVP; no transition state machine is applied
- Admin roles can change any task; other users must be the creator or assignee
- Missing or inaccessible tasks return `404 NOT_FOUND`
- Other task fields remain unchanged; `updatedAt` is managed by the service
- Gateway route: `PATCH /api/tasks/{taskId}/status`
- Event side effect: writes `TASK_STATUS_CHANGED` to `outbox_events`

Gateway example:

```bash
curl -i -X PATCH "http://localhost:8080/api/tasks/${TASK_ID}/status" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"status":"IN_PROGRESS"}'
```

### `DELETE /api/v1/tasks/{taskId}`

Soft-deletes a task through `DeleteTaskUseCase`.

- Admin roles can delete any task; other users must be the creator
- Assignees cannot delete tasks they did not create
- Response: `200 OK` with the standard response metadata and no data payload
- Missing, inaccessible, or already deleted tasks return `404 NOT_FOUND`
- Deleted tasks are excluded from normal get and list operations and cannot be updated
- Deletion timestamps and actor identifiers remain internal and are not exposed in task DTOs
- Gateway route: `DELETE /api/tasks/{taskId}`

Gateway example:

```bash
curl -i -X DELETE "http://localhost:8080/api/tasks/${TASK_ID}" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}"
```

### `PATCH /api/v1/tasks/{taskId}/assignee`

Assigns, reassigns, or unassigns a task through `AssignTaskUseCase`.

- Request field: `assigneeUserId` as a UUID, or explicit `null` to unassign
- The field must be present; an empty object is rejected
- Admin roles can assign any active task; other users must be the creator
- Assignees cannot reassign tasks they did not create
- Missing, inaccessible, or deleted tasks return `404 NOT_FOUND`
- User existence is not verified through `user-service` in this MVP endpoint
- Other task fields remain unchanged; `updatedAt` is managed by the service
- Gateway route: `PATCH /api/tasks/{taskId}/assignee`
- Event side effect: writes `TASK_ASSIGNED` to `outbox_events`

Gateway example:

```bash
curl -i -X PATCH "http://localhost:8080/api/tasks/${TASK_ID}/assignee" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"assigneeUserId":"11111111-1111-1111-1111-111111111111"}'
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

The repository test uses PostgreSQL 16.1 through Testcontainers and validates
the Flyway baseline, Hibernate mappings, sequences, and database constraints.
Use-case and security tests use H2 in PostgreSQL compatibility mode to keep the
suite fast. Docker must be running for the full command. Maven Surefire sets
Docker API version `1.44` for Docker Engine 29 compatibility.
