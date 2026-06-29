# Notification Service

## Status

The MVP API is wired into Docker Compose on port `8087` and is available
through the API Gateway.

## Configuration and Database

Runtime configuration is served by Config Server from
`config/notification-service-dev.yml`. Flyway owns schema migrations, and the
database name defaults to `notifications_db`.

## Domain Model

Domain entities, response DTOs, mappers, and Spring Data repositories are
implemented for notifications and notification preferences.

`NotificationEntity` is stored in `notifications` and contains:

- `notificationId`
- `recipientUserId`
- `type`
- `channel`
- `subject`
- `body`
- `status`
- `createdAt`, `updatedAt`, `sentAt`
- `failureReason`
- source metadata: `sourceService`, `sourceEntityType`, `sourceEntityId`

Implemented enum values:

| Enum | Values |
| --- | --- |
| `NotificationType` | `TASK_ASSIGNED`, `TASK_CREATED`, `SYSTEM` |
| `NotificationChannel` | `EMAIL`, `IN_APP` |
| `NotificationStatus` | `PENDING`, `SENT`, `FAILED` |

The current use cases persist notification rows. Email sending, read state,
mark-as-read, delete, and status transition APIs are not implemented.

## Kafka Task Event Processing

The supported task notification delivery path is Outbox Pattern + Kafka:

```text
task-service -> outbox_events -> Kafka platform.task-events
  -> NotificationEventConsumer -> TaskEventNotificationProcessor
  -> CreateSystemNotificationUseCase -> notifications
```

`NotificationEventConsumer` is enabled by `notification.kafka.enabled=true` and
listens to `notification.kafka.topic`, which defaults to
`platform.task-events`. It stores accepted event IDs in
`event_consumption_log`; `event_id` is unique and is used as the idempotency
key for at-least-once Kafka delivery. Duplicate event IDs are ignored.

`TaskEventNotificationProcessor` handles these task event types:

| Event type | Notification behavior |
| --- | --- |
| `TASK_CREATED` | Creates `IN_APP` `TASK_CREATED` for `assigneeUserId` when present. Events without assignee are consumed but skipped for notification creation. |
| `TASK_ASSIGNED` | Creates `IN_APP` `TASK_ASSIGNED` for `newAssigneeUserId` when present. |
| `TASK_STATUS_CHANGED` | Creates `IN_APP` `SYSTEM` notification for `assigneeUserId` when present. |

All task-event notifications use source metadata
`sourceService=task-service`, `sourceEntityType=TASK`, and
`sourceEntityId=<taskId>`.

## API

`POST /api/v1/notifications` creates a notification record. This endpoint does
not send email.

`GET /api/v1/notifications/{notificationId}` retrieves one notification by its
public UUID.

`GET /api/v1/notifications` lists notifications with optional
`recipientUserId`, `status`, `channel`, and `type` filters. Pagination uses
`page` and `size`; sorting uses `sort=field,direction` and defaults to
`createdAt,desc`.

`POST /internal/api/v1/notifications/system` creates an authenticated internal
in-application notification for another platform service. It accepts source
service, source entity type, and source entity UUID metadata. The API Gateway
does not route this endpoint. Task-created notifications from task-service do
not use this REST endpoint; they are created from Kafka task events.

Example request:

```json
{
  "recipientUserId": "88d3eecf-8199-4677-9664-aa8574074c16",
  "type": "TASK_ASSIGNED",
  "channel": "EMAIL",
  "subject": "Task assigned",
  "body": "A task was assigned to you."
}
```

Example response:

```json
{
  "code": 201,
  "status": "CREATED",
  "message": "Notification created successfully.",
  "data": {
    "notification": {
      "notificationId": "cd970e48-eb47-4c62-b983-2ade6ba203ef",
      "recipientUserId": "88d3eecf-8199-4677-9664-aa8574074c16",
      "type": "TASK_ASSIGNED",
      "channel": "EMAIL",
      "subject": "Task assigned",
      "body": "A task was assigned to you.",
      "status": "PENDING"
    }
  }
}
```

Example read request:

```text
GET /api/v1/notifications/cd970e48-eb47-4c62-b983-2ade6ba203ef
```

Example list request:

```text
GET /api/v1/notifications?status=PENDING&channel=EMAIL&page=0&size=20&sort=createdAt,desc
```

List responses return notification DTOs and pagination metadata:

```json
{
  "data": {
    "items": [],
    "page": {
      "number": 0,
      "size": 20,
      "totalElements": 0,
      "totalPages": 0
    }
  }
}
```

Notification creation still does not perform email delivery. `EMAIL` is a
persistable channel value, but no SMTP delivery use case is implemented in this
service.

## Local Startup and Routing

Start the service and its dependencies through Docker Compose:

```bash
docker compose --env-file .env -f compose.yml up -d --build notification-service
```

Health check:

```text
GET http://localhost:8087/actuator/health
```

Gateway route:

```text
http://localhost:8080/api/notifications
```

Internal service route:

```text
http://localhost:8087/api/v1/notifications
```

Notification API requests through either route require a valid access JWT.
Unauthenticated requests return `401 Unauthorized`.

## Build and Test

```bash
mvn -B -f backend/notification-service/pom.xml test
```

Repository, migration, use-case, and security integration tests use PostgreSQL
16.1 through Testcontainers. Docker must be running for the full suite. Maven
Surefire sets Docker API version `1.44` for Docker Engine 29 compatibility.

## Authentication

Notification API endpoints require a valid access JWT. The service validates
tokens independently and accepts them from either:

* `Authorization: Bearer <token>`
* the `access-token` cookie

The bearer header takes precedence when both are present. `/actuator/health`
and `/actuator/info` remain public. Role-based or ownership authorization is
not implemented.

## Not Implemented Yet

Email delivery, notification preferences API, role-based authorization,
ownership filtering by authenticated user, status updates, delete, and
mark-as-read remain for later branches.
