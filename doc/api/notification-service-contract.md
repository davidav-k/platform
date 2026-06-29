# Notification Service API Contract

## Scope

This document describes the implemented `notification-service` HTTP endpoints
and the Kafka-backed task notification flow. The service owns notification
persistence and event-consumption idempotency. It does not own task state or
user profiles.

All external endpoints:

- use the response envelope from [API contract standards](api-contract-standards.md)
- require a valid access JWT
- use UUID strings for notification identifiers and public user references

The current implementation does not enforce recipient ownership filtering based
on the authenticated user. Role-based notification authorization is not
implemented yet.

## DTOs

### NotificationResponse

| Field | Type | Description |
| --- | --- | --- |
| `notificationId` | UUID string | Notification identifier |
| `recipientUserId` | UUID string | Public identifier of recipient |
| `type` | enum | `TASK_ASSIGNED`, `TASK_CREATED`, or `SYSTEM` |
| `channel` | enum | `EMAIL` or `IN_APP` |
| `subject` | string or null | Display subject |
| `body` | string | Notification content |
| `status` | enum | `PENDING`, `SENT`, or `FAILED` |
| `createdAt` | string | ISO-8601 timestamp |
| `updatedAt` | string | ISO-8601 timestamp |
| `sentAt` | string or null | ISO-8601 timestamp for sent notifications |
| `failureReason` | string or null | Failure reason when status is `FAILED` |

### CreateNotificationRequest

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `recipientUserId` | UUID string | yes | Recipient public user identifier |
| `type` | enum | yes | `TASK_ASSIGNED`, `TASK_CREATED`, or `SYSTEM` |
| `channel` | enum | yes | `EMAIL` or `IN_APP` |
| `subject` | string | no | Maximum 255 characters |
| `body` | string | yes | Not blank; maximum 5000 characters |

### CreateSystemNotificationRequest

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `recipientUserId` | UUID string | yes | Recipient public user identifier |
| `type` | enum | yes | `TASK_ASSIGNED`, `TASK_CREATED`, or `SYSTEM` |
| `title` | string | no | Maximum 255 characters |
| `message` | string | yes | Not blank; maximum 5000 characters |
| `sourceService` | string | yes | Not blank; maximum 100 characters |
| `sourceEntityType` | string | yes | Not blank; maximum 100 characters |
| `sourceEntityId` | UUID string | yes | Related source entity identifier |

The system notification use case always persists `channel=IN_APP` and
`status=PENDING`.

## External Notification Endpoints

### `POST /api/v1/notifications`

Creates a notification record. This endpoint persists the row only; it does not
send email.

- Gateway route: `POST /api/notifications`
- Request DTO: `CreateNotificationRequest`
- Response: `201 CREATED`, `data.notification` contains `NotificationResponse`
- Authentication: required

### `GET /api/v1/notifications/{notificationId}`

Returns one notification by public UUID.

- Gateway route: `GET /api/notifications/{notificationId}`
- Request DTO: none
- Response: `200 OK`, `data.notification` contains `NotificationResponse`
- Authentication: required
- Validation: `notificationId` must be a UUID

### `GET /api/v1/notifications`

Returns a filtered, paginated notification list.

- Gateway route: `GET /api/notifications`
- Request DTO: none
- Response: `200 OK`, `data.items` contains `NotificationResponse` entries and
  `data.page` contains pagination metadata
- Authentication: required
- Optional filters: `recipientUserId`, `status`, `channel`, `type`
- Pagination: `page` defaults to `0`; `size` defaults to `20` and is capped at `100`
- Sorting: `sort` defaults to `createdAt,desc`

## Internal Notification Endpoint

### `POST /internal/api/v1/notifications/system`

Creates an in-application system notification for platform services. API
Gateway does not route this endpoint.

- Request DTO: `CreateSystemNotificationRequest`
- Response: `201 CREATED`, `data.notification` contains `NotificationResponse`
- Authentication: valid access JWT according to notification-service security
- Persisted values: `channel=IN_APP`, `status=PENDING`

Task-service no longer uses this REST endpoint for task-created notifications.
Those notifications are created by Kafka event processing.

## Kafka Task Notifications

The supported task notification path is:

```text
task-service -> outbox_events -> Kafka topic platform.task-events
  -> notification-service -> notifications
```

`NotificationEventConsumer` listens to `notification.kafka.topic` when
`notification.kafka.enabled=true`. It stores each accepted Kafka event ID in
`event_consumption_log`; the unique `event_id` acts as the idempotency key.

`TaskEventNotificationProcessor` currently handles:

| Event type | Notification result |
| --- | --- |
| `TASK_CREATED` | Creates `IN_APP` `TASK_CREATED` for `assigneeUserId` when present |
| `TASK_ASSIGNED` | Creates `IN_APP` `TASK_ASSIGNED` for `newAssigneeUserId` when present |
| `TASK_STATUS_CHANGED` | Creates `IN_APP` `SYSTEM` for `assigneeUserId` when present |

Events missing the required recipient user ID are consumed and logged, but no
notification row is created.

## Not Implemented

- Notification preferences HTTP API
- Mark-as-read or read state
- Delete notification endpoint
- Notification status update endpoint
- SMTP/email delivery from notification-service
- WebSocket, push, or realtime notification delivery
