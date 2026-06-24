# Outbox Pattern Design

## Status

This document describes the Platform outbox architecture and delivery
semantics. The first implemented producer is `task-service`, which persists
task outbox events and has a local publisher foundation with polling, claiming,
retry, and status transitions.

The current MVP continues to use its existing synchronous and in-process
integrations. Kafka infrastructure exists for local development, and
`task-service` can publish outbox events through `KafkaOutboxEventPublisher`
when explicitly enabled. `notification-service` has Kafka consumer support,
durable `event_id` idempotency through `event_consumption_log`, and task-event
notification processing behind `notification.kafka.enabled`.

Kafka publishing and notification consumption remain disabled by default. The
existing synchronous REST notification flow remains active and unchanged.
Notification cutover has not happened.

`task-service` now has an explicit runtime control for the synchronous REST
assignment notification path:

```yaml
notification-service:
  assignment-rest-enabled: true
```

The default is `true`, so MVP behavior is unchanged. Setting it to `false`
skips only task assignment notifications sent through the synchronous REST
path; it does not remove `RestNotificationClient`, `TaskNotificationPublisher`,
the notification-service internal REST endpoint, or task outbox event writing.

## 1. Problem Statement

The MVP uses synchronous service-to-service communication for task assignment
notifications. When `task-service` creates a task with an assignee that is not
the creator, it persists the task and then calls an internal
`notification-service` REST endpoint through `RestNotificationClient`. The
notification call has independent transactional state from the task database
transaction.

This model has several limitations:

- availability coupling: notification-service latency or failure affects the
  task workflow and requires special failure handling
- failure propagation: network errors, timeouts, and downstream errors occur
  inside the initiating request path
- dual-write risk: committing domain state and producing an integration
  message are separate operations, so one may succeed while the other fails
- retry ambiguity: a caller may not know whether a timed-out downstream
  request was processed, which can create duplicate effects
- authentication coupling: the current task notification call delegates the
  initiating user's JWT to another service
- limited scalability: adding consumers requires more direct dependencies or
  changes to the producer

`user-service` also publishes local Spring application events for registration
email handling. Those events are useful inside one process but are not durable
cross-service messages and do not survive a process failure.

The platform needs a reliable way to publish domain changes without a
distributed transaction between a service database and a message broker.

## 2. Goals

- Reliably capture publishable domain events with the business transaction.
- Preserve service autonomy and database ownership.
- Reduce direct runtime coupling between producers and consumers.
- Support eventual consistency across service boundaries.
- Allow independent consumers such as notifications and future audit services.
- Make publication failures, retries, and delivery lag observable.
- Support replay-safe, idempotent processing of duplicate deliveries.
- Provide versioned, auditable event contracts.
- Permit incremental migration without breaking current MVP behavior.
- Remain compatible with Java 17, Spring Boot 3.4.x, Spring Cloud 2024.x,
  PostgreSQL, and Flyway.

## 3. Non-Goals

- Replacing synchronous REST notification behavior in the current runtime.
- Removing task-service `RestNotificationClient` or `TaskNotificationPublisher`.
- Enabling Kafka publishing or notification consumption by default.
- Performing notification cutover in the same branch as infrastructure,
  producer, consumer, or test hardening work.
- Adding RabbitMQ, Spring Cloud Stream, or another broker abstraction.
- Adding or changing task-service business use cases for cutover.
- Implementing distributed transactions or two-phase commit.
- Guaranteeing immediate consistency between services.
- Guaranteeing exactly-once end-to-end processing.
- Replacing service-owned databases with a shared database.
- Implementing event sourcing or rebuilding aggregate state from events.
- Defining every final event field, topic, exchange, queue, or retention value.
- Replacing external HTTP APIs or API Gateway routing with messaging.
- Using events for request/response queries that require an immediate answer.

## 4. Current Architecture

### `user-service`

Owns users, stable public user identifiers, credentials, roles, authentication,
MFA, profiles, and account lifecycle. Registration currently publishes an
in-process Spring application event used for account email handling. No durable
integration event is published.

