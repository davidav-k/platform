# Platform

Microservice-based Task Management Platform built with Spring Boot.



## Planned

- Audit event pipeline and API
- Prometheus/Grafana monitoring

## Technology Stack

- Java 17
- Spring Boot 3.4.x
- Spring Cloud 2024.x
- Spring Security
- PostgreSQL and Flyway
- Redis
- Apache Kafka
- Docker Compose
- Vue 3, Vite, Vue Router, and native Fetch API
- JUnit 5, Mockito, H2, and Testcontainers

## Project Structure

```text
backend/
|-- user-service
|-- task-service
|-- notification-service
`-- audit-service

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
cp .env.example .envdocker compose --env-file .env -f compose.yml up -d --build
```

Frontend: `http://localhost:5173`

Core local ports:

| Component | Port |
| --- | --- |
| API Gateway | `8080` |
| User Service | `8085` |
| Task Service | `8086` |
| Notification Service | `8087` |
| Audit Service | `8088` |
| Frontend | `5173` |
| Config Server | `8888` |
| Eureka Server | `8761` |
| PostgreSQL | `5432` |
| Redis | `6379` |
| Kafka host listener | `9092` |
| MailHog SMTP/UI | `1025` / `8025` |
| Zipkin | `9411` |

Verify startup:

```bash
./scripts/check-local-stack.sh
```

Windows:

```powershell
.\scripts\check-local-stack.ps1
```

## Documentation

- Architecture - `doc/architecture.md`
- Development workflow - `doc/development-workflow.md`
- Authentication flow - `doc/security/auth-flow.md`
- Database migration strategy - `doc/database/migration-strategy.md`
- Service boundaries - `doc/architecture/service-boundaries.md`
- Environment variables - `doc/configuration/env-variables.md`
- Kafka notification verification - `doc/kafka-notification-e2e-verification.md`
- Postman collection guide - `doc/postman/postman_README.md`
- Technical debt - `doc/technical-debt.md`
- Frontend MVP - `frontend/vue-frontend/README.md`

## Status

The project is under active MVP development.
