# Outbox Pattern Design

## Current State

Task notification delivery uses the Outbox Pattern with Kafka:

```text
task-service transaction
  -> tasks
  -> outbox_events
  -> OutboxEventPollingScheduler
  -> KafkaOutboxEventPublisher
  -> Kafka topic platform.task-events
     -> notification-service NotificationEventConsumer -> notifications
     -> audit-service TaskEventConsumer
        -> TaskAuditEventNormalizer -> CreateAuditRecordUseCase -> audit_records

user-service transaction
  -> users / credentials / login state
  -> outbox_events
  -> OutboxEventPollingScheduler
  -> KafkaOutboxEventPublisher
  -> Kafka topic platform.user-events
     -> audit-service UserEventConsumer
        -> UserAuditEventNormalizer -> CreateAuditRecordUseCase -> audit_records

notification-service transaction
  -> notifications
  -> outbox_events
  -> OutboxEventPollingScheduler
  -> KafkaOutboxEventPublisher
  -> Kafka topic platform.notification-events
     -> audit-service NotificationAuditEventConsumer
        -> NotificationAuditEventNormalizer -> CreateAuditRecordUseCase -> audit_records
```

## Ownership

- `task-service` owns task persistence and task domain events.
- `user-service` owns user persistence and user lifecycle/authentication events.
- `notification-service` owns notification persistence, incoming task-event
  idempotency, and outgoing notification audit events.
- `audit-service` owns audit record persistence and consumes task, user, and
  notification events.
- Kafka is the transport for task domain events between the services.
- Kafka transports user events to Audit Service on a dedicated topic.
- Kafka transports notification audit events to Audit Service on a dedicated
  topic.
- Each service owns its database; no cross-service repositories or foreign keys
  are used.

## Task Events

Task-service currently writes these outbox events:

| Use case | Event type | Producer |
| --- | --- | --- |
| Create task | `TASK_CREATED` | `CreateTaskUseCaseImpl` |
| Assign, reassign, or unassign task | `TASK_ASSIGNED` | `AssignTaskUseCaseImpl` |
| Change task status | `TASK_STATUS_CHANGED` | `ChangeTaskStatusUseCaseImpl` |
| Update task fields | `TASK_UPDATED` | `UpdateTaskUseCaseImpl` |
| Soft-delete task | `TASK_DELETED` | `DeleteTaskUseCaseImpl` |

Each use case saves the task mutation and its outbox event in the same
transaction.

`TASK_CREATED` payload contains:

- `taskId`
- `title`
- `description`
- `status`
- `priority`
- `assigneeUserId`
- `createdByUserId`
- `createdAt`

`TASK_ASSIGNED` payload contains `taskId`, `title`, `status`, `priority`,
`previousAssigneeUserId`, `newAssigneeUserId`, `createdByUserId`, and
`updatedAt`.

`TASK_STATUS_CHANGED` payload contains `taskId`, `title`, `previousStatus`,
`newStatus`, `priority`, `assigneeUserId`, `createdByUserId`, and `updatedAt`.

Notification-service creates notification rows only when the event payload
contains the recipient required by the processor. Unassigned task creation
events, assignment events without `newAssigneeUserId`, and status events
without `assigneeUserId` are consumed and logged but do not create notification
rows.

Audit-service stores all five supported Task Service event types. It preserves
the source envelope metadata and JSON payload. `TASK_CREATED` uses
`createdByUserId` as the actor; assignment and status events leave the actor
null because their current payloads do not identify the acting user.

Audit normalization uses these stable internal actions:

| Event type | Audit action |
| --- | --- |
| `TASK_CREATED` | `CREATE_TASK` |
| `TASK_ASSIGNED` | `ASSIGN_TASK` |
| `TASK_STATUS_CHANGED` | `CHANGE_TASK_STATUS` |
| `TASK_UPDATED` | `UPDATE_TASK` |
| `TASK_DELETED` | `DELETE_TASK` |

Unsupported task event types are logged and acknowledged without creating an
audit record.

## User Events

