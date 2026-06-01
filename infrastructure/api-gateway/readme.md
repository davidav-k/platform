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

The API Gateway routes traffic to the following services:
- User Service (/api/users/**)
- Task Service (/api/tasks/**)

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
- Task Service: http://localhost:8080/api/tasks/**
