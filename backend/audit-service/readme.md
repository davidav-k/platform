# Audit Service

## Purpose

Audit Service consumes task lifecycle events from Kafka, normalizes them, and
stores immutable audit records. It exposes a read-only API for browsing those
records. There are no create, update, or delete REST endpoints.

The service registers with Eureka as audit-service and listens on port 8088 in
Docker Compose.

## REST API

The API is currently available directly from Audit Service:

    http://localhost:8088/api/v1/audit

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

    curl -fsS "http://localhost:8088/api/v1/audit/$AUDIT_ID"

An unknown auditId returns 404 NOT_FOUND using the same response envelope.

## Build and Test

Build Audit Service:

    mvn -B -f backend/audit-service/pom.xml clean verify

Run the focused REST API tests:

    mvn -B -f backend/audit-service/pom.xml \
      -Dtest=AuditControllerTest,AuditQueryServiceImplTest test

The persistence tests use PostgreSQL 16.1 through Testcontainers and require
Docker.

## Local Verification

Start the platform:

    docker compose --env-file .env -f compose.yml up -d --build

Check Audit Service health and Eureka registration:

    curl -fsS http://localhost:8088/actuator/health
    curl -fsS -H 'Accept: application/json' \
      http://localhost:8761/eureka/apps/AUDIT-SERVICE

Generate lifecycle events through the existing task API:

    curl -i -X POST "http://localhost:8080/api/tasks" \
      -H "Authorization: Bearer $ACCESS_TOKEN" \
      -H "Content-Type: application/json" \
      -d '{"title":"Audit API verification","priority":"HIGH"}'

    curl -i -X PATCH "http://localhost:8080/api/tasks/$TASK_ID" \
      -H "Authorization: Bearer $ACCESS_TOKEN" \
      -H "Content-Type: application/json" \
      -d '{"description":"Updated for audit verification"}'

    curl -i -X PATCH "http://localhost:8080/api/tasks/$TASK_ID/status" \
      -H "Authorization: Bearer $ACCESS_TOKEN" \
      -H "Content-Type: application/json" \
      -d '{"status":"IN_PROGRESS"}'

    curl -i -X DELETE "http://localhost:8080/api/tasks/$TASK_ID" \
      -H "Authorization: Bearer $ACCESS_TOKEN"

Allow the outbox publisher and Kafka consumer to process the events, then list
the records:

    curl -fsS \
      "http://localhost:8088/api/v1/audit?page=0&size=20&sort=occurredAt,desc"

Capture an auditId from data.items, then verify a single record and 404:

    curl -fsS "http://localhost:8088/api/v1/audit/$AUDIT_ID"
    curl -i "http://localhost:8088/api/v1/audit/00000000-0000-0000-0000-000000000000"

Filter examples:

    curl -fsS "http://localhost:8088/api/v1/audit?eventType=TASK_UPDATED"
    curl -fsS "http://localhost:8088/api/v1/audit?aggregateType=TASK"
    curl -fsS "http://localhost:8088/api/v1/audit?aggregateId=$TASK_ID"
    curl -fsS "http://localhost:8088/api/v1/audit?sourceService=task-service"
    curl -fsS "http://localhost:8088/api/v1/audit?actorUserId=$USER_ID"
    curl -fsS "http://localhost:8088/api/v1/audit?action=UPDATE_TASK"
    curl -fsS \
      "http://localhost:8088/api/v1/audit?from=2026-07-01T00:00:00Z&to=2026-07-31T23:59:59Z"
