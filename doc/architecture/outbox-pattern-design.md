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
     -> audit-service TaskEventConsumer -> audit_records
```

## Ownership

- `task-service` owns task persistence and task domain events.
- `notification-service` owns notification persistence and delivery state.
- `audit-service` owns audit record persistence and consumes Task Service
  events independently from notification-service.
- Kafka is the transport for task domain events between the services.
- Each service owns its database; no cross-service repositories or foreign keys
  are used.

## Task Events

Task-service currently writes these outbox events:

| Use case | Event type | Producer |
| --- | --- | --- |
| Create task | `TASK_CREATED` | `CreateTaskUseCaseImpl` |
| Assign, reassign, or unassign task | `TASK_ASSIGNED` | `AssignTaskUseCaseImpl` |
| Change task status | `TASK_STATUS_CHANGED` | `ChangeTaskStatusUseCaseImpl` |

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

Audit-service stores all three supported Task Service event types. It preserves
the source envelope metadata and JSON payload. `TASK_CREATED` uses
`createdByUserId` as the actor; assignment and status events leave the actor
null because their current payloads do not identify the acting user.

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
```

It uses consumer group `audit-service`, so notification and audit processing
both receive every Task Service event.

## Failure Handling

If publishing fails, task-service keeps the outbox event in a retryable state
and records the error message on the event. Publish diagnostics should include:

- `eventId`
- `eventType`
- `aggregateId`
- `error_message`

Notification-service records consumed event status in
`event_consumption_log`. Consumer diagnostics should identify the task event
without logging JWTs, cookies, authorization headers, passwords, or secrets.
Audit-service uses the unique `audit_records.event_id` constraint as its
idempotency backstop and ignores events already present through the persistence
use case.