### `task-service`

Owns tasks, assignments, status changes, and task deletion state. Creating a
task with a non-self assignee currently triggers a best-effort synchronous REST
call to notification-service after the task has been persisted. The flow is:

```text
CreateTaskUseCaseImpl
  -> TaskRepository.saveAndFlush(task)
  -> TaskNotificationPublisherImpl.notifyTaskAssigned(...)
  -> RestNotificationClient.createNotification(...)
  -> POST /internal/api/v1/notifications/system
```

The REST request delegates the initiating request's access token to
notification-service. Notification failure is logged and does not fail the task
response.

`task-service` also persists durable outbox rows in the same transaction as
task business changes for:

- `TASK_CREATED` from `CreateTaskUseCaseImpl`
- `TASK_ASSIGNED` from `AssignTaskUseCaseImpl`
- `TASK_STATUS_CHANGED` from `ChangeTaskStatusUseCaseImpl`

The local outbox publisher foundation is implemented but disabled by default
through `outbox.publisher.enabled=false`. When enabled, it polls claimable
events and passes them to `OutboxEventPublisher`. The current
`OutboxEventPublisher` implementation logs event metadata only and does not
send messages externally, call notification-service, or replace the synchronous
REST notification flow.

Synchronous REST assignment notifications are controlled independently through
`notification-service.assignment-rest-enabled`. The effective behavior is:

| `notification-service.enabled` | `notification-service.assignment-rest-enabled` | Assignment REST notification behavior |
| --- | --- | --- |
| `false` | any value | skipped |
| `true` | `true` | sent when existing assignment conditions pass |
| `true` | `false` | skipped for Kafka notification cutover verification |

### `notification-service`

Owns notifications, preferences, and delivery state. It exposes authenticated
HTTP endpoints and an internal endpoint used by task-service to create a
system notification. The internal endpoint persists an `IN_APP` notification
with `PENDING` status and task source metadata supplied by task-service.

`notification-service` also has Kafka consumer support for task events. The
consumer is disabled by default through `notification.kafka.enabled=false`.
When explicitly enabled, it consumes the task-event envelope, checks
`event_consumption_log` by `event_id`, creates notifications for supported task
events, and records the consumed event in the same local transaction. Duplicate
Kafka deliveries with the same `eventId` are ignored and must not create a
second notification.

### Shared infrastructure

API Gateway routes external HTTP traffic and rejects invalid JWTs early.
Config Server supplies runtime configuration, and Eureka provides service
discovery. Each persistence service owns a separate PostgreSQL database and
Flyway migrations. Docker Compose includes a single-node Kafka broker for
local development.

## 5. Proposed Event-Driven Architecture

Each producing service records an integration event in its own outbox table in
the same local database transaction as the aggregate change. A separate
publisher reads unpublished records and delivers them through an adapter. The
current adapter logs only; the selected future adapter is Kafka-based.
Consumers process messages independently and update only their own databases.

Target task-notification flow:

```text
Task Service
  -> persist business entity
  -> persist outbox event in same transaction
  -> publisher reads outbox
  -> event delivered
  -> notification-service processes event
```

The API Gateway, Config Server, and Eureka retain their current responsibilities.
Future broker traffic is internal service communication and must not be routed
through API Gateway.

## 6. Outbox Pattern Overview

A producer handles a domain command as follows:

1. Validate the command and authorization according to existing service rules.
2. Update the business aggregate in the service-owned database.
3. Create an outbox record containing the integration event in the same local
   transaction.
4. Commit the aggregate change and outbox record atomically.
5. A background publisher reads eligible unpublished outbox records.
6. The publisher marks claimed events `PROCESSING`.
7. The publisher delivers each event through `OutboxEventPublisher`.
8. After successful delivery acknowledgement, the publisher marks the record
   `PROCESSED`.
9. After failed delivery, the publisher marks the record `FAILED`, increments
   `retry_count`, stores a short error message, and leaves it eligible for
   retry until the configured retry limit is reached.
10. Consumers process the event and commit changes in their own databases.

