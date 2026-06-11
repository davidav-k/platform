# Vue Frontend

Minimal Vue 3 frontend for the Task Management Platform MVP. Login and profile
loading use the API Gateway; task and notification pages remain placeholders.

## Prerequisites

- Node.js 18 or later
- npm

## Install Dependencies

```bash
npm install
```

## Run the Development Server

```bash
npm run dev
```

Vite prints the local development URL when the server starts.

## Build for Production

```bash
npm run build
```

The production bundle is written to `dist/`.

## API Configuration

The expected local API Gateway URL is `http://localhost:8080`. Copy
`.env.example` to `.env` to customize the URL for local development:

```bash
cp .env.example .env
```

The configured environment variable is:

```dotenv
VITE_API_BASE_URL=http://localhost:8080
```

Authentication uses `HttpOnly` access and refresh cookies. The shared API
client sends `credentials: "include"` with every request so the browser can
receive and send those cookies. Cookie values are not available to frontend
JavaScript.

## API Services

Frontend API calls are grouped into small service modules:

- `src/services/apiClient.js` provides fetch-based HTTP helpers and error handling.
- `src/services/authService.js` maps login and token refresh endpoints.
- `src/services/profileService.js` maps the current-user profile endpoint.
- `src/services/taskService.js` maps task list, create, read, update, status, and delete endpoints.
- `src/services/notificationService.js` maps notification list, read, and mark-as-read endpoints.

The service paths use the external API Gateway contracts (`/api/users`,
`/api/tasks`, and `/api/notifications`). The current backend does not expose a
public logout endpoint, so the logout service currently performs local cleanup
only.

Login, profile loading, and the task list are wired into the frontend.
Notification UI integration will be added in later steps.

## Create Task

The protected `/tasks/create` page submits `POST /api/tasks` through
`taskService`. The form uses the current `CreateTaskRequest` fields: required
title, optional description, optional priority, and optional assignee user ID.

Client validation matches the backend contract: title is required and limited
to 200 characters, description is limited to 5,000 characters, priority is
`LOW`, `MEDIUM`, or `HIGH`, and a supplied assignee user ID must be a UUID.
When priority is omitted, task-service defaults it to `MEDIUM`.
Submitting controls are disabled while the request is active to prevent
duplicate creation.

On success, the frontend expects the created task at `data.task` and navigates
to `/tasks/{taskId}`. Backend field validation messages are displayed next to
their matching fields when available.

## Task List

The protected `/tasks` page loads tasks through the API Gateway and displays
task ID, title, status, priority, assignee user ID, and creation date. Task IDs
and titles link to `/tasks/:id`; the full row is also clickable and keyboard
accessible.

The page supports the backend's `status` and `priority` filters. It requests 20
tasks per page sorted by newest creation date and provides previous/next page
controls using the backend's zero-based pagination metadata. Free-text search
is not included because it is not part of the current task-service contract.

The backend environment must have API Gateway, task-service, its database, and
cookie authentication available. Authorization and task visibility remain
owned by task-service; non-admin users receive only tasks they created or are
assigned to.

## Task Details

The protected `/tasks/:id` page loads one task through `GET /api/tasks/{id}`.
It displays the current `TaskResponse` fields: task ID, title, description,
status, priority, assignee user ID, creator user ID, creation time, and update
time. The page is read-only and provides Back to Tasks and Refresh task actions.

The frontend expects the task at `data.task` in the standard API response
envelope. Invalid UUIDs show an invalid-ID message, explicit `403` responses
show access denied, and `404` responses show that the task is missing or not
available to the current account. The backend intentionally returns `404` for
both missing and inaccessible tasks in normal ownership checks.

## Update Task

The protected `/tasks/:id/edit` page first loads the task through
`GET /api/tasks/{id}`, then prefills the reusable task form. It updates title,
description, and priority through `PATCH /api/tasks/{id}`. Status and assignment
are intentionally excluded from this workflow because they have separate
contracts and authorization behavior.

Title is required and limited to 200 characters, description is limited to
5,000 characters, and priority must be `LOW`, `MEDIUM`, or `HIGH`. An empty
description is sent as `null` to clear it. On success, the frontend returns to
`/tasks/:id`; Cancel returns there without sending an update.

## Route Access

Public routes:

- `/`
- `/login`

Protected routes:

- `/profile`
- `/tasks`
- `/tasks/create`
- `/tasks/:id/edit`
- `/tasks/:id`
- `/notifications`

Before the first navigation decision, the router tries to load `/api/users/profile`
once. A valid HttpOnly access cookie restores the authenticated user without
storing tokens in frontend storage. If restoration fails, protected routes
redirect to `/login`. Authenticated users who open `/login` are redirected to
`/profile`.

## Manual Authentication Check

1. Start the backend environment with the repository `compose.yml` workflow.
2. Set `VITE_API_BASE_URL` to the API Gateway URL, normally `http://localhost:8080`.
3. Run `npm run dev` and open the URL printed by Vite.
4. Sign in with an existing enabled backend user.
5. Confirm the profile fields and authenticated navigation are displayed.
6. Select Logout and confirm the frontend redirects to `/login` and clears its local auth state.

## Manual Route Guard Check

1. Without signing in, open `/tasks` and confirm the router redirects to `/login`.
2. Sign in and confirm navigation continues to `/profile`.
3. Open `/tasks` directly and confirm it is available.
4. Refresh `/tasks` and confirm the session is restored when auth cookies are valid.
5. Select Logout, then open a protected route and confirm it redirects to `/login`.

## Manual Task List Check

1. Start the backend environment and sign in with an existing user.
2. Open `/tasks` and confirm visible tasks load.
3. Apply status and priority filters and confirm the list returns to page one.
4. Use Previous and Next when enough tasks exist for multiple pages.
5. Select a task ID or title and confirm navigation to `/tasks/:id`.
6. Verify an account with no visible tasks receives the `No tasks found` state.
7. Stop task-service temporarily and confirm the retryable error state appears.

## Manual Task Details Check

1. Sign in, open `/tasks`, and select an existing task.
2. Confirm all available task fields are displayed at `/tasks/:id`.
3. Select Refresh task and confirm the task reloads.
4. Open `/tasks/not-a-uuid` and confirm the invalid-ID state appears.
5. Open a missing or inaccessible task UUID and confirm the not-available state.
6. Select Back to Tasks and confirm navigation to `/tasks`.

## Manual Create Task Check

1. Sign in, open `/tasks`, and select Create Task.
2. Submit an empty title and confirm client validation appears.
3. Enter valid title, description, priority, and optional assignee UUID values.
4. Submit once and confirm controls remain disabled while the request runs.
5. Confirm the task is created and navigation continues to `/tasks/{taskId}`.
6. Submit backend-invalid values when applicable and confirm field errors appear.

## Manual Update Task Check

1. Sign in, open a task, and select Edit.
2. Confirm title, description, and priority are prefilled.
3. Change one or more fields and submit once.
4. Confirm controls remain disabled while saving and navigation returns to task details.
5. Confirm the updated values are displayed after navigation.
6. Select Cancel and confirm no update request is sent.
7. Verify blank/long title and long description validation messages.
8. Verify missing or inaccessible task handling with an appropriate UUID.

Every API request uses `credentials: "include"`. Authentication tokens remain
in backend-issued HttpOnly cookies and are never stored in local storage or
session storage.

The backend currently has no public logout endpoint. The Logout button clears
only the frontend's in-memory state; it cannot delete HttpOnly cookies. A page
reload may therefore restore the authenticated session until the backend adds
a cookie-clearing logout contract or the cookies expire.
