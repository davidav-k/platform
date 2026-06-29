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
  -> notification-service NotificationEventConsumer
  -> TaskEventNotificationProcessor
  -> notifications
```

Task-service publishes task notification intent through `outbox_events` and
Kafka. Notification-service may still expose internal notification endpoints for
other system use cases, but task creation notifications are produced through
Kafka only.

## Ownership

- `task-service` owns task persistence and task domain events.
- `notification-service` owns notification persistence and delivery state.
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

## Verification

Use the Kafka verification guide:

- [Kafka notification E2E verification](../kafka-notification-e2e-verification.md)

The key database checks are:

```sql
select event_id, event_type, aggregate_id, status, error_message
from outbox_events
order by created_at desc
limit 10;

select event_id, event_type, consumed_at, source
from event_consumption_log
order by consumed_at desc
limit 10;

select notification_id, recipient_user_id, type, channel, status,
       source_service, source_entity_type, source_entity_id
from notifications
order by created_at desc
limit 10;
```