The same database commit contains both the business change and the intent to
publish. A committed aggregate cannot lose its event merely because the broker
was unavailable at commit time. The publisher can retry later.

A failure can still occur after broker publication but before the outbox row is
marked published. The event may then be published again. The design therefore
uses **at-least-once delivery** and requires idempotent consumers. It does not
claim exactly-once processing.

The selected delivery guarantee for Platform outbox delivery is:

- **At-least-once delivery** from outbox publisher to the eventual delivery
  channel.
- Consumers must be idempotent.
- Duplicate events are possible and expected.
- Ordering is best-effort per aggregate and is not globally guaranteed.

Per-aggregate ordering should be approximated by polling in `created_at` order
and, for broker delivery, routing events for the same aggregate to the same
partition or ordered lane where the selected broker supports it. Consumers
must still tolerate duplicates and stale or out-of-order events.

Two publisher implementation strategies may be evaluated later:

- polling publisher using database locking and bounded batches
- change data capture that streams committed outbox rows

The initial implementation should prefer the smallest operational footprint
that meets measured throughput and reliability needs. The choice is deferred.

## 7. Proposed Event Types

These are the initial task-service outbox event types expected for MVP
implementation. They are currently written by `task-service` to its local
outbox table and can be delivered to Kafka when the task-service publisher and
Kafka adapter are explicitly enabled.

| Event | Producer | Purpose |
| --- | --- | --- |
| `TASK_CREATED` | `task-service` | Announces task creation after task persistence |
| `TASK_ASSIGNED` | `task-service` | Announces assignment, reassignment, or unassignment after assignment state changes |
| `TASK_STATUS_CHANGED` | `task-service` | Announces an explicit status transition after task status persistence |

Broader events such as task deletion, user lifecycle events, and notification
events remain future candidates and require separate design review. Event names
use uppercase enum-style identifiers here to match the requested MVP outbox
scope and the existing `TASK_ASSIGNED` notification type.

## 8. Event Payload Guidelines

Current task-service outbox rows store metadata columns plus a JSON payload.
The persisted row data is the producer-side event source of record. Payloads
are immutable after creation; publishers and delivery adapters must not rewrite
event payloads.

A future transport envelope should contain:

| Field | Purpose |
| --- | --- |
| `eventId` | Globally unique event identifier used for tracing and idempotency |
| `eventType` | Stable logical event name, for example `TASK_ASSIGNED` |
| `aggregateId` | Stable public identifier of the changed aggregate |
| `aggregateType` | Aggregate category, for example `TASK` or `USER` |
| `occurredAt` | UTC ISO-8601 timestamp for the domain change |
| `version` | Event schema version; its exact format requires a contract decision |
| `payload` | Event-specific object containing the minimum data consumers need |

Recommended additional metadata to evaluate includes producer name,
correlation ID, causation ID, aggregate version, and trace context. Transport
metadata such as broker offsets or delivery tags does not belong in the
portable domain envelope.

The current `outbox_events` table does not have an `event_version` column. A
future migration should add an explicit `event_version` field before multiple
payload schema versions are active. This branch does not require adding that
field.

Payload rules:

- Use stable public UUIDs, never service-local numeric primary keys.
- Include only data required by known consumers.
- Prefer facts that occurred over commands telling another service how to
  mutate its state.
- Use UTC timestamps and explicit enum values.
- Make additive compatible changes where possible.
- Version incompatible schemas and define consumer transition rules.
- Do not place entities, JPA models, or service-internal DTOs on the wire.
- Treat `event_type` as the stable semantic meaning of the event.
- Keep payload schema changes backward compatible whenever possible.

Consumers must treat `eventId` as an idempotency key. A consumer should record
successful processing in its own database transaction with the resulting local
state change. Duplicate delivery of the same `eventId` must not create a second
notification or repeat another non-idempotent side effect.

For notification cutover, `notification-service` must use `event_id` based
deduplication before creating notification side effects. It should either store
processed event IDs in a consumer-owned table or enforce an equivalent unique
constraint tied to the resulting notification. The same `event_id` must not
create duplicate notifications.

