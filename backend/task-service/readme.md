# Task Service

## Status

Infrastructure skeleton. Service starts, registers in Eureka and loads configuration from Config Server.
Business logic is not yet implemented.

## Purpose

Manages tasks within the Task Management Platform (TMP).
Exposed via API Gateway at `/api/tasks/**`.

`task-service` will own tasks, assignments, statuses, comments, and task history.
It must not access the `user-service` database directly.

The HTTP API contract is defined in [Task service API contract](../../doc/api/task-service-contract.md).
Service boundary and gateway alignment notes are in [Service boundaries](../../doc/architecture/service-boundaries.md).

## Stack

- Java 17
- Spring Boot 3.4.0
- Spring Cloud 2024.0.1
- Maven

## Local Startup

### Prerequisites

Running infrastructure services:
- Config Server (`http://localhost:8888`)
- Eureka Server (`http://localhost:8761`)

### Run via Maven

```bash
CONFIG_SERVER_URI=http://localhost:8888 \
ACTIVE_PROFILE=dev \
mvn -f backend/task-service/pom.xml spring-boot:run
```

### Run via Docker Compose

```bash
docker compose --env-file .env -f compose.yml up -d --build task-service
```

### Health check

```
GET http://localhost:8086/actuator/health
```

### Build and test

```bash
mvn -B -f backend/task-service/pom.xml test
```
