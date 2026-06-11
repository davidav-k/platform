# Vue Frontend

Minimal Vue 3 frontend for the Task Management Platform MVP. The application
currently provides navigation and placeholder pages only; backend API
integration will be implemented in later steps.

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
public logout endpoint, so no logout service function is provided yet.

These modules are not wired into the placeholder views. Real authentication,
task, profile, and notification UI integration will be added in later steps.