Ordering is not globally guaranteed. Where order matters, routing should keep
events for one aggregate together and consumers should use aggregate versions
or equivalent rules to detect stale or out-of-order events.

## 9. Ownership and Service Responsibilities

### Task Service

- Remains the source of truth for task state.
- Owns task creation, assignment, status transitions, and task persistence.
- Creates task events in the task database transaction that changes the task.
- Publishes task event records through a task-service-owned publisher.
- Polls claimable `NEW` and retryable `FAILED` outbox events.
- Marks claimed events `PROCESSING` before delivery.
- Marks events `PROCESSED` only after successful delivery.
- Marks events `FAILED`, increments `retry_count`, and records a short error
  message after failed delivery.
- Controls retry eligibility with `outbox.publisher.max-retries`.
- Does not wait for notification creation to complete before committing a task.
- Does not embed notification delivery logic in task event publication.
- Keeps the current synchronous REST notification flow until a dedicated
  notification cutover branch removes it.

### Notification Service

- Remains the source of truth for notification records, notification
  preferences, notification channels, and delivery state.
- During the current MVP, receives task assignment notification requests
  through `POST /internal/api/v1/notifications/system`.
- In the target architecture, consumes relevant task events, initially from
  the approved MVP event set.
- Applies notification preferences and creates notification records in its own
  transaction.
- Deduplicates consumed events before creating side effects.
- Owns delivery retries and notification status independently of task-service.
- May publish `NotificationCreated` for approved downstream consumers.

### User Service

- Remains the source of truth for user lifecycle state.
- Publishes user lifecycle events from the same transaction as the lifecycle
  change once those lifecycle operations are implemented.
- Does not publish credentials, token values, MFA secrets, or unnecessary
  profile details.
- Continues to own account-verification behavior until a separate migration is
  explicitly designed.

Every service owns its outbox records and publisher operation. No central
service writes outbox rows on behalf of another service.

## 10. Reliability Considerations

### Retries

The current task-service publisher foundation increments `retry_count` after a
failed delivery attempt. `NEW` and `FAILED` events remain retryable while their
`retry_count` is less than `outbox.publisher.max-retries`. Once the maximum is
reached, events remain `FAILED` for manual inspection or future recovery work.

Retry backoff, jitter, next-attempt scheduling, dead-letter handling, and
operator replay are future work. A broker adapter should distinguish
transient delivery failures from invalid event data or permanent business
rejection.

### Duplicate delivery

Duplicate messages are expected under at-least-once delivery. Consumers must
be idempotent and persist processed `eventId` values or an equivalent unique
constraint in the same transaction as their local side effects.

### Poison messages

Messages that repeatedly fail must stop blocking normal processing. The broker
design must define a dead-letter topic or queue, retry limits, operator alerts,
safe inspection, and controlled replay after correction.

### Ordering and concurrency

Ordering guarantees should be scoped per aggregate, not globally. Publishers
must avoid concurrently publishing the same outbox row. Consumers must handle
concurrent delivery and stale aggregate versions safely.

### Event lifecycle

Outbox records should use this initial lifecycle:

| Status | Meaning |
| --- | --- |
| `NEW` | Event was persisted with the aggregate transaction and is eligible for publication |
| `PROCESSING` | A publisher has claimed the event for delivery |
| `PROCESSED` | Event delivery was acknowledged and no further publication is pending |
| `FAILED` | Event failed publication and requires retry or operator handling according to retry policy |

Current task-service producer behavior is:

```text
business use case transaction
  -> persist task change
  -> persist outbox event with status NEW

publisher polling transaction
  -> select claimable NEW/FAILED events ordered by created_at
  -> mark selected events PROCESSING

delivery attempt
  -> call OutboxEventPublisher.publish(outboxEvent)
  -> on success, mark PROCESSED and set processed_at
  -> on failure, mark FAILED, increment retry_count, store error_message
```

### Publisher recovery and cleanup

Outbox records need explicit publication states, retry metadata, and retention.
Processed rows should be archived or deleted only according to an operational
retention policy. Stuck pending records require detection and recovery.

