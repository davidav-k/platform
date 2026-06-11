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

## Backend Gateway

The expected local API Gateway URL is `http://localhost:8080`. Copy
`.env.example` to `.env` to customize the URL for local development:

```bash
cp .env.example .env
```

The API client is prepared to send cookies with requests, but the application
does not call backend endpoints yet.
