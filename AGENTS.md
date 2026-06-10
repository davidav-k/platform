# Platform Development Instructions

You are working on Platform — a microservice-based Task Management Platform.

## Goal

Current goal is MVP stabilization.

Priorities:

1. Startup stability
2. Security hardening
3. user-service stabilization
4. task-service stabilization
5. notification-service stabilization
6. Gateway and contract alignment
7. Frontend integration
8. Event-driven architecture preparation

Prefer small, reviewable, incremental changes.

Do not introduce large-scale refactoring unless explicitly requested.

---

## Technology Stack

* Java 17
* Spring Boot 3.4.x
* Spring Cloud 2024.x
* Spring Security
* Spring Cloud Gateway
* Eureka
* Config Server
* PostgreSQL 16.x
* Redis
* Docker Compose
* Maven
* JUnit 5
* Mockito
* Testcontainers
* Vue 3

Always verify version compatibility before upgrades.

---

## Architecture Rules

### Service Ownership

user-service:

* authentication
* authorization
* MFA
* user lifecycle

task-service:

* task lifecycle
* ownership
* assignment
* status transitions

notification-service:

* notifications
* preferences
* delivery

api-gateway:

* routing
* JWT validation
* gateway filters

Do not move responsibilities between services without explicit justification.

### Controllers

Controllers must:

* validate requests
* call use cases
* return DTOs

Controllers must not contain business logic.

### Service Layer

Service layer owns:

* business rules
* use cases
* transactions

### DTO Rules

Keep separation between:

* Entity
* DTO
* Security Principal

Never expose entities directly.

### Database Rules

Each service owns its own schema/database.

Never create:

* cross-service foreign keys
* cross-service repository access

---

## Security Rules

Always verify:

* JWT validation
* expiration handling
* refresh flow
* authorization rules
* MFA flow
* security filter ordering

Special attention:

* JWT expiration units
* HttpOnly cookies
* Secure cookies
* SameSite
* MaxAge
* RequestContext cleanup

Never log:

* passwords
* JWT tokens
* refresh tokens
* secrets

---

## Configuration Rules

Prefer:

* application.yml
* Config Server
* Environment Variables

Never hardcode:

* credentials
* secrets
* environment-specific values

---

## Coding Style

Prefer:

* simple code
* readable code
* explicit names

Avoid:

* premature optimization
* speculative design
* unnecessary abstractions

---

## Workflow

Before implementation:

1. Analyze existing code.
2. Identify affected modules.
3. Identify affected files.
4. Identify risks.
5. Propose minimal solution.

Do not start implementation before analysis.

After implementation:

1. List modified files.
2. Review architecture impact.
3. Review security impact.
4. Suggest tests.
5. Suggest documentation updates.

---

## Testing

Provide required tests for every change:

* Unit Tests
* Integration Tests
* MockMvc Tests
* Security Tests
* Testcontainers Tests

---

## Documentation

Update documentation when changing:

* API contracts
* Architecture
* Security
* Configuration
* Startup process

Relevant documents:

* README.md
* doc/architecture.md
* doc/development-workflow.md
* doc/security/*
* doc/configuration/*
* doc/technical-debt.md

---

## Technical Debt

Do not introduce new technical debt without documenting it.

Prefer reducing nearby technical debt when touching existing code.

---

## Required Response Format

Before implementation:

## Analysis

## Affected Modules

## Affected Files

## Risks

## Proposed Solution

After implementation:

## Summary

## Modified Files

## Tests

## Documentation Updates

## Remaining Technical Debt