`PROCESSING` recovery for publisher crashes is not finalized. A future branch
should decide whether to add claim timestamps, ownership metadata, or a
timeout-based reset policy before enabling real production delivery.

## 11. Delivery Adapter Contract

`OutboxEventPublisher` is the adapter boundary between local outbox processing
and any future delivery technology.

The adapter contract is:

- Receive already persisted `OutboxEventEntity` data from the processor.
- Deliver using only event metadata and payload stored on the outbox row.
- Return normally only after the delivery channel has accepted the event.
- Throw an exception when delivery fails so the processor can mark the event
  `FAILED` and increment retry state.
- Not know task-service use case or business-rule internals.
- Not mutate task business entities or any non-outbox aggregate state.
- Not call `notification-service` directly.
- Not log full payloads by default.

The current local/development fallback, `LoggingOutboxEventPublisher`, is
intentionally logging/no-op only. `KafkaOutboxEventPublisher` is available as
the real task-service delivery adapter when the publisher is explicitly
configured to use Kafka. The adapter publishes to `platform.task-events` by
default and keeps outbox status transitions in `OutboxEventProcessor`.

## 12. Monitoring

At minimum, observe:

- unpublished outbox count and age of the oldest pending event
- publication throughput, latency, retries, and failures
- broker availability and consumer lag or queue depth
- consumer processing success, retries, deduplication count, and dead letters
- end-to-end latency from `occurredAt` to consumer completion

Logs and traces should carry `eventId`, correlation ID, event type, producer,
and aggregate ID without logging sensitive payloads.

## 13. Broker Decision: Kafka

Kafka is selected as the broker for Platform domain events. Local Docker
Compose infrastructure and the task-service producer adapter are implemented,
but publishing remains opt-in and disabled by default.

Kafka is the better fit for this project because it provides:

- a durable event log for domain events
- scalable consumer groups for independent services
- replay-friendly retained events for future consumers
- partition-based ordering where events use a stable aggregate key
- a strong fit for event-driven microservices
- a natural path for future notification, audit, and analytics services

RabbitMQ remains useful for command-style work queues and flexible routing, but
the Platform roadmap is closer to durable domain event streams with multiple
independent consumers.

| Consideration | Kafka | RabbitMQ |
| --- | --- | --- |
| Primary model | Durable ordered log with consumer offsets | Brokered queues and exchanges with acknowledgements |
| Replay | Strong fit for retained event replay and new consumer groups | Possible through topology and retention choices, but not the primary queue model |
| Ordering | Partition ordering; key design is important | Queue ordering with caveats for concurrency, retries, and redelivery |
| Routing | Topics and consumer groups | Flexible exchanges, routing keys, and queues |
| Typical fit | Event streams, multiple independent consumers, audit, analytics, long retention | Work queues, command-style delivery, flexible routing, simpler bounded workflows |
| Operational questions | Partition count, retention, rebalancing, schema governance | Exchange/queue topology, acknowledgements, dead lettering, queue growth |
| Spring integration | Spring for Apache Kafka | Spring AMQP |

Kafka and Spring Kafka versions should continue to follow the Spring Boot
3.4.x dependency baseline and be verified with Spring Cloud 2024.x during
future upgrades.

## 14. Initial Kafka Topic Strategy

The initial topic for task domain events is:

```text
platform.task-events
```

Topic plan:

| Item | Decision |
| --- | --- |
| Producer | `task-service` |
| Initial topic | `platform.task-events` |
| Consumers | `notification-service`; future `audit-service` |
| Event key | `aggregateId`, which is the task public UUID for task events |
| Event value | Kafka message envelope containing outbox metadata and payload |
| Ordering expectation | best-effort ordering per taskId partition key |

For task events, `aggregateId` is the task ID. Using it as the Kafka key should
route events for the same task to the same partition, preserving partition
order where Kafka and producer settings allow it. This does not create a global
ordering guarantee and does not remove the requirement for idempotent,
duplicate-safe consumers.

## 15. Kafka Message Envelope

