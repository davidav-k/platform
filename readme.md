# Platform

* The project is under active development.

## Overview
Task Management Platform is an MVP-stage microservice project. The runnable
platform currently focuses on user registration, authentication, authorization,
profile management, and account lifecycle. Task management and platform
notifications are documented future service boundaries, not implemented
features.

## Current MVP Status

### Implemented

- `user-service`: registration, account verification email, login, access and
  refresh JWT cookies, MFA, profile access, account updates, locking, and
  deletion
- `api-gateway`: external `/api/users/**` routing, JWT early rejection,
  Eureka-backed service discovery, CORS, and circuit breaker fallback
- `config-server`: native configuration repository served from `./config`
- `eureka-server`: service registration and discovery
- Docker Compose support for PostgreSQL, Redis, MailHog, and Zipkin
- Flyway-owned `user-service` schema with Hibernate validation
- GitHub Actions Maven test matrix for the four implemented Java modules

### In Progress

- MVP stabilization of startup, security behavior, documentation, and
  user-service
- Gateway alignment for future services
- Frontend integration; `frontend/vue-frontend` currently contains
  documentation only

### Planned

- `task-service` and `notification-service`; their directories are
  documentation shells and their future APIs are documented contracts
- Event-driven communication and Kafka integration
- Audit service and OpenAI-backed automation
- Kubernetes, Helm, Prometheus, and Grafana deployment support

### Security
- **JWT Authentication**: Stateless authentication using signed JSON Web Tokens
- **Token Management**: Signed JWTs with explicit second-based expiration and shorter browser-cookie lifetimes
- **User Service Cache**: In-process Guava cache; Redis runs in Compose but is not integrated into user-service
- **Role-Based Access Control**: Granular permissions based on user roles

### Backend Services
- **User Service**: Manages user registration, authentication, and profile management
- **Task Service**: Future owner of task CRUD operations, assignments, and status tracking
- **Notification Service**: Future owner of system notifications, preferences, and platform email delivery

### Data Storage
- **PostgreSQL**: User-service persistence; schema changes are managed by Flyway
- **Redis**: Started and health-checked by Compose, but not currently used by user-service
- **Database per Service**: Each microservice has its own database for independence

### DevOps & Infrastructure
- **Docker**: Containerization for consistent development and deployment environments
- **GitHub Actions**: CI workflow for the Maven test matrix
- **Config Server**: Spring Cloud Config server backed by the local native repository in `./config`

## Stack
- **Backend**: Java 17, Spring Boot 3.4.0, Spring Cloud 2024.0.1
- **Database**: PostgreSQL 16.1, Flyway migrations
- **Infrastructure**: Docker Compose v2, Redis 7, MailHog, Zipkin, Eureka, Config Server
- **Future**: Vue 3 frontend, Kafka, Kubernetes, Helm, Prometheus, Grafana

## Run
### Local
- Install Docker with Docker Compose v2.
- Create the local environment file from the development-only template:
```bash
cp .env.example .env
```
- Replace placeholder values in `.env`. See [Environment variables](doc/configuration/env-variables.md).
- Build and run the containers from the repository root:
```bash
docker compose --env-file .env -f compose.yml up -d --build
```
- Use API Gateway as the external API base URL: `http://localhost:8080`

Check container readiness:
```bash
docker compose --env-file .env -f compose.yml ps
docker compose --env-file .env -f compose.yml logs -f config-server eureka-server user-service gateway
```

Verify the complete local stack after startup:
```bash
./scripts/check-local-stack.sh
```

Windows PowerShell:
```powershell
.\scripts\check-local-stack.ps1
```

Expected local ports:
- `8080` API Gateway
- `8085` User Service
- `8888` Config Server
- `8761` Eureka Server
- `5432` PostgreSQL
- `6379` Redis
- `1025` MailHog SMTP
- `8025` MailHog UI
- `9411` Zipkin

Useful health checks:
```bash
curl -fsS http://localhost:8888/actuator/health
curl -fsS http://localhost:8761/actuator/health
curl -fsS http://localhost:8085/actuator/health
curl -fsS http://localhost:8080/actuator/health
```

## Development Workflow

Repository workflow documentation:
- [Architecture overview](doc/architecture.md)
- [Development workflow](doc/development-workflow.md)
- [Development checklist](doc/development-checklist.md)
- [Technical debt tracking](doc/technical-debt.md)
- [Environment variables](doc/configuration/env-variables.md)
- [Database migration strategy](doc/database/migration-strategy.md)
- [Authentication flow](doc/security/auth-flow.md)
- [Health checks](doc/operations/health-checks.md)
- [API contract standards](doc/api/api-contract-standards.md)
- [Task service API contract](doc/api/task-service-contract.md)
- [Notification service API contract](doc/api/notification-service-contract.md)
- [Service boundaries](doc/architecture/service-boundaries.md)
- [Documentation audit](doc/reports/documentation-audit.md)

Pull requests should use the GitHub pull request template and target `dev` during MVP development unless the change is urgent repository maintenance for `main`.

Run Maven tests locally before opening a pull request:

```bash
mvn -B -f backend/user-service/pom.xml test
mvn -B -f infrastructure/api-gateway/pom.xml test
mvn -B -f infrastructure/config-server/pom.xml test
mvn -B -f infrastructure/eureka-server/pom.xml test
```

GitHub Actions runs the same Maven test matrix on pull requests and pushes to `main` and `dev`.

## Pre-commit

This repository uses `pre-commit` for basic file hygiene and secret scanning before commits.

Run all hooks manually:
```bash
pre-commit run --all-files
```

Configured checks include trailing whitespace cleanup, end-of-file fixing, YAML and JSON validation, merge conflict marker detection, private key detection, and GitGuardian secret scanning.
