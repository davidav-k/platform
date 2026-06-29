# Task Service API Contract

## Scope

This document describes `task-service` API endpoints. Implemented endpoints
reflect the running MVP. Planned endpoints are contracts for future work. The
service owns tasks, assignments, statuses, comments, and task history as defined
in [Service boundaries](../architecture/service-boundaries.md).

All endpoints:

- use the response envelope from [API contract standards](api-contract-standards.md)
- require a valid access JWT
- use UUID strings for task-domain identifiers
- use the stable `user-service` public `userId` for user references

## Implemented Endpoints

### `POST /api/v1/tasks`

Creates a task owned by the authenticated user. `createdByUserId` is set from
the JWT subject and is ignored if supplied in the request payload.

- Gateway route: `POST /api/tasks`
- Request DTO: `CreateTaskRequest`
- Response: `201 CREATED`, `data.task` contains `CreateTaskResponse`
- Authentication: required
- Event side effect: task creation writes a `TASK_CREATED` row to
  `outbox_events` in the same transaction as the task. Notification delivery is
  asynchronous through Kafka; task-service does not call notification-service
  directly.

### `GET /api/v1/tasks/{taskId}`

Returns one task by its public UUID.

- Gateway route: `GET /api/tasks/{taskId}`
- Request DTO: none
- Response: `200 OK`, `data.task` contains `TaskResponse`
- Authentication: required
- Authorization: `ROLE_ADMIN` and `ROLE_SUPER_ADMIN` can read any task; other users can read tasks they created or are assigned to
- Missing or inaccessible task: `404 NOT_FOUND`

### `GET /api/v1/tasks`

Returns a filtered, paginated task list.

- Gateway route: `GET /api/tasks`
- Request DTO: none
- Response: `200 OK`, `data.items` contains `TaskResponse` entries and `data.page` contains pagination metadata
- Authentication: required
- Authorization: `ROLE_ADMIN` and `ROLE_SUPER_ADMIN` can list all tasks; other users see only tasks they created or are assigned to
- Optional filters: `status`, `priority`, `assigneeUserId`, `createdByUserId`
- Pagination: `page` (default `0`), `size` (default `20`, max `100`)
- Sorting: `sort` (default `createdAt,desc`)

### `PATCH /api/v1/tasks/{taskId}`

Partially updates editable task fields.

- Gateway route: `PATCH /api/tasks/{taskId}`
- Request DTO: `UpdateTaskRequest`
- Response: `200 OK`, `data.task` contains `TaskResponse`
- Authentication: required
- Authorization: `ROLE_ADMIN` and `ROLE_SUPER_ADMIN` can update any task; other users can update tasks they created or are assigned to
- Missing or inaccessible task: `404 NOT_FOUND`
- At least one editable field must be provided
- Omitted fields remain unchanged; `description` and `assigneeUserId` may be cleared with `null`
- Changing `assigneeUserId` requires `ROLE_ADMIN`, `ROLE_SUPER_ADMIN`, or task creator permission
- Status changes are not supported by this endpoint

### `PATCH /api/v1/tasks/{taskId}/status`

Changes the task status independently from generic task updates.

- Gateway route: `PATCH /api/tasks/{taskId}/status`
- Request DTO: `UpdateTaskStatusRequest`
- Response: `200 OK`, `data.task` contains `TaskResponse`
- Authentication: required
- Authorization: `ROLE_ADMIN` and `ROLE_SUPER_ADMIN` can change any task; other users can change tasks they created or are assigned to
- Missing or inaccessible task: `404 NOT_FOUND`
- All valid `TaskStatus` enum-to-enum changes are allowed for the MVP
- Other task fields remain unchanged; `updatedAt` is managed by the service
- Event side effect: writes `TASK_STATUS_CHANGED` to `outbox_events` in the
  same transaction as the status update
- Task history and workflow transition rules are not implemented yet

### `DELETE /api/v1/tasks/{taskId}`

Soft-deletes an active task without removing its database row.

- Gateway route: `DELETE /api/tasks/{taskId}`
- Request DTO: none
- Response: `200 OK` with the standard response metadata and no `data` field
- Authentication: required
- Authorization: `ROLE_ADMIN` and `ROLE_SUPER_ADMIN` can delete any task; other users can delete only tasks they created
- Assignment alone does not grant delete permission
- Missing, inaccessible, or already deleted task: `404 NOT_FOUND`
- Deleted tasks are excluded from normal get and list operations and cannot be updated or have their status changed
- `deletedAt` and `deletedByUserId` are internal persistence fields and are not exposed by `TaskResponse`

### `PATCH /api/v1/tasks/{taskId}/assignee`