The Kafka adapter publishes a transport envelope shaped as:

| Field | Source |
| --- | --- |
| `eventId` | `OutboxEventEntity.eventId` |
| `eventType` | `OutboxEventEntity.eventType` |
| `aggregateType` | `OutboxEventEntity.aggregateType` |
| `aggregateId` | `OutboxEventEntity.aggregateId` |
| `occurredAt` | outbox `createdAt` unless a future payload-specific domain timestamp is promoted |
| `eventVersion` | initial value decided in the implementation branch; recommended default is `1` |
| `payload` | parsed or embedded JSON from `OutboxEventEntity.payload` |

The envelope should preserve the immutable payload written by the task use
case. The adapter may wrap the payload with metadata, but must not change the
meaning of the payload fields. `eventVersion` is a transport/schema field; the
current `outbox_events` table does not have an `event_version` column, so the
initial Kafka adapter may use a configured/default value until a future
migration adds persisted event versions.

## 16. Kafka Outbox Adapter Design

`KafkaOutboxEventPublisher` implements `OutboxEventPublisher`.

Adapter responsibilities:

- Read the already persisted `OutboxEventEntity` supplied by
  `OutboxEventProcessor`.
- Map the entity to the Kafka message envelope.
- Send the message to the configured topic, initially `platform.task-events`.
- Use `aggregateId` as the Kafka record key.
- Return normally only after Kafka accepts the send according to the selected
  producer acknowledgement policy.
- Throw on failed send so `OutboxEventProcessor` can mark the event `FAILED`.

Adapter boundaries:

- Does not know task-service business logic.
- Does not call `notification-service`.
- Does not mutate task entities or any business aggregates.
- Does not own outbox status transitions; the processor remains responsible
  for `PROCESSING`, `PROCESSED`, and `FAILED`.
- Does not replace or remove the synchronous REST notification flow.

The existing `LoggingOutboxEventPublisher` may remain as a local/development
fallback if useful, but it must remain explicit and not be confused with real
delivery.

## 17. Kafka Retry and Idempotency Semantics

Kafka delivery keeps the selected outbox semantics:

- `eventId` is the idempotency key.
- Duplicate Kafka messages are possible.
- Consumers must handle duplicate event delivery safely.
- `notification-service` must persist consumed event IDs or enforce an
  equivalent unique constraint before notification cutover.
- The same `eventId` must not create duplicate notifications.

Retry behavior:

- A failed Kafka send causes the outbox event to become `FAILED`.
- `retry_count` is incremented after failed delivery.
- The event remains retryable until `outbox.publisher.max-retries`.
- After max retries, the event remains `FAILED` for manual inspection or
  future recovery.
- Dead-letter handling is future work and should be designed before production
  cutover.

## 18. Future Configuration Plan

Future task-service configuration:

```yaml
outbox:
  publisher:
    enabled: false
    batch-size: 20
    max-retries: 3
    fixed-delay: 5s
    adapter: kafka

platform:
  kafka:
    task-events-topic: platform.task-events
```

`outbox.publisher.enabled` remains false by default. The task-service Kafka
adapter is selected only when explicitly configured. `notification.kafka.enabled`
also remains false by default so Kafka-created notifications are opt-in during
verification.

## 19. MVP Migration Strategy

### Phase 1: Keep synchronous REST and add outbox persistence

Keep the existing task-to-notification REST flow. Add task-service outbox
persistence in the same local transaction as task persistence. Add no broker
dependency and no publisher. Use current tests as the behavior baseline and
verify that task rollback also rolls back the outbox insert.

Status: implemented for `TASK_CREATED`, `TASK_ASSIGNED`, and
`TASK_STATUS_CHANGED`.

### Phase 2: Add publisher

Introduce a task-service publisher that reads eligible outbox rows and marks
lifecycle progress. Keep the synchronous REST notification flow active. If a
broker is selected for this phase, choose it through a separate implementation
decision and version-compatibility check.

Status: local publisher foundation is implemented with polling, claiming,
retry, and status transitions. The delivery adapter is logging/no-op only and
`outbox.publisher.enabled=false` by default.

