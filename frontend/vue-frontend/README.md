# Vue Frontend

## Overview

This directory contains the Vue 3 MVP client for the Task Management Platform.
It provides cookie-authenticated access to profile, task management, and
notification workflows through the API Gateway.

The frontend is intentionally small. Backend services remain authoritative for
authentication, authorization, task ownership, assignment, and validation.

## Technology Stack

- Vue 3
- Vite 6
- Vue Router 4
- Native Fetch API
- Plain CSS

The MVP does not use Axios, Pinia, Vuex, a UI framework, or a form library.

## Prerequisites

- Node.js 18 or later
- npm 9 or later
- The backend environment running from the repository root for local Vite mode
- API Gateway available at `http://localhost:8080` by default

Start and verify the backend before running authenticated frontend flows:

```bash
cp .env.example .env
docker compose --env-file .env -f compose.yml up -d --build
./scripts/check-local-stack.sh
```

Docker Compose can run the complete MVP, including the production-style
frontend container. Vite remains available for frontend development.

## Configuration

Copy the frontend environment example when a local override is needed:

```bash
cd frontend/vue-frontend
cp .env.example .env
```

Default configuration:

```dotenv
VITE_API_BASE_URL=http://localhost:8080
```

`VITE_*` variables are embedded into the JavaScript bundle at build time. They
are not runtime container environment variables. In Docker Compose mode,
`VITE_API_BASE_URL` from the root `.env` file is passed as an image build
argument; changing it requires rebuilding the frontend image.

All browser API requests go through API Gateway. The shared native Fetch client
sets `credentials: "include"` so backend-issued HttpOnly access and refresh
cookies are sent with requests. Tokens are not stored in local storage or
session storage and are not logged by the frontend.

## Running Locally

From `frontend/vue-frontend`:

```bash
npm install
npm run dev
```

Open the local URL printed by Vite.

## Running With Docker Compose

From the repository root, start the complete MVP:

```bash
cp .env.example .env
docker compose --env-file .env -f compose.yml up -d --build
./scripts/check-local-stack.sh
```

URLs:

- Frontend: `http://localhost:5173`
- API Gateway: `http://localhost:8080`

The frontend image is built with Node and served by nginx. Nginx falls back to
`index.html` for Vue Router paths, so browser refreshes work on task,
notification, and unknown frontend routes.

To build only the frontend image:

```bash
docker compose --env-file .env -f compose.yml build frontend
```

## Production Build

```bash
npm run build
```

The generated bundle is written to `dist/`. Non-nginx production hosting must serve
`index.html` for unknown paths so Vue Router history-mode URLs work after a
browser refresh.

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

The implemented notification API does not expose read state or a mark-as-read
operation.

## Frontend Services

API access stays in `src/services`:

- `apiClient.js`: Fetch helpers, cookie credentials, response parsing, and safe errors
- `authService.js`: Login and refresh mappings
- `profileService.js`: Current profile mapping
- `taskService.js`: Task list, details, create, update, status, assignment, and delete mappings
- `notificationService.js`: Notification list and details mappings
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
- There is no audit UI, advanced dashboard, bulk task workflow, or task restore UI.
- Kafka and the transactional outbox are design work only; the frontend does
  not expose event-delivery controls.
- Automated frontend component and browser tests are not configured yet.

## MVP Verification Checklist

- [ ] Start the backend stack and verify API Gateway at `http://localhost:8080`.
- [ ] Start Vite with `npm run dev`.
- [ ] Login with an existing enabled user and verify profile loading.
- [ ] Refresh a protected route and verify cookie-based session restoration.
- [ ] List and filter tasks.
- [ ] Create a task and open its details page.
- [ ] Edit the task.
- [ ] Change its status.
- [ ] Assign, reassign, or unassign it using a valid user UUID.
- [ ] Soft delete it and verify return to the task list.
- [ ] Open the notifications list and notification details.
- [ ] Logout and verify protected routes redirect to login.
- [ ] Open an unknown URL and verify the Page Not Found view.
- [ ] Run `npm run build` successfully.

For backend endpoint verification, seeded users, and the broader service E2E
workflow