User-service writes these events to its local outbox for business flows that
already exist:

| Flow | Event type |
| --- | --- |
| Registration | `USER_REGISTERED` |
| Successful authentication | `USER_LOGIN_SUCCESS` |
| Failed authentication | `USER_LOGIN_FAILED` |
| Profile update | `USER_PROFILE_UPDATED` |
| Account deletion | `USER_DELETED` |
| Password change | `PASSWORD_CHANGED` |
| MFA enable | `MFA_ENABLED` |

User mutation events share their existing transaction. Failed authentication
uses a dedicated transaction to durably record the attempt without changing
the login response. Payloads are whitelist-based and exclude passwords,
tokens, MFA secrets, QR-code secrets, confirmation keys, and reset tokens.
The existing in-process registration email event remains unchanged.

User-service publishing is controlled independently by
`USER_OUTBOX_PUBLISHER_*` variables and publishes to `platform.user-events`.
Audit Service consumes the seven currently published event types, normalizes
them, recursively removes sensitive payload fields, and uses `eventId` for
idempotent persistence.

## Notification Events

Notification-service writes `NOTIFICATION_CREATED` for the public create use
case and `NOTIFICATION_SYSTEM_CREATED` for internal and task-event-driven
system creation. No read, delete, sent, or delivery-failed event is published
because those mutation flows do not exist yet.

The payload is whitelist-based and contains notification/recipient IDs, type,
channel, status, source metadata, and lifecycle timestamps. Subject, body,
JWTs, cookies, and request headers are excluded by the producer. Audit Service
also removes technical credential/header fields recursively before persistence.

## Publisher Configuration

Task-service outbox publishing is controlled by:

```text
OUTBOX_PUBLISHER_ENABLED=true
OUTBOX_PUBLISHER_ADAPTER=kafka
OUTBOX_PUBLISHER_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
OUTBOX_PUBLISHER_KAFKA_TOPIC=platform.task-events
```

`OUTBOX_PUBLISHER_KAFKA_BOOTSTRAP_SERVERS` falls back to
`KAFKA_BOOTSTRAP_SERVERS`, and `OUTBOX_PUBLISHER_KAFKA_TOPIC` falls back to
`KAFKA_TASK_EVENTS_TOPIC`.

Inside Docker, bootstrap servers must use `kafka:9092`, not `localhost:9092`.

Notification-service publishing is controlled independently by:

```text
NOTIFICATION_OUTBOX_PUBLISHER_ENABLED=true
NOTIFICATION_OUTBOX_PUBLISHER_ADAPTER=kafka
NOTIFICATION_OUTBOX_PUBLISHER_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
NOTIFICATION_OUTBOX_PUBLISHER_KAFKA_TOPIC=platform.notification-events
```

## Consumer Configuration

Notification-service Kafka processing is controlled by:

```text
NOTIFICATION_KAFKA_ENABLED=true
NOTIFICATION_KAFKA_TOPIC=platform.task-events
```

`NOTIFICATION_KAFKA_TOPIC` falls back to `KAFKA_TASK_EVENTS_TOPIC`.

Audit-service Kafka processing is controlled independently by:

```text
AUDIT_KAFKA_ENABLED=true
AUDIT_KAFKA_TOPIC=platform.task-events
AUDIT_KAFKA_USER_TOPIC=platform.user-events
AUDIT_KAFKA_NOTIFICATION_TOPIC=platform.notification-events
```

It uses consumer group `audit-service` and independent listeners for task,
user, and notification topics.

## Failure Handling

If publishing fails, task-service keeps the outbox event in a retryable state
and records the error message on the event. Publish diagnostics should include:

- `eventId`
- `eventType`
- `aggregateId`
- `error_message`

Notification-service records consumed task events in `event_consumption_log`
and produced notification events in `outbox_events`. Publisher and consumer
diagnostics identify envelopes without logging JWTs, cookies, authorization
headers, passwords, message bodies, or secrets.
Audit-service uses the unique `audit_records.event_id` constraint as its
idempotency backstop and ignores events already present through the persistence
use case.