### Phase 2b: Add real delivery adapter

Add a real implementation behind `OutboxEventPublisher` without changing task
business use cases. The selected adapter target is Kafka. Add broker
infrastructure and dependencies only after compatibility and operational
requirements are verified. The adapter must preserve at-least-once semantics
and must not include task-service business logic.

### Phase 3: Add Kafka notification processing

Implement notification-service event consumption with durable idempotency and
controlled duplicate prevention. `notification-service` now supports creating
notifications from `TASK_CREATED`, `TASK_ASSIGNED`, and `TASK_STATUS_CHANGED`
events when `notification.kafka.enabled=true`. Processing and
`event_consumption_log` writes happen in the same local transaction, and the
consumption record is saved only after notification processing succeeds.

### Phase 4: Controlled notification cutover

Do not allow both synchronous REST and Kafka processing to create the same task
assignment notification in production. The safer cutover approach for this
project is:

1. Keep REST notification flow active.
2. Keep Kafka consumer disabled by default.
3. Enable Kafka producer and consumer together only in controlled local/dev
   environments first.
4. Before any dual-running environment can create user-visible task assignment
   notifications, disable REST assignment notification when Kafka notification
   consumer is enabled.
5. Verify Kafka-created notifications and duplicate suppression.
6. Remove task-service's direct notification REST call only in a dedicated
   cleanup branch after verification.

The alternative approach, adding a shared idempotency key across REST-created
and Kafka-created notifications, is possible but broader. It would require
coordinating identifiers across synchronous and asynchronous flows and changing
notification creation contracts. For the current platform, disabling the REST
assignment notification path only when Kafka notification consumer is enabled
is the smaller and safer cutover.

For local Kafka notification flow verification, use explicit runtime flags:

```text
OUTBOX_PUBLISHER_ENABLED=true
OUTBOX_PUBLISHER_ADAPTER=kafka
NOTIFICATION_KAFKA_ENABLED=true
NOTIFICATION_SERVICE_ASSIGNMENT_REST_ENABLED=false
```

Manual verification steps:

1. Start Docker Compose with Kafka, PostgreSQL, task-service, and
   notification-service.
2. Enable the task-service outbox publisher and select the Kafka adapter.
3. Enable the notification-service Kafka consumer.
4. Disable task-service REST assignment notifications with
   `NOTIFICATION_SERVICE_ASSIGNMENT_REST_ENABLED=false`.
5. Create or assign a task that has a non-creator assignee.
6. Verify notification-service creates the notification from the Kafka event.
7. Verify duplicate event delivery does not create a duplicate notification
   with the existing duplicate-delivery integration test.

REST notification code still exists and can be re-enabled by setting
`notification-service.assignment-rest-enabled=true`. Final removal remains a
future cleanup branch.

### Phase 5: Remove synchronous REST integration

After event publication and consumption meet defined acceptance criteria,
remove task-service's direct notification REST call and delegated JWT
propagation in a dedicated branch. Keep rollback controls during rollout and
update service contracts, runbooks, diagrams, and tests.

User lifecycle events can follow the same phases independently. Services do
not need to migrate all event types in one release.

## 20. Notification Cutover Strategy

The current synchronous REST notification flow remains active. Kafka
notification processing exists but is disabled by default through
`notification.kafka.enabled=false`. Duplicate delivery protection is
implemented through `event_consumption_log` and covered by unit tests plus a
PostgreSQL-backed integration test.

Cutover sequence:

1. Phase 1: keep REST notification flow active, keep Kafka consumer disabled
   by default, and verify tests.
2. Phase 2: enable Kafka consumer in local/dev only while keeping REST active;
   observe duplicate risk and operational behavior without production cutover.
3. Phase 3: prevent double notification creation before any dual-running
   user-visible environment. Prefer disabling REST assignment notification
   when Kafka notification consumer is enabled instead of creating the same
   notification through both paths.
4. Phase 4: enable Kafka publishing and Kafka consumer together in a controlled
   environment and verify notifications are created from events.
