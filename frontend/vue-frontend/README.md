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

## Route Access

Public routes:

- `/`
- `/login`

Protected routes:

- `/profile`
- `/tasks`
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

Every API request uses `credentials: "include"`. Authentication tokens remain
in backend-issued HttpOnly cookies and are never stored in local storage or
session storage.

The backend currently has no public logout endpoint. The Logout button clears
only the frontend's in-memory state; it cannot delete HttpOnly cookies. A page
reload may therefore restore the authenticated session until the backend adds
a cookie-clearing logout contract or the cookies expire.
