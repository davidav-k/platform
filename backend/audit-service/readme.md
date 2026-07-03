# Audit Service

## Purpose

Audit Service consumes task, user, and notification lifecycle events from
Kafka, normalizes them, and stores immutable audit records. It exposes a
read-only API for browsing those records. There are no create, update, or
delete REST endpoints.

Task events are consumed from `platform.task-events`. User events are consumed
from `platform.user-events`, and notification events from
`platform.notification-events`. All listeners use the `audit-service`
consumer group and the event ID remains the persistence idempotency key.

Supported User Service mappings:

- `USER_REGISTERED` -> `REGISTER_USER`
- `USER_LOGIN_SUCCESS` -> `LOGIN_SUCCESS`
- `USER_LOGIN_FAILED` -> `LOGIN_FAILED`
- `USER_PROFILE_UPDATED` -> `UPDATE_USER_PROFILE`
- `USER_DELETED` -> `DELETE_USER`
- `PASSWORD_CHANGED` -> `CHANGE_PASSWORD`
- `MFA_ENABLED` -> `ENABLE_MFA`

Unknown user event types are skipped. User payloads are sanitized recursively
before persistence to remove password, JWT, token, secret, and confirmation-key
fields.

Supported Notification Service mappings:

- `NOTIFICATION_CREATED` -> `CREATE_NOTIFICATION`
- `NOTIFICATION_SYSTEM_CREATED` -> `CREATE_SYSTEM_NOTIFICATION`

Unknown notification event types are skipped. Notification payloads are
sanitized recursively to remove password, JWT, token, cookie, request-header,
authorization, and secret fields. Recipient IDs are not treated as actors.

The service registers with Eureka as audit-service and listens on port 8088 in
Docker Compose.

## REST API

The API is currently available directly from Audit Service:

    http://localhost:8088/api/v1/audit

## Security

Audit API access requires a valid platform access token with one of these
authorities:

- ROLE_ADMIN
- ROLE_SUPER_ADMIN

Tokens are accepted through the existing Authorization Bearer header or the
existing HttpOnly access-token cookie. Unauthenticated requests return 401;
authenticated users without an allowed role receive 403. The health and info
actuator endpoints remain public.

### GET /api/v1/audit

Returns audit records with pagination, sorting, and optional exact-match
filters.

Pagination parameters:

- page: zero-based page number; default 0
- size: page size from 1 through 100; default 20
- sort: one allow-listed field and direction in field,direction format;
  default occurredAt,desc

Allowed sort fields:

- auditId
- eventId
- eventType
- aggregateType
- aggregateId
- sourceService
- actorUserId
- actorEmail
- action
- occurredAt
- createdAt

Optional filters:

- eventType
- aggregateType
- aggregateId
- sourceService
- actorUserId
- action
- from: inclusive ISO-8601 lower bound on occurredAt
- to: inclusive ISO-8601 upper bound on occurredAt

Example:

    curl -fsS \
      -H "Authorization: Bearer $ADMIN_ACCESS_TOKEN" \
      "http://localhost:8088/api/v1/audit?eventType=TASK_UPDATED&aggregateType=TASK&page=0&size=20&sort=occurredAt,desc"

Successful responses use the platform response envelope:

    {
      "code": 200,
      "status": "OK",
      "message": "Audit records retrieved successfully.",
      "data": {
        "items": [],
        "page": {
          "number": 0,
          "size": 20,
          "totalElements": 0,
          "totalPages": 0
        }
      }
    }

Audit DTOs expose public audit fields only. Database IDs, entity versions, and
stored event payloads are not returned.

### GET /api/v1/audit/{auditId}

Returns one record by public auditId.

    curl -fsS \
      -H "Authorization: Bearer $ADMIN_ACCESS_TOKEN" \
      "http://localhost:8088/api/v1/audit/$AUDIT_ID"

An unknown auditId returns 404 NOT_FOUND using the same response envelope.

## Build and Test

Build Audit Service:

    mvn -B -f backend/audit-service/pom.xml clean verify

Run the full local producer-to-Audit verification after starting Docker
Compose:

```bash
./scripts/verify-audit-flow.sh
```

The script logs in as the configured admin, triggers successful and failed
login events, creates/updates/assigns/status-changes/deletes a task, creates a
notification, and polls the secured Audit API until task, user, and
notification audit records are visible. It uses only standard shell tools and
`curl`; `ADMIN_PASSWORD` is read from the repository `.env` file.

Run the focused REST API and security tests:

    mvn -B -f backend/audit-service/pom.xml \
      -Dtest=AuditControllerTest,AuditQueryServiceImplTest,AuditSecurityIntegrationTest test

The persistence tests use PostgreSQL 16.1 through Testcontainers and require
Docker.