Assigns, reassigns, or unassigns an active task.

- Gateway route: `PATCH /api/tasks/{taskId}/assignee`
- Request DTO: `AssignTaskRequest`
- Response: `200 OK`, `data.task` contains `TaskResponse`
- Authentication: required
- Authorization: `ROLE_ADMIN` and `ROLE_SUPER_ADMIN` can assign any task; other users can assign only tasks they created
- Assignment alone does not grant reassignment permission
- Missing, inaccessible, or deleted task: `404 NOT_FOUND`
- `assigneeUserId` must be present and contain a UUID or explicit `null` to unassign
- The service does not synchronously verify user existence in this endpoint
- Other task fields remain unchanged; `updatedAt` is managed by the service
- Event side effect: writes `TASK_ASSIGNED` to `outbox_events` in the same
  transaction as the assignment update

## Implemented DTOs

### CreateTaskRequest

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `title` | string | yes | Not blank; maximum 200 characters |
| `description` | string | no | Maximum 5000 characters |
| `priority` | enum | no | `LOW`, `MEDIUM`, `HIGH` |
| `assigneeUserId` | UUID string | no | Target user identifier |

### CreateTaskResponse

| Field | Type | Description |
| --- | --- | --- |
| `taskId` | UUID string | Task identifier |
| `title` | string | Task title |
| `description` | string or null | Task description |
| `status` | enum | Always `NEW` on creation |
| `priority` | enum | Task priority |
| `assigneeUserId` | UUID string or null | Assigned user, if provided |
| `createdByUserId` | UUID string | Derived from authenticated JWT subject |
| `createdAt` | string | ISO-8601 timestamp with offset |

### TaskResponse

| Field | Type | Description |
| --- | --- | --- |
| `taskId` | UUID string | Task identifier |
| `title` | string | Task title |
| `description` | string or null | Task description |
| `status` | enum | Current status: `NEW`, `IN_PROGRESS`, `DONE`, `CANCELLED` |
| `priority` | enum | `LOW`, `MEDIUM`, `HIGH` |
| `assigneeUserId` | UUID string or null | Assigned user identifier |
| `createdByUserId` | UUID string | Public identifier of the creator |
| `createdAt` | string | ISO-8601 timestamp with offset |
| `updatedAt` | string | ISO-8601 timestamp with offset |

### UpdateTaskRequest

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `title` | string | no | When provided, not null or blank; maximum 200 characters |
| `description` | string or null | no | Maximum 5000 characters; `null` clears the description |
| `priority` | enum | no | When provided, not null; `LOW`, `MEDIUM`, `HIGH` |
| `assigneeUserId` | UUID string or null | no | `null` clears the assignee |

### UpdateTaskStatusRequest

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `status` | enum | yes | `NEW`, `IN_PROGRESS`, `DONE`, `CANCELLED` |

### AssignTaskRequest

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `assigneeUserId` | UUID string or null | yes | UUID assigns or reassigns; explicit `null` unassigns |

## Enumerations

### TaskStatus

| Value | Description |
| --- | --- |
| `NEW` | Newly created task, not yet started |
| `IN_PROGRESS` | Task is being worked on |
| `DONE` | Task is completed |
| `CANCELLED` | Task was cancelled |

### TaskPriority

| Value | Description |
| --- | --- |
| `LOW` | Low priority |
| `MEDIUM` | Medium priority |
| `HIGH` | High priority |

## Planned Endpoints

The following endpoints are not yet implemented. Their contracts are defined
here as targets for future MVP work.

### `POST /api/v1/tasks/{taskId}/comments`

Adds a comment to a task.

- Request DTO: `CreateTaskCommentRequest`
- Response: `201 CREATED`, `data.comment` contains `TaskCommentResponse`

### `GET /api/v1/tasks/{taskId}/comments`

Returns comments for a task.

- Response: `200 OK`, `data.items` contains `TaskCommentResponse` entries and `data.page` contains pagination metadata
- Default sort: `createdAt,asc`

## Planned DTOs

### CreateTaskCommentRequest

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `text` | string | yes | Not blank; maximum 5000 characters |

### TaskCommentResponse

| Field | Type | Description |
| --- | --- | --- |
| `commentId` | UUID string | Comment identifier |
| `taskId` | UUID string | Task identifier |
| `authorUserId` | UUID string | Public identifier of author |
| `text` | string | Comment text |
| `createdAt` | string | ISO-8601 timestamp |
| `updatedAt` | string | ISO-8601 timestamp |

## Future History API

`task-service` owns `TaskHistory`, but a public history endpoint is outside the
initial contract. Add one only after retention, visibility, and audit
requirements are defined.
