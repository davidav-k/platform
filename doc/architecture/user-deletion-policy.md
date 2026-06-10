# User Deletion and Deactivation Policy

## Status

This document records the approved MVP architecture decision for user
lifecycle termination across `user-service`, `task-service`, and
`notification-service`.

The decision is a target policy. It does not claim that all described behavior
is implemented. Current implementation gaps are listed separately below.

## Problem Statement

A user is referenced outside `user-service` by a stable public `userId`.
Tasks retain creator and assignee identifiers, while notifications retain
recipient identifiers. Physically removing a user from `user-service` cannot
safely cascade through service-owned databases without coupling services,
losing history, or granting one service control over another service's data.

For MVP, "user deletion" therefore needs one platform meaning that preserves
audit history, prevents further account use, and respects database ownership.

## Service Ownership Boundaries

### `user-service`

Owns identity, credentials, profile data, roles, authentication, and account
lifecycle state. It is the source of truth for whether a user may authenticate
or is active.

### `task-service`

Owns task records. It stores only stable user identifiers in
`createdByUserId`, `assigneeUserId`, and task deletion audit fields. It must not
delete or rewrite tasks by accessing the user database.

### `notification-service`

Owns notifications and notification preferences. It stores stable identifiers
such as `recipientUserId` and preference `userId`. It must not delete its data
by accessing the user database.

There are no cross-service foreign keys. Each service applies retention and
authorization rules to the records it owns.

## Current Implemented Behavior

The following behavior exists at the time of this decision:

- Registration creates a user with `enabled=false`; account verification sets
  `enabled=true`.
- Login rejects disabled users through Spring Security account-state checks.
- Administrative lock and unlock operations change `accountNonLocked`. They
  are temporary access controls, not a documented deletion or deactivation
  lifecycle.
- `DELETE /api/users/{userId}` is an admin-authorized operation that currently
  hard-deletes the user from the `user-service` database. It rejects deletion
  of system user `0` and self-deletion.
- The hard delete removes user-service-owned login history and role
  assignments. Database cascades remove credentials and confirmations.
- `task-service` retains `createdByUserId` and optional `assigneeUserId` UUIDs.
  It does not validate that a referenced user currently exists or is active.
- `notification-service` retains `recipientUserId` and preference `userId`
  UUIDs. It does not consume user lifecycle changes.
- No cross-service deletion propagation or cleanup is implemented.

The current hard-delete endpoint does not conform to the approved MVP policy
below. Aligning its implementation and deciding any contract transition is
future work, not part of this documentation change.

## MVP Decision

For MVP, user deletion means **account deactivation with retained identity**.

1. `user-service` remains the lifecycle authority and should make the account
   unable to authenticate or use protected profile operations.
2. The user row and stable public `userId` are retained. MVP operations must
   not physically delete users.
3. `task-service` retains tasks created by the user. `createdByUserId` remains
   unchanged for history and audit purposes.
4. Tasks assigned to the user remain assigned until an authorized admin or
   task creator explicitly reassigns or unassigns them.
5. `notification-service` retains existing notifications and preferences for
   historical consistency. Retention does not imply continued delivery.
6. No service makes synchronous cascading deletion calls to another service.
7. Existing task and notification authorization rules continue to apply.
   Deactivation does not grant regular users additional visibility.
8. Other services store stable identifiers, not copied user profile details,
   unless a separately documented business requirement requires a snapshot.

Hard deletion and anonymization are not supported MVP lifecycle operations.
They require separate privacy, retention, audit, and API decisions.

## Behavior Matrix

| Scenario | `user-service` | `task-service` | `notification-service` |
| --- | --- | --- | --- |
| User is disabled during registration | Login is denied until verification enables the account | No lifecycle synchronization is implemented | No lifecycle synchronization is implemented |
| User is administratively locked | Login is denied while `accountNonLocked=false`; unlock may restore access | Existing tasks and assignments are unchanged | Existing notifications are unchanged |
| User is deactivated under the MVP policy | Account must not authenticate; retained identity remains the lifecycle source of truth | Created tasks remain; creator ID is unchanged; assignments remain until explicit reassignment | Existing notifications and preferences remain stored; new intentional delivery should be suppressed once lifecycle validation exists |
| Deactivated user is a task creator | Profile details must not be copied into task responses | Task remains subject to existing task RBAC and retention rules | No automatic notification deletion |
| Deactivated user is an assignee | Account cannot act on the assignment | Assignment remains until admin/creator reassigns or unassigns it | Existing assignment notifications remain historical records |
| Admin views retained records | User administration follows user-service authorization | Admin may see tasks referencing the stable user ID | Admin visibility follows the notification authorization contract; no extra access is implied by deactivation |
| Regular user views retained records | No new rights are granted | Existing ownership/assignment RBAC remains unchanged | Existing notification ownership rules remain unchanged |
| User is hard deleted | Currently implemented by user-service, but not supported by the approved MVP policy | No cascade; UUID references would remain | No cascade; UUID references would remain |
| User is anonymized | Not implemented; future privacy feature | Historical identifiers require an explicit pseudonymization decision | Content and recipient identifiers require an explicit privacy review |