5. Phase 5: remove the synchronous REST notification flow only in a dedicated
   cleanup branch after verification.

## 21. Next Implementation Sequence

Recommended implementation order:

1. Keep Kafka producer and consumer disabled by default.
2. Run unit tests and Docker-backed integration tests for duplicate delivery.
3. Verify local/dev Kafka producer and consumer behavior with explicit flags.
4. Add operational metrics for consumer lag, duplicate count, processing
   failures, and notification creation latency.
5. Use `notification-service.assignment-rest-enabled=false` to disable REST
   assignment notifications when Kafka notification consumer is enabled.
6. Cut over notification creation from REST to events in a controlled branch.
7. Remove `task-service` synchronous REST notification integration in a
   dedicated branch after verification.

## 22. Database Design Sketch

Each producing service owns a table conceptually shaped as:

```text
outbox_events
- id
- event_id
- aggregate_type
- aggregate_id
- event_type
- payload
- status
- retry_count
- error_message
- created_at
- updated_at
- processed_at
- version
```

Likely implementation concerns include:

- numeric `id` for local persistence and UUID `event_id` for cross-service
  tracing and idempotency
- JSON payload storage with explicit schema version; `event_version` is a
  recommended future field because the current task-service table does not yet
  include it
- indexes supporting unpublished status and creation order
- retry count, next-attempt time, and last-error metadata
- optimistic or pessimistic claim/lock metadata for concurrent publishers
- retention and cleanup of published records

A consumer-side processed-event or inbox record is also likely necessary for
durable idempotency. Its exact schema is deferred. All schema changes must use
new Flyway migrations in the owning service; no shared outbox database or
cross-service foreign key is allowed.

## 23. Security Considerations

- Do not include passwords, password hashes, JWTs, refresh tokens, MFA secrets,
  verification keys, cookies, or credentials in events.
- Do not propagate end-user JWTs through asynchronous messages.
- Prefer stable identifiers over names, email addresses, phone numbers, or
  other personal information.
- Include personal data only when a documented consumer requirement and
  retention policy justify it.
- Authenticate and authorize service access to broker resources using service
  identities with least privilege.
- Encrypt broker connections and stored messages according to deployment
  requirements.
- Restrict producers to their owned event destinations and consumers to their
  required subscriptions.
- Treat event payloads as untrusted input at consumer boundaries and validate
  schema, size, enums, and identifiers.
- Redact sensitive payload fields from logs, traces, dead-letter inspection,
  and operational dashboards.
- Define retention and erasure implications before events contain personal
  data, especially for user lifecycle and notification content.

## 24. Open Questions

- Polling publisher or change data capture for the first implementation?
- Kafka partition count, retention, topic configuration, and producer
  acknowledgement policy?
- Event naming and schema-versioning convention?
- JSON with governed schemas, another serialization format, or a schema
  registry?
- Required per-aggregate ordering and aggregate-version semantics?
- Dead-letter ownership, alerting, inspection, replay, and data retention?
- Outbox and processed-event retention periods?
- How will correlation and trace context cross asynchronous boundaries?
- Which observability stack and service-level objectives will be used?
- How will schema compatibility be tested in CI across producers and consumers?
- Which event is the first production migration candidate: `TASK_ASSIGNED` or
  another lower-risk event?
- What acceptance criteria permit removal of the synchronous REST integration?
- What `event_version` convention should be used before adding incompatible
  payload versions?
- How should stuck `PROCESSING` rows be detected and reset safely?

## Decision Summary

The selected delivery semantics are service-owned transactional outbox records
with at-least-once delivery, idempotent consumers, duplicate-tolerant
processing, and best-effort per-aggregate ordering. Kafka is selected as the
broker for platform domain events, starting with `platform.task-events`
produced by `task-service` and keyed by task `aggregateId`. The current
implementation persists task-service outbox rows, can publish through Kafka
when explicitly enabled, and can create notification-service records from Kafka
events when explicitly enabled. Synchronous REST notification removal remains
future work and must happen only in a dedicated cutover branch.
