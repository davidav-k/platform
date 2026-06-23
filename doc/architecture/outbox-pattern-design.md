# Outbox Pattern Design

## Status

This document describes a proposed future event-driven communication model for
Platform, with the first implementation candidate scoped to task-service task
events. It is an architecture target, not implemented functionality.

The current MVP continues to use its existing synchronous and in-process
integrations until the migration phases in this document are implemented and
verified. This design-review branch does not introduce a broker, outbox schema,
entity, repository, publisher, consumer, scheduler, or infrastructure
configuration.

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

- Implementing Kafka, RabbitMQ, outbox tables, or message-processing code in
  this branch.
- Adding task-service outbox migrations, entities, repositories, publishers,
  schedulers, or consumers in this branch.
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
response. The separate task assignment endpoint currently updates assignment
state but does not publish a notification. Status changes are persisted by
task-service and do not currently notify notification-service.

### `notification-service`

Owns notifications, preferences, and delivery state. It exposes authenticated
HTTP endpoints and an internal endpoint used by task-service to create a
system notification. The internal endpoint persists an `IN_APP` notification
with `PENDING` status and task source metadata supplied by task-service. It
does not consume broker events.

### Shared infrastructure

API Gateway routes external HTTP traffic and rejects invalid JWTs early.
Config Server supplies runtime configuration, and Eureka provides service
discovery. Each persistence service owns a separate PostgreSQL database and
Flyway migrations. Docker Compose currently contains no message broker.

## 5. Proposed Event-Driven Architecture

Each producing service records an integration event in its own outbox table in
the same local database transaction as the aggregate change. A separate
publisher reads unpublished records and sends them to the selected broker.
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
6. The publisher sends each event to the broker.
7. After broker acknowledgement, the publisher marks the record as processed
   or otherwise records publication progress.
8. Consumers process the event and commit changes in their own databases.

The same database commit contains both the business change and the intent to
publish. A committed aggregate cannot lose its event merely because the broker
was unavailable at commit time. The publisher can retry later.

A failure can still occur after broker publication but before the outbox row is
marked published. The event may then be published again. The design therefore
uses **at-least-once delivery** and requires idempotent consumers. It does not
claim exactly-once processing.

Two publisher implementation strategies may be evaluated later:

- polling publisher using database locking and bounded batches
- change data capture that streams committed outbox rows

The initial implementation should prefer the smallest operational footprint
that meets measured throughput and reliability needs. The choice is deferred.

## 7. Proposed Event Types

These are the initial task-service outbox event types expected for MVP
implementation planning. They are not implemented contracts in this branch.

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

A common event envelope should contain:

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

Payload rules:

- Use stable public UUIDs, never service-local numeric primary keys.
- Include only data required by known consumers.
- Prefer facts that occurred over commands telling another service how to
  mutate its state.
- Use UTC timestamps and explicit enum values.
- Make additive compatible changes where possible.
- Version incompatible schemas and define consumer transition rules.
- Do not place entities, JPA models, or service-internal DTOs on the wire.

Consumers must treat `eventId` as an idempotency key. A consumer should record
successful processing in its own database transaction with the resulting local
state change. Duplicate delivery of the same `eventId` must not create a second
notification or repeat another non-idempotent side effect.

Ordering is not globally guaranteed. Where order matters, routing should keep
events for one aggregate together and consumers should use aggregate versions
or equivalent rules to detect stale or out-of-order events.

## 9. Ownership and Service Responsibilities

### Task Service

- Remains the source of truth for task state.
- Owns task creation, assignment, status transitions, and task persistence.
- Creates task events in the task database transaction that changes the task.
- Publishes task event records through a task-service-owned publisher.
- Does not wait for notification creation to complete before committing a task.
- Does not embed notification delivery logic in task event publication.
- During Phase 1, keeps the current synchronous REST notification flow while
  adding outbox persistence.

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

Publishers and consumers require bounded retries with backoff and jitter.
Retry policy must distinguish transient failures from invalid messages or
permanent business rejection.

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

### Publisher recovery and cleanup

Outbox records need explicit publication states, retry metadata, and retention.
Processed rows should be archived or deleted only according to an operational
retention policy. Stuck pending records require detection and recovery.

### Monitoring

At minimum, observe:

