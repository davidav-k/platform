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
| `/api/tasks` | `/api/v1/tasks` | Implemented and routed to `task-service` |
| `/api/tasks/**` | `/api/v1/tasks/**` | Implemented and routed to `task-service` |
| `/api/notifications` | `/api/v1/notifications` | Implemented and routed to `notification-service` |
| `/api/notifications/**` | `/api/v1/notifications/**` | Implemented and routed to `notification-service` |

The user route forwards `POST`, `GET`, `PUT`, `PATCH`, `DELETE`, and `OPTIONS`.
Password changes are exposed as `PATCH /api/users/password/{userId}`.

The gateway does not route notification-service internal endpoints such as
`/internal/api/v1/notifications/system`.

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
- Task Service: http://localhost:8080/api/tasks and http://localhost:8080/api/tasks/**
- Notification Service: http://localhost:8080/api/notifications and http://localhost:8080/api/notifications/**

Use [Health checks](../../doc/operations/health-checks.md) to verify Gateway
health and the read-only user-service routing probe.

### Manual Password-Change Route Check

After logging in with a local test account, verify the protected route with a
cookie jar and request-body file stored outside the repository:

```bash
curl -i --request PATCH \
  "http://localhost:8080/api/users/password/${USER_ID}" \
  --cookie /path/to/local-auth-cookies.txt \
  --header 'Content-Type: application/json' \
  --data @/path/to/local-password-change.json
```

The local JSON file contains `oldPassword`, `newPassword`, and
`confirmNewPassword`. Do not commit or log the cookie jar, request body, or
password values.
