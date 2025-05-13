# Task Management Platform

A microservice-based platform for managing users, tasks, notifications, and analytics. Built with Spring Boot 3.3.3 and modern DevOps principles, event-driven architecture, and scalable infrastructure.

## Architecture Overview

- `user-service` — handles user registration, authentication, and role management (JWT, Spring Security)
- `task-service` — manages tasks, statuses, and comments
- `notification-service` — email/push notifications (Kafka-based)
- `audit-service` — user activity logging
- `config-server` — centralized configuration using Spring Cloud Config
- `eureka-server` — service discovery via Spring Cloud Eureka
- `gateway` — API Gateway (Spring Cloud Gateway)
- Supporting services: `zipkin`, `mailhog`, `redis`, `postgres`

## Getting Started (Local)

### 1. Requirements

- Docker + Docker Compose
- Java 17+ and Maven (optional for manual builds)

### 2. Clone the Repository

```bash
git clone https://github.com/davidav-k/platform.git
```

### 3. Configure Environment Variables

Create a `.env` file at the project root (based on `.env.example`):


### 4. Launch the Application

```bash
docker compose --env-file .env -f compose.yml up -d
```

Services available at:

- `localhost:8085` — user-service
- `localhost:8080` — API Gateway
- `localhost:8025` — MailHog
- `localhost:9411` — Zipkin
- `localhost:8761` — Eureka dashboard

## Healthcheck and Startup Order

- PostgreSQL is monitored with `pg_isready` via Docker `healthcheck`
- Services wait for PostgreSQL to be fully initialized
- `depends_on.condition: service_healthy` is used for controlled startup


## Security

- JWT authentication with HS256
- Spring Security filters via API Gateway
- Role-based access: `ADMIN`, `USER`, `MODERATOR`

## Observability

- Zipkin for distributed tracing
- Spring Cloud Sleuth integration
- Prometheus + Grafana monitoring

## CI/CD & Production Readiness

- Compatible with GitHub Actions
- Ready for Kubernetes deployment
- Multi-profile support for environments

---

© 2025 [davidav-k](https://github.com/davidav-k)