- unpublished outbox count and age of the oldest pending event
- publication throughput, latency, retries, and failures
- broker availability and consumer lag or queue depth
- consumer processing success, retries, deduplication count, and dead letters
- end-to-end latency from `occurredAt` to consumer completion

Logs and traces should carry `eventId`, correlation ID, event type, producer,
and aggregate ID without logging sensitive payloads.

## 11. Future Broker Decision

The repository roadmap mentions Kafka, but no final broker decision or
operational commitment exists. Both options require a separate ADR and proof of
operation before implementation.

| Consideration | Kafka | RabbitMQ |
| --- | --- | --- |
| Primary model | Durable ordered log with consumer offsets | Brokered queues and exchanges with acknowledgements |
| Replay | Strong fit for retained event replay and new consumer groups | Possible through topology and retention choices, but not the primary queue model |
| Ordering | Partition ordering; key design is important | Queue ordering with caveats for concurrency, retries, and redelivery |
| Routing | Topics and consumer groups | Flexible exchanges, routing keys, and queues |
| Typical fit | Event streams, multiple independent consumers, analytics, long retention | Work queues, command-style delivery, flexible routing, simpler bounded workflows |
| Operational questions | Partition count, retention, rebalancing, schema governance | Exchange/queue topology, acknowledgements, dead lettering, queue growth |
| Spring integration | Spring for Apache Kafka | Spring AMQP |

The decision should use expected throughput, replay needs, ordering scope,
consumer count, operational expertise, hosting constraints, and observability
requirements. Exact library versions must be selected from the Spring Boot
3.4.x dependency baseline and verified with Spring Cloud 2024.x when the
implementation branch begins.

## 12. MVP Migration Strategy

### Phase 1: Keep synchronous REST and add outbox persistence

Keep the existing task-to-notification REST flow. Add task-service outbox
persistence in the same local transaction as task persistence. Add no broker
dependency and no publisher. Use current tests as the behavior baseline and
verify that task rollback also rolls back the outbox insert.

### Phase 2: Add publisher

Introduce a task-service publisher that reads eligible outbox rows and marks
lifecycle progress. Keep the synchronous REST notification flow active. If a
broker is selected for this phase, choose it through a separate implementation
decision and version-compatibility check.

### Phase 3: Move notification delivery to event-driven flow

Implement notification-service event consumption with durable idempotency,
retry handling, and controlled duplicate prevention. Move task notification
creation to the event-driven flow after end-to-end acceptance criteria pass.
Avoid letting both synchronous REST and event consumption create duplicate
notifications in production; use feature flags or controlled environments for
rollout.

### Phase 4: Remove synchronous REST integration

After event publication and consumption meet defined acceptance criteria,
disable and then remove task-service's direct notification REST call and
delegated JWT propagation. Keep rollback controls during rollout and update
service contracts, runbooks, diagrams, and tests.

User lifecycle events can follow the same phases independently. Services do
not need to migrate all event types in one release.

## 13. Database Design Sketch

Each producing service may eventually own a table conceptually shaped as:

```text
outbox_event
- id
- aggregate_type
- aggregate_id
- event_type
- payload
- status
- created_at
- processed_at
```

Likely implementation concerns include:

- UUID `id` corresponding to `eventId`
- JSON payload storage with explicit schema version
- indexes supporting unpublished status and creation order
- retry count, next-attempt time, and last-error metadata
- optimistic or pessimistic claim/lock metadata for concurrent publishers
- retention and cleanup of published records

A consumer-side processed-event or inbox record is also likely necessary for
durable idempotency. Its exact schema is deferred. All schema changes must use
new Flyway migrations in the owning service; no shared outbox database or
cross-service foreign key is allowed.

## 14. Security Considerations

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

## 15. Open Questions

- Kafka or RabbitMQ, and what measured requirements decide the choice?
- Polling publisher or change data capture for the first implementation?
- Topic, exchange, queue, partition, and routing-key conventions?
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

## Decision Summary

The proposed direction is a service-owned transactional outbox with
at-least-once broker delivery and idempotent consumers. It preserves current
aggregate ownership and HTTP APIs while allowing synchronous integrations to
be replaced incrementally after reliability and operational behavior are
verified. The broker and implementation mechanism remain open decisions.
