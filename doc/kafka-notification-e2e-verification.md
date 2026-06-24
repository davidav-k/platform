# Kafka Notification E2E Verification And Cutover

This document verifies the default local Kafka notification path without
removing the existing synchronous REST notification code.

The default runtime is Kafka-based notification delivery:

- task-service outbox publisher is enabled
- task-service outbox publisher adapter is `kafka`
- notification-service Kafka consumer is enabled
- task-service REST assignment notifications are disabled

Use this flow for local/dev verification and rollback validation.

Kafka notification mode is the recommended runtime mode after verification:

```text
Task Assignment
-> Task Service
-> Outbox Event
-> Kafka
-> Notification Consumer
-> Notification Created
```

The synchronous REST assignment notification code remains available for
configuration-only rollback.

## Prerequisites

- Docker Desktop or a local Docker daemon is running.
- `.env` exists at the repository root.
- Local stack can pass:

```bash
./scripts/check-local-stack.sh
```

- A verified regular user exists for the Postman MVP flow, or you are ready to
  complete registration verification through MailHog.

## Runtime Modes

Default Kafka notification mode:

```text
OUTBOX_PUBLISHER_ENABLED=true
OUTBOX_PUBLISHER_ADAPTER=kafka
OUTBOX_PUBLISHER_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
OUTBOX_PUBLISHER_KAFKA_TOPIC=platform.task-events
NOTIFICATION_KAFKA_ENABLED=true
NOTIFICATION_KAFKA_TOPIC=platform.task-events
NOTIFICATION_ASSIGNMENT_REST_ENABLED=false
```

Rollback mode:

```text
OUTBOX_PUBLISHER_ENABLED=false
OUTBOX_PUBLISHER_ADAPTER=logging
NOTIFICATION_KAFKA_ENABLED=false
NOTIFICATION_ASSIGNMENT_REST_ENABLED=true
```

Keep these defaults unless you intentionally need different local ports or
topic names:

```text
KAFKA_LOCAL_PORT=9092
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
KAFKA_TASK_EVENTS_TOPIC=platform.task-events
```

Start or recreate the local stack:

```bash
docker compose --env-file .env -f compose.yml up -d --build
./scripts/check-local-stack.sh
```

`NOTIFICATION_ASSIGNMENT_REST_ENABLED=false` disables only the task-service
synchronous REST assignment notification call. It does not remove
`RestNotificationClient`, `TaskNotificationPublisher`, or the
notification-service internal REST endpoint.

If task-service starts with Kafka publishing enabled and REST assignment
notifications disabled while `NOTIFICATION_KAFKA_ENABLED=false`, it logs a
warning. Startup is not blocked, but task assignment notifications may not be
delivered until the notification-service Kafka consumer is enabled.

## Create A Verifiable Event

Use the existing Postman MVP flow documented in
[doc/postman/README.md](postman/README.md).

The relevant step is the request that creates an assigned task:

```text
POST /api/tasks
```

with an `assigneeUserId` different from the creator. The existing collection
creates an assigned task for the admin recipient after the regular user is
logged in.

A manual request through Gateway is also valid if you already have a session:

```http
POST http://localhost:8080/api/tasks
Content-Type: application/json

{
  "title": "Kafka E2E assigned task",
  "description": "Verifies Kafka notification flow",
  "priority": "HIGH",
  "assigneeUserId": "<recipient-user-id>"
}
```

## Run The Verification Script

After creating the assigned task, run:

```bash
./scripts/verify-kafka-notification-flow.sh
```

The script checks:

- default Kafka notification env values are enabled
- REST assignment notifications are disabled
- Docker Compose services are running
- Kafka broker is reachable
- latest `TASK_ASSIGNED` outbox event is `PROCESSED`
- `event_consumption_log` contains exactly one row for the event
- `notifications` contains exactly one `TASK_ASSIGNED` notification for the
  task and recipient
- task-service logs contain the REST-assignment skip message when available

The script does not perform login or create users/tasks. It intentionally does
not hardcode local credentials or depend on mutable user state.

## Operational Runbook

Phase A: verify Kafka consumer is enabled.

```text
NOTIFICATION_KAFKA_ENABLED=true
NOTIFICATION_KAFKA_TOPIC=platform.task-events
```

Start or recreate notification-service and verify startup logs show
`kafkaConsumerEnabled=true`.

Phase B: verify Kafka producer is enabled.

```text
OUTBOX_PUBLISHER_ENABLED=true
OUTBOX_PUBLISHER_ADAPTER=kafka
OUTBOX_PUBLISHER_KAFKA_TOPIC=platform.task-events
OUTBOX_PUBLISHER_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
```