## API Implications

This decision does not change a public API contract.

- No new deactivate, restore, hard-delete, anonymize, or lifecycle-query
  endpoint is defined here.
- The existing user hard-delete endpoint remains implemented but is a policy
  conformance gap. A later implementation change must decide whether to change
  its semantics, replace it, deprecate it, or version the contract.
- Lock/unlock must not be documented as permanent deactivation without an
  explicit contract decision, because unlock currently restores access.
- Task and notification DTOs continue exposing stable identifiers according to
  their existing contracts. They must not begin embedding deleted-user profile
  data as a workaround for missing cross-service joins.
- Existing response-envelope and error conventions remain unchanged. Any
  future lifecycle endpoint must use the standard response envelope and define
  authentication, authorization, idempotency, and missing-user behavior.

Profile access for a deactivated user is expected to be denied because the
account cannot authenticate. The exact response and token invalidation behavior
must be specified when deactivation is implemented.

## Data Retention and Audit Implications

- Stable public `userId` values are retained so task creator, assignee, and
  notification recipient history remains interpretable.
- `createdByUserId` is immutable and must not be replaced with an admin,
  system user, or null value after deactivation.
- `assigneeUserId` is not cleared automatically during MVP. Automatic cleanup
  could hide operational work and would require an explicit reassignment rule.
- Existing notification records remain in the notification database. Delivery
  attempts after deactivation are a separate concern from record retention.
- Retained identifiers must not be used to expose user profile fields across
  service boundaries. User-facing presentation may use a neutral label such as
  "Deactivated user" once a lifecycle projection exists.
- This policy is an architecture retention rule, not a legal retention
  schedule. Regulatory erasure and retention periods require product, legal,
  and security decisions.

## Privacy and Security

Deactivation must prevent new authenticated activity. A future implementation
must address access-token and refresh-token behavior so previously issued
tokens cannot continue to authorize a deactivated account.

Other services should retain only stable identifiers needed for ownership,
authorization, history, or delivery tracking. Notification subjects and bodies
may contain user-entered or profile-derived content and therefore require a
separate privacy review before anonymization or legal erasure is implemented.

Deactivation does not weaken authorization. In particular, another user's
deactivation must not make their tasks or notifications visible to a regular
user who could not previously access them.

## Future Event-Driven Approach

The planned direction is asynchronous lifecycle propagation without shared
database access or synchronous deletion cascades.

Illustrative events include `UserDeactivated`, `UserReactivated`, and, only if
hard deletion is later approved, `UserDeleted`. These names are examples, not
implemented event contracts.

A future design should define:

- versioned event schemas containing the stable public `userId`, lifecycle
  timestamp, event ID, and minimal audit-safe metadata
- transactional publication, preferably through an outbox
- idempotent consumers and replay behavior
- task-service projections used to prevent new assignment to inactive users
  without rewriting historical tasks
- notification-service projections used to suppress new delivery to inactive
  users while retaining existing records
- failure handling, ordering, reconciliation, and reactivation semantics

A scheduled report or explicit admin workflow may later identify assignments
held by inactive users. MVP does not implement automatic reassignment or
unassignment.

## Open Questions

- Which explicit user state model should distinguish unverified, active,
  temporarily locked, deactivated, and anonymized accounts?
- Should the existing delete route change semantics, be deprecated, or be
  replaced by a versioned lifecycle endpoint?
- Who may deactivate and reactivate users, and is self-deactivation allowed?
- How are active access and refresh tokens revoked or rejected immediately?
- What retention periods apply to user profile data, login history, tasks,
  notification content, and preferences?
- Which profile fields must be erased or pseudonymized for privacy requests?
- Should new task assignment perform synchronous lifecycle validation before
  event-driven projections are available?
- How should administrators discover and resolve assignments to inactive users?
- What notification channels must be suppressed immediately on deactivation?

## Non-Goals

This decision does not implement lifecycle endpoints, database migrations,
event infrastructure, cascades, anonymization, token revocation, assignment
cleanup, or notification cleanup.
