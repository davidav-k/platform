# Platform

Microservice-based Task Management Platform built with Spring Boot.

> Current focus: MVP stabilization. User, task, and notification services are operational.

## Implemented

### Services

- user-service
  - Registration
  - Authentication
  - JWT access/refresh flow
  - MFA
  - Profile and account lifecycle management
- task-service
  - Create, get, list, update, status change, assignment, and soft delete
  - Ownership/RBAC, filtering, and pagination
  - JWT-based authentication
  - Flyway-managed PostgreSQL schema
- notification-service
  - Create, get, and list notifications with filtering and pagination
  - Task-created notification integration
  - JWT-based authentication
  - Flyway-managed PostgreSQL schema
  - Docker Compose and API Gateway integration
- api-gateway
  - Routing
  - JWT validation
  - Service discovery integration
- config-server
- eureka-server
- Vue 3 frontend
  - Cookie-based login, profile loading, and session restoration
  - Guarded task list, details, create, edit, status, assignment, and soft-delete flows
  - Guarded notification list and details flows
  - Shared loading, empty, error, retry, and Not Found UX

### Infrastructure

- PostgreSQL
- Redis
- MailHog
- Zipkin
- Docker Compose

## Planned

- Kafka and outbox integration
- Audit service
- Kubernetes deployment

## Technology Stack

- Java 17
- Spring Boot 3.4.x
- Spring Cloud 2024.x
- Spring Security
- PostgreSQL and Flyway
- Redis
- Docker Compose
- Vue 3, Vite, Vue Router, and native Fetch API
- JUnit 5, Mockito, H2, and Testcontainers

## Project Structure

```text
backend/
|-- user-service
|-- task-service
`-- notification-service

infrastructure/
|-- api-gateway
|-- config-server
`-- eureka-server

frontend/
`-- vue-frontend

config/
doc/
```

## Quick Start

```bash
cp .env.example .env
docker compose --env-file .env -f compose.yml up -d --build
```

API Gateway: `http://localhost:8080`

Verify startup:

```bash
./scripts/check-local-stack.sh
```

Windows:

```powershell
.\scripts\check-local-stack.ps1
```

Run the frontend separately from Docker Compose:

```bash
cd frontend/vue-frontend
npm install
npm run dev
```

See the [frontend README](frontend/vue-frontend/README.md) for routes, features,
configuration, limitations, and the MVP verification checklist.

## Documentation

- Architecture - `doc/architecture.md`
- Development workflow - `doc/development-workflow.md`
- Authentication flow - `doc/security/auth-flow.md`
- Database migration strategy - `doc/database/migration-strategy.md`
- Service boundaries - `doc/architecture/service-boundaries.md`
- Environment variables - `doc/configuration/env-variables.md`
- Technical debt - `doc/technical-debt.md`
- Frontend MVP - `frontend/vue-frontend/README.md`

## Status

The project is under active MVP development.
