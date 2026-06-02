# API Contract Standards

## Scope

These standards apply to new platform APIs. Existing `user-service` endpoints
remain unchanged. New contracts should follow the existing `user-service`
response shape while avoiding known inconsistencies documented below.

## URL Versioning

- Internal service APIs use `/api/v1/<resources>`.
- New resource collections use plural names, for example `/api/v1/tasks`.
- External browser and frontend traffic enters through API Gateway routes
  without the version segment, for example `/api/tasks/**`.
- Internal-only APIs use `/internal/api/v1/<resources>` and must not be routed
  publicly by API Gateway.

Existing `user-service` is an intentional compatibility exception: its internal
base path is `/api/v1/user/**`.

## Response Envelope

New services use the envelope already established by `user-service`:

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `time` | string | yes | ISO-8601 response timestamp |
| `code` | integer | yes | HTTP status code |
| `path` | string | yes | Request path handled by the service |
| `status` | string | yes | HTTP status name, for example `OK` |
| `message` | string | yes | Human-readable summary |
| `exception` | string | no | Safe exception summary; omit for successful responses |
| `data` | object | yes | Response payload or an empty object |

Success example:

```json
{
  "time": "2026-06-02T12:00:00Z",
  "code": 200,
  "path": "/api/v1/tasks/0b3df21d-f449-4ef1-98f8-2532fe49dd16",
  "status": "OK",
  "message": "Task retrieved successfully.",
  "data": {
    "task": {
      "taskId": "0b3df21d-f449-4ef1-98f8-2532fe49dd16",
      "title": "Prepare release notes"
    }
  }
}
```

## Error Model

Errors use the same envelope. The actual HTTP response status must match
`code`. Error messages must not expose secrets, JWTs, passwords, internal SQL,
or stack traces.

Error example:

```json
{
  "time": "2026-06-02T12:00:00Z",
  "code": 404,
  "path": "/api/v1/tasks/0b3df21d-f449-4ef1-98f8-2532fe49dd16",
  "status": "NOT_FOUND",
  "message": "Task not found.",
  "data": {}
}
```

Validation errors place field-level messages in `data`:

```json
{
  "time": "2026-06-02T12:00:00Z",
  "code": 400,
  "path": "/api/v1/tasks",
  "status": "BAD_REQUEST",
  "message": "Provided arguments are not valid",
  "data": {
    "title": "Title must not be blank"
  }
}
```

Standard status usage:

| Status | Usage |
| --- | --- |
| `400 BAD_REQUEST` | Invalid request or unsupported state transition |
| `401 UNAUTHORIZED` | Missing or invalid authentication |
| `403 FORBIDDEN` | Authenticated principal lacks permission |
| `404 NOT_FOUND` | Aggregate does not exist or is not visible to the caller |
| `409 CONFLICT` | State conflict or duplicate operation |
| `500 INTERNAL_SERVER_ERROR` | Unexpected server error |
| `503 SERVICE_UNAVAILABLE` | Required dependency is unavailable |

## Pagination

List endpoints accept:

| Query parameter | Type | Default | Rules |
| --- | --- | --- | --- |
| `page` | integer | `0` | Must be at least `0` |
| `size` | integer | `20` | Must be between `1` and `100` |
| `sort` | string | endpoint-specific | Format: `<field>,<asc|desc>` |

Paginated payloads use:

```json
{
  "items": [],
  "page": {
    "number": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0
  }
}
```

## Identifiers

- New aggregate identifiers are UUID strings.
- `taskId`, `commentId`, `assignmentId`, and `notificationId` are UUIDs.
- Cross-service references use stable public identifiers only.
- Task and notification contracts refer to a user by the public `userId`
  supplied by `user-service`, not by its database primary key.
- Internal numeric database IDs must not cross a service boundary.

## Date And Time

- API timestamps use ISO-8601 strings with a UTC offset, preferably `Z`.
- Date-only values use ISO-8601 `YYYY-MM-DD`.
- Services store instants in UTC and convert only at presentation boundaries.

## Authentication And Authorization

- API Gateway validates JWTs as an early rejection layer.
- Every downstream service independently validates JWTs.
- Downstream services must not trust `X-Authenticated-User` as the sole proof
  of identity.
- Authorization decisions belong to the service that owns the resource.
- Internal-only endpoints require service authentication when implemented.
  They must not rely on an externally supplied user header.

## Current Compatibility Notes

- `user-service` exception handlers use the established envelope, but several
  handlers return the body directly rather than a `ResponseEntity`. Future
  implementations must ensure the transport status matches envelope `code`.
- API Gateway currently emits a smaller JSON error body for rejected JWTs and
  plain text for some fallback responses. Envelope alignment is future gateway
  work and is not part of these contracts.
