# Task Service API Contract

## Scope

This document defines future `task-service` endpoints only. It does not
describe implemented controllers. The service owns tasks, assignments,
statuses, comments, and task history as defined in
[Service boundaries](../architecture/service-boundaries.md).

All endpoints:

- use the response envelope from [API contract standards](api-contract-standards.md)
- require a valid access JWT
- use UUID strings for task-domain identifiers
- use the stable `user-service` public `userId` for user references

## DTOs

### CreateTaskRequest

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `title` | string | yes | Not blank; maximum 200 characters |
| `description` | string | no | Maximum 5000 characters |
| `priority` | enum | yes | `LOW`, `MEDIUM`, `HIGH`, or `CRITICAL` |
| `dueAt` | string | no | ISO-8601 timestamp with offset |

### UpdateTaskRequest

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `title` | string | yes | Not blank; maximum 200 characters |
| `description` | string | no | Maximum 5000 characters |
| `priority` | enum | yes | `LOW`, `MEDIUM`, `HIGH`, or `CRITICAL` |
| `dueAt` | string | no | ISO-8601 timestamp with offset |

Status and assignments are intentionally excluded. Their dedicated endpoints
make authorization and history recording explicit.

### UpdateTaskStatusRequest

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `status` | enum | yes | `TODO`, `IN_PROGRESS`, `BLOCKED`, or `DONE` |

Allowed transition rules are a task-domain decision and must be finalized
before implementation.

### AssignTaskRequest

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `userId` | UUID string | yes | Existing active `user-service` public user identifier |

### CreateTaskCommentRequest

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `text` | string | yes | Not blank; maximum 5000 characters |

### TaskResponse

| Field | Type | Description |
| --- | --- | --- |
| `taskId` | UUID string | Task identifier |
| `title` | string | Task title |
| `description` | string or null | Task description |
| `priority` | enum | Task priority |
| `status` | enum | Current task status |
| `creatorUserId` | UUID string | Public identifier of the creator |
| `assignments` | array of `TaskAssignmentResponse` | Current assignments |
| `dueAt` | string or null | ISO-8601 timestamp |
| `createdAt` | string | ISO-8601 timestamp |
| `updatedAt` | string | ISO-8601 timestamp |

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

## Task Endpoints

### `POST /api/v1/tasks`

Creates a task owned by the authenticated user.

- Request DTO: `CreateTaskRequest`
- Response: `201 CREATED`, `data.task` contains `TaskResponse`
- Authorization: authenticated `ROLE_USER`, `ROLE_MANAGER`, `ROLE_ADMIN`, or
  `ROLE_SUPER_ADMIN`
- Validation: request DTO rules apply

### `GET /api/v1/tasks/{taskId}`

Returns one visible task.

- Request DTO: none
- Response: `200 OK`, `data.task` contains `TaskResponse`
- Authorization: creator, assignee, `ROLE_ADMIN`, or `ROLE_SUPER_ADMIN`;
  manager scope remains to be defined
- Validation: `taskId` must be a UUID

### `PUT /api/v1/tasks/{taskId}`

Replaces editable task fields.

- Request DTO: `UpdateTaskRequest`
- Response: `200 OK`, `data.task` contains `TaskResponse`
- Authorization: creator, `ROLE_ADMIN`, or `ROLE_SUPER_ADMIN`; manager scope
  remains to be defined
- Validation: `taskId` and request DTO rules apply

### `DELETE /api/v1/tasks/{taskId}`

Deletes a task according to the future retention policy.

- Request DTO: none
- Response: `200 OK`, `data` is empty
- Authorization: creator, `ROLE_ADMIN`, or `ROLE_SUPER_ADMIN`; manager scope
  remains to be defined
- Validation: `taskId` must be a UUID

Whether deletion is soft or hard deletion must be decided before
implementation.

### `GET /api/v1/tasks`

Returns a filtered page of tasks visible to the caller.

- Request DTO: none
- Response: `200 OK`, `data.items` contains `TaskResponse` entries and
  `data.page` contains standard pagination metadata
- Authorization: authenticated user sees created or assigned tasks;
  `ROLE_ADMIN` and `ROLE_SUPER_ADMIN` may see all tasks
- Pagination: standard `page`, `size`, and `sort`; default sort is
  `createdAt,desc`
- Optional filters: `status`, `priority`, `assigneeUserId`, `creatorUserId`
- Validation: identifier filters must be UUIDs; enum filters must be supported values

## Assignment Endpoints

### `POST /api/v1/tasks/{taskId}/assign`

Assigns a task to a user. Repeating an active assignment is a conflict.

- Request DTO: `AssignTaskRequest`
- Response: `201 CREATED`, `data.assignment` contains `TaskAssignmentResponse`
- Authorization: creator, `ROLE_ADMIN`, or `ROLE_SUPER_ADMIN`; manager scope
  remains to be defined
- Validation: `taskId` and `userId` must be UUIDs; assignment target must be
  an existing active user

### `DELETE /api/v1/tasks/{taskId}/assign/{userId}`

Removes an active task assignment.

- Request DTO: none
- Response: `200 OK`, `data` is empty
- Authorization: creator, `ROLE_ADMIN`, or `ROLE_SUPER_ADMIN`; manager scope
  remains to be defined
- Validation: `taskId` and `userId` must be UUIDs

## Status Endpoint

### `PATCH /api/v1/tasks/{taskId}/status`

Changes the task status and records task history.

- Request DTO: `UpdateTaskStatusRequest`
- Response: `200 OK`, `data.task` contains `TaskResponse`
- Authorization: creator, assignee, `ROLE_ADMIN`, or `ROLE_SUPER_ADMIN`;
  manager scope remains to be defined
- Validation: `taskId` must be a UUID; status must be supported; transition
  must satisfy task-domain rules

## Comment Endpoints

### `POST /api/v1/tasks/{taskId}/comments`

Adds a comment to a visible task.

- Request DTO: `CreateTaskCommentRequest`
- Response: `201 CREATED`, `data.comment` contains `TaskCommentResponse`
- Authorization: creator, assignee, `ROLE_ADMIN`, or `ROLE_SUPER_ADMIN`;
  manager scope remains to be defined
- Validation: `taskId` and request DTO rules apply

### `GET /api/v1/tasks/{taskId}/comments`

Returns comments for a visible task.

- Request DTO: none
- Response: `200 OK`, `data.items` contains `TaskCommentResponse` entries and
  `data.page` contains standard pagination metadata
- Authorization: creator, assignee, `ROLE_ADMIN`, or `ROLE_SUPER_ADMIN`;
  manager scope remains to be defined
- Pagination: standard `page`, `size`, and `sort`; default sort is
  `createdAt,asc`
- Validation: `taskId` must be a UUID

## Future History API

`task-service` owns `TaskHistory`, but a public history endpoint is outside the
initial contract. Add one only after retention, visibility, and audit
requirements are defined.
