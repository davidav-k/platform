# AGENTS.md

# Platform / Task Management Platform

This repository contains a Spring Boot microservices learning project.

The primary objective is to evolve the platform while preserving clean architecture, service boundaries, and incremental development.

---

# Technology Stack

Backend

- Java 17
- Spring Boot 3.4.x
- Spring Cloud 2024.x
- Spring Security
- Spring Cloud Gateway
- Spring Cloud Config
- Eureka Server
- Apache Kafka
- PostgreSQL 16
- Redis
- Flyway
- Maven
- Docker Compose

Testing

- JUnit 5
- Mockito
- Testcontainers

Frontend

- Vue 3
- Vite
- JavaScript

---

# Current Architecture

Infrastructure services

- config-server
- eureka-server
- api-gateway

Business services

- user-service
- task-service
- notification-service

Future services

- audit-service
- scheduler-service
- ai-service

Each service owns its own database/schema.

Never introduce shared persistence.

---

# Architectural Rules

Always preserve:

- Clean Architecture
- SOLID
- SRP
- Explicit transaction boundaries
- Thin controllers
- Business logic inside service layer
- Repository layer only for persistence
- DTOs separated from Entities
- Infrastructure isolated from API contracts

Do not move business logic into controllers.

Do not bypass service layer.

---

# Event Driven Architecture

Inter-service communication should prefer Kafka events.

Current pattern:

Task Service

↓

Outbox Pattern

↓

Kafka

↓

Notification Service

↓

Notification persistence

Future integrations should follow the same approach.

Avoid synchronous REST communication when events are appropriate.

Audit Service must consume Kafka events rather than REST APIs.

---

# Coding Rules

Prefer:

- readable code
- small classes
- cohesive methods
- descriptive naming

Avoid:

- unnecessary abstractions
- speculative design
- overengineering
- magic strings
- duplicated logic

When introducing constants, prefer dedicated constants/classes instead of inline literals.

---

# Dependency Rules

Do not introduce new libraries unless necessary.

Reuse existing Spring Boot capabilities whenever possible.

Keep dependency changes minimal.

Do not change Java or Spring versions without explicit reason.

---

# Database

Use Flyway for every schema change.

Never modify previous migrations.

Always create a new migration.

Keep migrations idempotent and deterministic.

---

# REST API

Do not break existing REST contracts.

Maintain backward compatibility whenever possible.

Keep controllers thin.

Validate input at API boundary.

Return consistent error responses.

---

# Security

Never log:

- JWT
- passwords
- secrets

Prefer HttpOnly cookies.

Use SecurityContext when user identity is required.

Never expose internal implementation details through API.

---

# Documentation

Whenever architecture changes:

Update

- README
- Mermaid diagrams
- Postman Collection
- architecture documentation if required

Documentation is part of the feature.

---

# Git Workflow

One branch = one feature.

Keep pull requests focused.

Prefer small atomic commits.

Do not combine unrelated changes.

---

# Before Changing Code

Always analyze the existing implementation first.

Follow the current project style.

Modify only what is necessary.

Avoid unrelated refactoring.

Do not rename files or packages without a strong reason.

---

# When Producing Changes

Explain:

1. what was changed

2. why it was changed

3. affected files

4. tests that should be executed

---

# Preferred Response Style

For implementation requests:

1. Brief implementation plan.

2. Concrete code or patch.

3. Testing checklist.

If information is missing, explicitly state assumptions instead of inventing code.

---

# Current Development Priority

Current roadmap:

1. Audit Service
2. Audit Event Pipeline
3. Audit REST API
4. Audit Frontend
5. Email/WebSocket Notifications
6. Scheduler Service
7. AI Service
8. Monitoring
9. CI/CD improvements
10. Production readiness

Architectural consistency is always more important than adding new features.
