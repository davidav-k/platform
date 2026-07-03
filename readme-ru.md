# Platform

Микросервисная Task Management Platform на Spring Boot и Vue 3.


## Технологический стек

- Java 17
- Spring Boot 3.4.x
- Spring Cloud 2024.x
- Spring Security
- PostgreSQL 16.x и Flyway
- Redis
- Kafka
- Docker Compose
- Vue 3, Vite, Vue Router
- Maven
- JUnit 5, Mockito, Testcontainers

## Структура проекта

```text
backend/
|-- user-service
|-- task-service
`-- notification-service

infrastructure/
|-- api-gateway
|-- config-server
|-- eureka-server
`-- redis

frontend/
`-- vue-frontend

config/
doc/
```

## Локальный старт

```bash
docker compose --env-file .env -f compose.yml up -d --build
```

Основные URL:

- API Gateway: `http://localhost:8080`
- Frontend: `http://localhost:5173`
- Eureka: `http://localhost:8761`
- MailHog: `http://localhost:8025`
- Zipkin: `http://localhost:9411`

Проверка локального стека:

```bash
./scripts/check-local-stack.sh
```

Windows:

```powershell
.\scripts\check-local-stack.ps1
```

## Postman

Postman collection находится в `doc/postman`.


## Документация

- Архитектура: `doc/architecture.md`
- Service boundaries: `doc/architecture/service-boundaries.md`
- Outbox Pattern: `doc/architecture/outbox-pattern-design.md`
- Development workflow: `doc/development-workflow.md`
- Auth flow: `doc/security/auth-flow.md`
- Environment variables: `doc/configuration/env-variables.md`
- Task API: `doc/api/task-service-contract.md`
- Notification API: `doc/api/notification-service-contract.md`
- Postman: `doc/postman/postman_README.md`
- Frontend: `frontend/vue-frontend/README.md`
