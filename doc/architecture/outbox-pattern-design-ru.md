# Outbox Pattern

## Текущее состояние

Доставка task notifications использует Outbox Pattern и Kafka:

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


## Task events

`task-service` пишет такие outbox events:

| Use case | Event type | Producer |
| --- | --- | --- |
| Create task | `TASK_CREATED` | `CreateTaskUseCaseImpl` |
| Assign, reassign или unassign task | `TASK_ASSIGNED` | `AssignTaskUseCaseImpl` |
| Change task status | `TASK_STATUS_CHANGED` | `ChangeTaskStatusUseCaseImpl` |

Каждый use case сохраняет изменение задачи и outbox event в одной транзакции.

`TASK_CREATED` payload содержит:

- `taskId`
- `title`
- `description`
- `status`
- `priority`
- `assigneeUserId`
- `createdByUserId`
- `createdAt`

`TASK_ASSIGNED` payload содержит `taskId`, `title`, `status`, `priority`,
`previousAssigneeUserId`, `newAssigneeUserId`, `createdByUserId`, `updatedAt`.

`TASK_STATUS_CHANGED` payload содержит `taskId`, `title`, `previousStatus`,
`newStatus`, `priority`, `assigneeUserId`, `createdByUserId`, `updatedAt`.

`notification-service` создаёт notification row только если в payload есть
recipient, нужный конкретному processor-у.

## Конфигурация task-service

```text
OUTBOX_PUBLISHER_ENABLED=true
OUTBOX_PUBLISHER_ADAPTER=kafka
OUTBOX_PUBLISHER_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
OUTBOX_PUBLISHER_KAFKA_TOPIC=platform.task-events
```

В Docker bootstrap servers должны указывать на `kafka:9092`.

## Конфигурация notification-service

```text
NOTIFICATION_KAFKA_ENABLED=true
NOTIFICATION_KAFKA_TOPIC=platform.task-events
```

`NotificationEventConsumer` сохраняет обработанные события в
`event_consumption_log`. Уникальный `event_id` используется как idempotency key
для повторных Kafka deliveries.
