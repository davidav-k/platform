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

### `PUT /api/v1/tasks/{taskId}`

Replaces editable task fields.

- Request DTO: `UpdateTaskRequest`
- Response: `200 OK`, `data.task` contains `TaskResponse`
- Authorization: creator, `ROLE_ADMIN`, or `ROLE_SUPER_ADMIN`

### `DELETE /api/v1/tasks/{taskId}`

Soft-deletes a task.

- Response: `200 OK`, `data` is empty
- Authorization: creator, `ROLE_ADMIN`, or `ROLE_SUPER_ADMIN`
- Whether deletion is soft or hard must be decided before implementation.

### `PATCH /api/v1/tasks/{taskId}/status`

Changes the task status and records task history.

- Request DTO: `UpdateTaskStatusRequest`
- Response: `200 OK`, `data.task` contains `TaskResponse`
- Transition rules are a task-domain decision and must be finalized before implementation.

### `POST /api/v1/tasks/{taskId}/assign`

Assigns a task to a user.

- Request DTO: `AssignTaskRequest`
- Response: `201 CREATED`, `data.assignment` contains `TaskAssignmentResponse`

### `DELETE /api/v1/tasks/{taskId}/assign/{userId}`

Removes an active task assignment.

- Response: `200 OK`, `data` is empty

### `POST /api/v1/tasks/{taskId}/comments`

Adds a comment to a task.

- Request DTO: `CreateTaskCommentRequest`
- Response: `201 CREATED`, `data.comment` contains `TaskCommentResponse`

### `GET /api/v1/tasks/{taskId}/comments`

Returns comments for a task.

- Response: `200 OK`, `data.items` contains `TaskCommentResponse` entries and `data.page` contains pagination metadata
- Default sort: `createdAt,asc`

## Planned DTOs

### UpdateTaskRequest

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `title` | string | yes | Not blank; maximum 200 characters |
| `description` | string | no | Maximum 5000 characters |
| `priority` | enum | yes | `LOW`, `MEDIUM`, `HIGH` |

### UpdateTaskStatusRequest

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `status` | enum | yes | `NEW`, `IN_PROGRESS`, `DONE`, `CANCELLED` |

### AssignTaskRequest

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `userId` | UUID string | yes | Existing active `user-service` public user identifier |

### CreateTaskCommentRequest

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `text` | string | yes | Not blank; maximum 5000 characters |

### TaskAssignmentResponse

| Field | Type | Description |
| --- | --- | --- |
| `assignmentId` | UUID string | Assignment identifier |
| `taskId` | UUID string | Task identifier |
| `userId` | UUID string | Assigned public user identifier |
| `assignedAt` | string | ISO-8601 timestamp |
| `assignedByUserId` | UUID string | Public identifier of assigning actor |

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
