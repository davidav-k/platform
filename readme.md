# Platform

Microservice-based Task Management Platform built with Spring Boot.

> Current focus: MVP stabilization — user-service and task-service are operational.

## Implemented

### Services

- user-service
  - Registration
  - Authentication
  - JWT access/refresh flow
  - MFA
  - Profile management
  - User lifecycle management

- task-service
  - Create task
  - Get task by ID
  - List tasks with filters and pagination
  - JWT-based authentication
  - Flyway-managed PostgreSQL schema

- api-gateway
  - Routing
  - JWT validation
  - Service discovery integration

- config-server
- eureka-server

### Infrastructure

- PostgreSQL
- Redis
- MailHog
- Zipkin
- Docker Compose

## Planned

- notification-service
- Kafka integration
- Audit service
- Vue 3 frontend
- Kubernetes deployment

## Technology Stack

- Java 17
- Spring Boot 3.4.x
- Spring Cloud 2024.x
- Spring Security
- PostgreSQL
- Flyway
- Redis
- Docker Compose
- JUnit 5
- Mockito
- Testcontainers

## Project Structure

```text
backend/
├── user-service
├── task-service
└── notification-service

infrastructure/
├── api-gateway
├── config-server
└── eureka-server

frontend/
└── vue-frontend

config/
doc/
```

## Quick Start

```bash
cp .env.example .env

docker compose --env-file .env -f compose.yml up -d --build
```

API Gateway:

```text
http://localhost:8080
```

Verify startup:

```bash
./scripts/check-local-stack.sh
```

Windows:

```powershell
.\scripts\check-local-stack.ps1
```

## Documentation

- Architecture — `doc/architecture.md`
- Development Workflow — `doc/development-workflow.md`
- Authentication Flow — `doc/security/auth-flow.md`
- Database Migration Strategy — `doc/database/migration-strategy.md`
- Service Boundaries — `doc/architecture/service-boundaries.md`
- Environment Variables — `doc/configuration/env-variables.md`
- Technical Debt — `doc/technical-debt.md`

## Status

The project is under active MVP development.
