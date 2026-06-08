# Notification Service

## Status

Bootstrap module created. The service is expected to use port `8087` in a
future deployment, but it is not wired into Docker Compose yet.

## Configuration and Database

Runtime configuration is served by Config Server from
`config/notification-service-dev.yml`. Flyway owns schema migrations, and the
database name defaults to `notifications_db`.

## Domain Model

Domain entities, response DTOs, mappers, and Spring Data repositories are
implemented for notifications and notification preferences.

## API

`POST /api/v1/notifications` creates a notification record. This endpoint does
not send email and does not require security in this branch. It is not routed
through the API Gateway yet.

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

These are internal service paths. Gateway routing and security are not
implemented, and notification creation still does not perform email delivery.

## Not Implemented Yet

Security, Gateway routing, Docker Compose integration, Kafka, email delivery,
task-service integration, notification preferences API, status updates, delete,
and mark-as-read remain for later branches.
