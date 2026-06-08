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

## API

`POST /api/v1/notifications` creates a notification record. This endpoint does
not send email.

`GET /api/v1/notifications/{notificationId}` retrieves one notification by its
public UUID.

`GET /api/v1/notifications` lists notifications with optional
`recipientUserId`, `status`, `channel`, and `type` filters. Pagination uses
`page` and `size`; sorting uses `sort=field,direction` and defaults to
`createdAt,desc`.

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

Notification creation still does not perform email delivery.

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

## Authentication

Notification API endpoints require a valid access JWT. The service validates
tokens independently and accepts them from either:

* `Authorization: Bearer <token>`
* the `access-token` cookie

The bearer header takes precedence when both are present. `/actuator/health`
and `/actuator/info` remain public. Role-based or ownership authorization is
not implemented.

## Not Implemented Yet

Kafka, email delivery, task-service integration, notification preferences API,
role-based authorization, ownership filtering, status updates, delete, and
mark-as-read remain for later branches.