Create or update a task and verify new outbox events move to `PROCESSED`.

Phase C: verify REST assignment notifications are disabled.

```text
NOTIFICATION_ASSIGNMENT_REST_ENABLED=false
```

This skips only synchronous REST assignment notification sending from
task-service. REST code and notification-service internal endpoints remain in
place.

Phase D: observe notification creation.

Verify:

- `outbox_events.status = PROCESSED`
- Kafka topic receives `platform.task-events` messages
- `event_consumption_log` has exactly one row for the `event_id`
- `notifications` has exactly one notification for the task/recipient pair
- task-service logs show the REST assignment notification path was skipped

Phase E: roll back if necessary.

Use the rollback mode values below and recreate affected services. No code
change is required.

## Operational Verification

Inspect the latest task assignment outbox event:

```bash
docker exec -e PGPASSWORD="$POSTGRES_PASSWORD" tsp_postgres \
  psql -U "$POSTGRES_USER" -d "${TASK_POSTGRES_DB:-tasks_db}" \
  -c "SELECT event_id, aggregate_id, event_type, status, processed_at FROM outbox_events WHERE event_type = 'TASK_ASSIGNED' ORDER BY created_at DESC LIMIT 5;"
```

Expected:

- `event_type = TASK_ASSIGNED`
- `status = PROCESSED`
- `processed_at` is not null

Inspect the notification consumer idempotency log:

```bash
docker exec -e PGPASSWORD="$POSTGRES_PASSWORD" tsp_postgres \
  psql -U "$POSTGRES_USER" -d "${NOTIFICATION_POSTGRES_DB:-notifications_db}" \
  -c "SELECT event_id, event_type, consumed_at, source FROM event_consumption_log ORDER BY consumed_at DESC LIMIT 5;"
```

Expected:

- exactly one row for the verified `event_id`
- `event_type = TASK_ASSIGNED`
- `source = task-service`

Inspect the notification row:

```bash
docker exec -e PGPASSWORD="$POSTGRES_PASSWORD" tsp_postgres \
  psql -U "$POSTGRES_USER" -d "${NOTIFICATION_POSTGRES_DB:-notifications_db}" \
  -c "SELECT notification_id, recipient_user_id, type, source_service, source_entity_type, source_entity_id FROM notifications WHERE type = 'TASK_ASSIGNED' ORDER BY created_at DESC LIMIT 5;"
```

Expected:

- exactly one notification for the verified task/recipient pair
- `source_service = task-service`
- `source_entity_type = TASK`
- `source_entity_id` equals the task ID from the outbox event

Inspect Kafka topic traffic:

```bash
docker exec tsp_kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic platform.task-events \
  --from-beginning \
  --max-messages 5
```

Expected:

- messages contain `eventType`
- task assignment messages use `TASK_ASSIGNED`
- `eventId` matches the idempotency key stored by notification-service

## Duplicate Delivery Protection

Duplicate Kafka delivery is expected under at-least-once delivery. The
notification-service consumer uses `event_id` as the durable idempotency key.

Verification options:

- Run the Docker-backed integration test:

```bash
mvn -B -f backend/notification-service/pom.xml -Dtest=NotificationKafkaDuplicateDeliveryIntegrationTest test
```

- Or verify manually that `event_consumption_log` has one row for the event and
  `notifications` has one matching notification for the same task/recipient.

## REST Assignment Path Verification

With `NOTIFICATION_ASSIGNMENT_REST_ENABLED=false`, task-service should skip the
synchronous REST assignment notification path while still writing outbox events.

Check task-service logs:

```bash
docker compose --env-file .env -f compose.yml logs task-service \
  | grep "Skipping synchronous REST assignment notification"
```

If the log is absent, check that:

- the stack was recreated after changing `.env`
- the task was created with an assignee in the create-task request
- `NOTIFICATION_ASSIGNMENT_REST_ENABLED=false` is present in `.env`

The assignment endpoint writes `TASK_ASSIGNED` outbox events, but the legacy
synchronous REST assignment notification path currently runs from task
creation with an assignee.

## Rollback Runbook

Set these values in `.env`:

```text
OUTBOX_PUBLISHER_ENABLED=false
OUTBOX_PUBLISHER_ADAPTER=logging
NOTIFICATION_KAFKA_ENABLED=false
NOTIFICATION_ASSIGNMENT_REST_ENABLED=true
```

Then recreate the stack:

```bash
docker compose --env-file .env -f compose.yml up -d --build
```

This returns local behavior to the REST fallback: synchronous REST assignment
notifications are active, Kafka notification consumption is disabled, and
Kafka publishing is not used.
