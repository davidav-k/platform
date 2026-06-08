# Service Boundaries

## Current Phase

The MVP currently runs `user-service` and supporting infrastructure. The
`task-service` and `notification-service` directories are documentation shells.
Their APIs in this repository are contracts for future implementation.

The current communication model for new MVP services is synchronous REST.
Event-driven integration is a future direction only. No broker is introduced
by these contracts.

## Service Responsibilities

### user-service

`user-service` is the source of truth for:

- users and stable public `userId` values
- roles and authorities
- authentication, JWT issuance, refresh, and MFA
- profiles
- account lifecycle, including registration, verification, locking, and deletion

It may send its existing account email messages during the MVP. General
platform notification ownership belongs to `notification-service`.

### task-service

`task-service` is the source of truth for:

- tasks
- task assignments
- task statuses and status transitions
- task comments
- task history

It stores user references by `user-service` public `userId`. It does not own
user profiles, credentials, roles, or authorities.

### notification-service

`notification-service` is the source of truth for:

- system notifications
- notification preferences
- email delivery requests for platform activity
- notification delivery tracking
- future notification templates

It does not own task state or user profiles. It stores only the user reference
and delivery data needed for notification processing.

## Aggregate Ownership

| Aggregate | Source of truth | Notes |
| --- | --- | --- |
| `User` | `user-service` | Includes account lifecycle and profile |
| `Role` | `user-service` | Authorities are defined with the role owner |
| `Authentication` | `user-service` | Includes JWT issuance, refresh, and MFA |
| `Task` | `task-service` | Root for title, description, status, priority, and due date |
| `TaskAssignment` | `task-service` | References assignee by public `userId` |
| `TaskComment` | `task-service` | References author by public `userId` |
| `TaskHistory` | `task-service` | Records task-domain changes |
| `Notification` | `notification-service` | System-notification aggregate |
| `NotificationPreference` | `notification-service` | Per-user delivery preferences |
| `NotificationTemplate` | `notification-service` | Future rendering ownership |
| `DeliveryStatus` | `notification-service` | Tracks notification delivery attempts |

## Data Ownership

- Each service owns its database schema and migrations.
- Services exchange public identifiers, not database primary keys.
- No service reads or writes another service's tables.
- Cross-service data copies are limited to the fields needed for local
  behavior and are not treated as the source of truth.
- A task stores `creatorUserId` and assignee `userId` references but does not
  duplicate user profile ownership.
- A notification stores recipient `userId`, channel, content, and delivery
  state but does not duplicate task ownership.

## Allowed Dependencies

During the synchronous REST phase:

| Caller | Callee | Allowed purpose |
| --- | --- | --- |
| API Gateway | all externally routed services | Route requests and reject invalid JWTs early |
| `task-service` | `user-service` | Validate referenced users when a task assignment is created |
| `notification-service` | `user-service` | Resolve current recipient delivery details when required |
| `task-service` | `notification-service` | Request task-related notifications through internal APIs |

All downstream services independently validate JWTs and authorize access to
their own aggregates.

## Forbidden Dependencies

- `task-service` must not access the `user-service` database.
- `notification-service` must not access the `task-service` database.
- `user-service` must not become the owner of tasks or notification preferences.
- Services must not use shared entity classes or shared persistence models.
- Services must not trust gateway headers as the sole authentication proof.
- Internal notification APIs must not be exposed through API Gateway.

## Authorization Model

The implemented user model contains `ROLE_USER`, `ROLE_MANAGER`, `ROLE_ADMIN`,
and `ROLE_SUPER_ADMIN`, plus granular authorities. Future services should
authorize with explicit domain authorities while preserving these role
expectations:

| Role | Expected capability |
| --- | --- |
| `ROLE_USER` | Create own tasks, view tasks created by or assigned to the user, update permitted own tasks, and comment on visible tasks |
| `ROLE_MANAGER` | Manage tasks and assignments within the manager's permitted scope |
| `ROLE_ADMIN` | Manage all tasks and use administration operations |
| `ROLE_SUPER_ADMIN` | Same task-domain access as admin; retains highest user administration capability |

The exact manager scope is unresolved and must be decided before implementing
manager-only behavior. Contracts therefore state owner, assignee, or
administrative access explicitly.

## Inter-Service Communication

### MVP: synchronous REST

- Frontend traffic goes through API Gateway.
- `task-service` may call `user-service` to validate assignment targets.
- `task-service` may call internal notification APIs for immediate platform
  notification requests.
- Timeouts, retries, and idempotency must be specified during implementation.

### Future: event-driven

Future versions may replace selected synchronous calls with broker-delivered
events. Event names reserved for design work include:

| Event | Producer | Typical consumers |
| --- | --- | --- |
| `TaskCreatedEvent` | `task-service` | `notification-service`, future audit service |
| `TaskAssignedEvent` | `task-service` | `notification-service`, future audit service |
| `TaskStatusChangedEvent` | `task-service` | `notification-service`, future audit service |
| `TaskCommentAddedEvent` | `task-service` | `notification-service`, future audit service |
| `NotificationRequestedEvent` | platform services | `notification-service` |

These names do not define payload schemas or introduce Kafka. Event payload
contracts, delivery guarantees, and versioning require a separate design.

## API Gateway Routing Contract

| External route | Future internal route | Service | State |
| --- | --- | --- | --- |
| `/api/users/**` | `/api/v1/user/**` | `user-service` | Implemented |
| `/api/tasks/**` | `/api/v1/tasks/**` | `task-service` | Implemented (create, get, list) |
| `/api/notifications/**` | `/api/v1/notifications/**` | `notification-service` | Contract only |
| `/api/notification-preferences/**` | `/api/v1/notification-preferences/**` | `notification-service` | Contract only |

The current gateway configuration serves both `/api/users/**` and
`/api/tasks/**`. Notification routes do not exist yet.
