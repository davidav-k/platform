# Service Boundaries

## Current Phase

The MVP currently runs `user-service`, `task-service`, `notification-service`,
API Gateway, Vue frontend, Config Server, Eureka, PostgreSQL, Redis, Kafka,
MailHog, and Zipkin in Docker Compose.

The implemented cross-service notification path for task creation is
event-driven:

```text
task-service -> outbox_events -> Kafka platform.task-events
  -> notification-service -> notifications

user-service -> outbox_events -> Kafka platform.user-events
  -> audit-service -> audit_records

notification-service -> outbox_events -> Kafka platform.notification-events
  -> future audit-service notification consumer
```

Frontend traffic remains synchronous HTTP through API Gateway. Kafka/outbox is
used for task delivery and for durable user audit-event production; it is not a
general event-driven replacement for all service interactions.

## Service Responsibilities

### user-service

`user-service` is the source of truth for:

- users and stable public `userId` values
- roles and authorities
- authentication, JWT issuance, refresh, and MFA
- profiles
- account lifecycle, including registration, verification, locking, and deletion
- user lifecycle and authentication outbox events


### task-service

`task-service` is the source of truth for:

- tasks
- task assignments
- task statuses and status transitions
- task soft deletion
- task-domain outbox events

It stores user references by `user-service` public `userId`. It does not own
user profiles, credentials, roles, or authorities. It does not call
notification-service directly for task-created notifications.

### notification-service

`notification-service` is the source of truth for:

- notification records
- notification preferences persistence
- task-event notification processing
- event-consumption idempotency through `event_consumption_log`
- notification audit-event publishing through `outbox_events`

It does not own task state or user profiles. It stores only the recipient user
reference, notification content/status, source metadata, and consumed event IDs
needed for notification processing.

## Aggregate Ownership

| Aggregate | Source of truth | Notes |
| --- | --- | --- |
| `User` | `user-service` | Includes account lifecycle and profile |
| `Role` | `user-service` | Authorities are defined with the role owner |
| `Authentication` | `user-service` | Includes JWT issuance, refresh, and MFA |
| `UserOutboxEvent` | `user-service` | Durable user lifecycle and authentication event rows in `outbox_events` |
| `Task` | `task-service` | Root for title, description, status, priority, assignee, and deletion state |
| `TaskAssignment` | `task-service` | References assignee by public `userId` |
| `TaskOutboxEvent` | `task-service` | Durable task-domain event rows in `outbox_events` |
| `Notification` | `notification-service` | System-notification aggregate |
| `NotificationPreference` | `notification-service` | Persisted preference entity; no public API yet |
| `ConsumedEvent` | `notification-service` | Idempotency record for accepted Kafka events |
| `NotificationOutboxEvent` | `notification-service` | Durable notification creation event in `outbox_events` |

Task comments, task history, notification templates, and delivery attempts are
not implemented in the current codebase.

## Data Ownership

- Each service owns its database schema and migrations.
- Services exchange public identifiers, not database primary keys.
- No service reads or writes another service's tables.
- Cross-service data copies are limited to the fields needed for local
  behavior and are not treated as the source of truth.
- A task stores creator and assignee `userId` references but does not duplicate
  user profile ownership.
- A notification stores recipient `userId`, channel, content, status, source
  metadata, and delivery timestamps but does not duplicate task ownership.

User lifecycle termination does not cascade across databases. The approved MVP
policy retains stable user identifiers and historical task and notification
records; see
[User deletion and deactivation policy](user-deletion-policy.md).

## Allowed Dependencies

| Caller | Callee | Allowed purpose |
| --- | --- | --- |
| API Gateway | externally routed services | Route requests and reject invalid JWTs early |
| Frontend | API Gateway | Use public `/api/**` routes only |
| `task-service` | Kafka | Publish task domain events from `outbox_events` |
| `user-service` | Kafka | Publish user lifecycle and authentication events from `outbox_events` |
| Kafka | `notification-service` | Deliver task events to the notification consumer |

Downstream services independently validate JWTs and authorize access to their
own aggregates.

## Forbidden Dependencies

- `task-service` must not access the `user-service` database.
- `notification-service` must not access the `task-service` database.
- Services must not use shared entity classes or shared persistence models.
- Services must not trust gateway headers as the sole authentication proof.
- Task-service must publish task-created notification intent through
  `outbox_events` and Kafka, not through an HTTP dependency on
  notification-service.

## Authorization Model

The implemented user model contains `ROLE_USER`, `ROLE_MANAGER`, `ROLE_ADMIN`,
and `ROLE_SUPER_ADMIN`, plus granular authorities. Task-service currently
implements owner/assignee/admin style rules for task access, update, assignment,
status changes, and soft delete.

The exact manager scope is unresolved and must be decided before implementing
manager-only behavior. Contracts therefore state owner, assignee, or
administrative access explicitly.

## Inter-Service Communication

### Implemented HTTP

- Frontend traffic goes through API Gateway.
- Gateway routes `/api/users/**`, `/api/tasks/**`, and
  `/api/notifications/**`.
- Gateway validates JWTs early; downstream services validate JWTs again.

### Implemented Kafka

- `user-service` publishes audit-relevant registration, login, profile,
  deletion, password-change, and MFA-enable events to `platform.user-events`.
- `audit-service` consumes supported user events, sanitizes their payloads,
  and stores them idempotently in `audit_records`.
- `task-service` writes task create, assignment, status, update, and deletion
  events to its outbox.
- `OutboxEventPollingScheduler` publishes `NEW` and `FAILED` outbox events
  through `KafkaOutboxEventPublisher`.
- `notification-service` consumes `platform.task-events` when
  `notification.kafka.enabled=true`.
- `NotificationEventConsumer` uses `event_consumption_log.event_id` as the
  idempotency key.
- `notification-service` publishes `NOTIFICATION_CREATED` and
  `NOTIFICATION_SYSTEM_CREATED` to `platform.notification-events`.
- `TaskEventNotificationProcessor` handles `TASK_CREATED`, `TASK_ASSIGNED`,
  and `TASK_STATUS_CHANGED` events.

## API Gateway Routing Contract

| External route | Internal route | Service | State |
| --- | --- | --- | --- |
| `/api/users/**` | `/api/v1/user/**` | `user-service` | Implemented |
| `/api/tasks` | `/api/v1/tasks` | `task-service` | Implemented |
| `/api/tasks/**` | `/api/v1/tasks/**` | `task-service` | Implemented |
| `/api/notifications` | `/api/v1/notifications` | `notification-service` | Implemented |
| `/api/notifications/**` | `/api/v1/notifications/**` | `notification-service` | Implemented |

No Gateway route exists for `/internal/api/v1/notifications/system`.
