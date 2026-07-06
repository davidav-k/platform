# Vue Frontend

## Overview

This directory contains the Vue 3 MVP client for the Task Management Platform.
It provides cookie-authenticated access to profile, task management,
notification, and administrator audit workflows through the API Gateway.

The frontend is intentionally small. Backend services remain authoritative for
authentication, authorization, task ownership, assignment, and validation.

## Technology Stack

- Vue 3
- Vite 6
- Vue Router 4
- Native Fetch API
- Plain CSS


## Prerequisites

- Node.js 18 or later
- npm 9 or later
- The backend environment running from the repository root for local Vite mode
- API Gateway available at `http://localhost:8080` by default

Start and verify the backend before running authenticated frontend flows:

```bash
docker compose --env-file .env -f compose.yml up -d --build
./scripts/check-local-stack.sh
```

Docker Compose can run the complete MVP, including the production-style
frontend container. Vite remains available for frontend development.


## Available Pages

Public routes:

| Route | Page |
| --- | --- |
| `/` | Home |
| `/login` | Login |
| `/:pathMatch(.*)*` | Page Not Found fallback |

Protected routes:

| Route | Page |
| --- | --- |
| `/profile` | Current user profile |
| `/tasks` | Filtered and paginated task list |
| `/tasks/create` | Create task form |
| `/tasks/:id` | Task details and task actions |
| `/tasks/:id/edit` | Edit task form |
| `/notifications` | Paginated notification list |
| `/notifications/:id` | Notification details |
| `/audit` | Filtered and paginated Audit Log |
| `/audit/:auditId` | Audit record details |

Protected navigation first attempts session restoration through the profile
endpoint. Unauthenticated users are redirected to login and returned to the
requested internal route after successful authentication.

## Supported MVP Features

### Authentication

- Login with email and password
- Load the current profile
- Restore a session from valid HttpOnly cookies after refresh
- Authenticated and guest-only route guards
- Frontend logout and navigation reset

### Tasks

- List tasks with status and priority filters
- Navigate through backend pagination
- View task details
- Create and edit tasks with client and backend validation feedback
- Use AI assistance in task create/edit forms to improve descriptions,
  suggest subtasks, summarize a task, and suggest priority
- Change task status
- Assign, reassign, and unassign by user UUID
- Soft delete with explicit confirmation
- Handle loading, empty, retry, validation, access-denied, and not-found states

Supported task statuses are `NEW`, `IN_PROGRESS`, `DONE`, and `CANCELLED`.
Supported priorities are `LOW`, `MEDIUM`, and `HIGH`.

### Notifications

- List notifications with backend pagination
- View notification details
- Display notification type, channel, delivery status, timestamps, and failure
  information when provided by the backend
- Read Kafka-backed task-created notifications through
  `GET /api/notifications`

The implemented notification API does not expose read state or a mark-as-read
operation.

### Audit Log

- Browse audit records with event type, aggregate type, source service, and
  action filters
- Sort and navigate through backend pagination, newest events first by default
- Open a read-only audit detail page using the public `auditId`
- Display explicit loading, empty, access-denied, not-found, and retry states

Audit routes require an authenticated frontend session. Audit Service remains
the authorization source of truth and returns `403 Forbidden` unless the user
has `ROLE_ADMIN` or `ROLE_SUPER_ADMIN`. The current Audit API deliberately does
not return stored event payloads, so the detail page cannot display payload JSON.

## Frontend Services

API access stays in `src/services`:

- `apiClient.js`: Fetch helpers, cookie credentials, response parsing, and safe errors
- `authService.js`: Login and refresh mappings
- `profileService.js`: Current profile mapping
- `taskService.js`: Task list, details, create, update, status, assignment, and delete mappings
- `notificationService.js`: Notification list and details mappings
- `auditService.js`: Audit list, filters, pagination, and details mappings
- `authState.js`: Minimal Vue reactive authentication state

Views and components do not call `fetch` directly.

## Known Limitations

- The backend has no public logout endpoint. Logout clears in-memory frontend
  state but cannot remove HttpOnly cookies, so refresh can restore the session
  until the cookies expire.
- MFA verification UI is not implemented; an MFA-required response displays a
  placeholder message.
- Assignment uses a user UUID because no public user-search or assignment-
  candidate endpoint exists.
- Notification mark-as-read, polling, WebSocket, and realtime updates are not
  implemented or exposed by current backend contracts.
- There is no advanced audit analytics, export, write action, bulk task
  workflow, or task restore UI.
- Kafka and the transactional outbox run in the backend. The frontend does not
  expose event-delivery controls and only reads persisted notifications through
  the Gateway.
- Frontend component tests run with `npm test` (Vitest); browser E2E tests are not configured yet.


For backend endpoint verification, seeded users, and the broader service E2E
workflow
