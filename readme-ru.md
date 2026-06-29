# Platform

Микросервисная Task Management Platform на Spring Boot и Vue 3.

Текущая цель проекта - стабилизация MVP. Реализованы базовые сервисы пользователей,
задач, уведомлений, API Gateway, Config Server, Eureka, Docker Compose окружение,
frontend MVP и Kafka-based task notification flow.

## Реализовано

### Сервисы

- `user-service`
  - регистрация, вход, refresh/logout;
  - JWT access token и refresh-cookie flow;
  - MFA;
  - профиль пользователя и lifecycle учётной записи;
  - PostgreSQL/Flyway схема.
- `task-service`
  - создание, просмотр, список, обновление, смена статуса, назначение и soft-delete задач;
  - ownership/RBAC проверки для task operations;
  - transactional outbox в таблице `outbox_events`;
  - Kafka publication в topic `platform.task-events`.
- `notification-service`
  - создание и чтение уведомлений;
  - Kafka consumer для task events;
  - обработка `TASK_CREATED`, `TASK_ASSIGNED`, `TASK_STATUS_CHANGED`;
  - идемпотентность через `event_consumption_log`;
  - PostgreSQL/Flyway схема.
- `api-gateway`
  - маршруты `/api/users/**`, `/api/tasks/**`, `/api/notifications/**`;
  - JWT validation;
  - service discovery integration.
- `frontend/vue-frontend`
  - login/session restore;
  - task list, task details, create/edit/status/assignment/delete flows;
  - notifications page через `GET /api/notifications`.

### Инфраструктура

- PostgreSQL
- Redis
- Kafka
- MailHog
- Zipkin
- Config Server
- Eureka
- Docker Compose

## Notification Flow

Task notifications создаются только через Outbox Pattern + Kafka:

```text
Frontend
  -> API Gateway
  -> task-service
  -> TaskEntity + outbox_events
  -> Kafka topic platform.task-events
  -> notification-service
  -> notifications
  -> Frontend GET /api/notifications
```

`task-service` не вызывает `notification-service` напрямую для task notifications.

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

## Быстрый старт

```bash
cp .env.example .env
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

Frontend development server:

```bash
cd frontend/vue-frontend
npm install
npm run dev
```

## Postman

Postman collection находится в `doc/postman`.

Task notification checks создают задачу с `assigneeUserId`, ждут
`notificationWaitMillis`, затем проверяют `TASK_CREATED` / `IN_APP` уведомление
через `GET /api/notifications`. Задержка нужна из-за асинхронного Outbox + Kafka
пути.

## Планируется

- audit-service;
- production-grade deployment/Kubernetes;
- расширенные настройки уведомлений;
- email delivery из notification-service;
- read state / mark-as-read для уведомлений.

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

## Статус

Проект остаётся в стадии MVP stabilization. Текущая реализация использует Kafka
и transactional outbox для task notification flow.
