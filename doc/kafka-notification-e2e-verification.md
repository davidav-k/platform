# Kafka Notification E2E Verification

This document verifies the supported task notification path:

```text
task-service
  -> outbox_events
  -> Kafka topic platform.task-events
  -> notification-service
  -> notifications
```

Task-service no longer sends task assignment notifications directly to
notification-service over REST. Notifications for newly created assigned tasks
are created only from the `TASK_CREATED` outbox event consumed by
notification-service.

## Required Configuration

Local Docker configuration should keep the Kafka path enabled:

```text
OUTBOX_PUBLISHER_ENABLED=true
OUTBOX_PUBLISHER_ADAPTER=kafka
OUTBOX_PUBLISHER_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
OUTBOX_PUBLISHER_KAFKA_TOPIC=platform.task-events
NOTIFICATION_KAFKA_ENABLED=true
NOTIFICATION_KAFKA_TOPIC=platform.task-events
KAFKA_TASK_EVENTS_TOPIC=platform.task-events
```

Inside Docker, Kafka bootstrap servers must use `kafka:9092`, not
`localhost:9092`.

## Manual Check

1. Start the stack:

```bash
docker compose --env-file .env -f compose.yml up -d --build task-service notification-service
```

2. Create a task through the Gateway with a non-null `assigneeUserId`:

```bash
curl -i -X POST http://localhost:8080/api/tasks \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Kafka notification smoke test",
    "description": "Verify TASK_CREATED notification",
    "priority": "HIGH",
    "assigneeUserId": "11111111-1111-1111-1111-111111111111"
  }'
```

3. Confirm task-service wrote and published the outbox event:

```bash
docker logs tsp_task_service
```

Expected log indicators:

- `Published outbox event to Kafka`
- `eventType=TASK_CREATED`
- `topic=platform.task-events`

4. Confirm notification-service consumed the task event and saved a
   notification:

```bash
docker logs tsp_notification_service
```

Expected log indicators:

- `Received TASK_CREATED event`
- `Creating task event notification`
- `Created system notification`

5. Check PostgreSQL:

```sql
select event_id, event_type, aggregate_id, status, error_message
from outbox_events
order by created_at desc
limit 10;

select event_id, event_type, status, error_message
from event_consumption_log
order by consumed_at desc
limit 10;

select notification_id, recipient_user_id, type, channel, status,
       source_service, source_entity_type, source_entity_id
from notifications
order by created_at desc
limit 10;
```

Expected database state:

- `outbox_events.event_type = TASK_CREATED`
- `outbox_events.status = PROCESSED`
- `event_consumption_log.status = PROCESSED`
- `notifications.type = TASK_CREATED`
- `notifications.channel = IN_APP`
- `notifications.recipient_user_id` equals the task `assigneeUserId`
- `notifications.source_service = task-service`
- `notifications.source_entity_type = TASK`
- `notifications.source_entity_id` equals the task id

If the task is created without `assigneeUserId`, notification-service should log
that the `TASK_CREATED` notification was skipped and no notification row should
be inserted for that event.
