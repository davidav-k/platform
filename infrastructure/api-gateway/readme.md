# API Gateway Service
### Overview
This service acts as a centralized entry point for the microservice architecture,
handling routing, authentication, and cross-cutting concerns like CORS and circuit breaking.

### Features
- JWT-based authentication
- Dynamic service discovery via Eureka
- Intelligent request routing
- Circuit breaker pattern implementation
- CORS configuration
- Path rewriting

### Technologies
- Spring Cloud Gateway
- Spring Cloud Netflix Eureka Client
- JWT for authentication
- Resilience4j for fault tolerance

### Configuration

The API Gateway currently defines:

| External route | Internal rewrite | State |
| --- | --- | --- |
| `/api/users/**` | `/api/v1/user/**` | Implemented and routed to `user-service` |
| `/api/tasks/**` | `/api/v1/task/**` | Reserved route only; no runnable `task-service` exists |

The user route currently forwards `POST`, `GET`, `PUT`, `DELETE`, and
`OPTIONS`. It does not forward `PATCH`.

Notification routes do not exist yet. The future routing contract is
documented in
[Service boundaries](../../doc/architecture/service-boundaries.md).

### Authentication
Protected requests are validated using JWT tokens. The gateway accepts the
`access-token` cookie or an `Authorization` header with the Bearer scheme. It
extracts the username and passes it as an `X-Authenticated-User` header to
downstream services. Downstream services still validate JWTs independently.

Registration, login, account verification, MFA verification, and refresh are
allowed through the gateway without an access token so `user-service` can
apply the endpoint-specific checks.

### API Routes
- User Service: http://localhost:8080/api/users/**
- Reserved task route: http://localhost:8080/api/tasks/**

Use [Health checks](../../doc/operations/health-checks.md) to verify Gateway
health and the read-only user-service routing probe.
