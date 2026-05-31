# Platform

* The project is under active development.

## Overview
Task Management Platform is a microservice-based system for managing tasks, with features for user authentication, task creation, editing, and notifications. The platform implements modern security measures with **access** and **refresh tokens** to ensure secure user authentication and authorization. **Access tokens** are stored in **Redis** for fast, scalable, and secure token management.

### Architecture
- **Microservices**: Loosely coupled services that can be developed, deployed, and scaled independently
- **API Gateway**: Central entry point that routes requests to appropriate microservices
- **Service Discovery**: Uses Eureka for dynamic service registration and discovery
- **Config Server**: Centralized configuration management for all microservices, providing externalized configuration properties stored in a Git repository
- **Event-Driven Communication**: Services communicate asynchronously through Kafka events

### Security
- **JWT Authentication**: Stateless authentication using signed JSON Web Tokens
- **Token Management**: Short-lived access tokens (15 minutes) and long-lived refresh tokens (7 days)
- **Redis Cache**: High-performance in-memory data store for token validation and blacklisting
- **Role-Based Access Control**: Granular permissions based on user roles

### Backend Services
- **User Service**: Manages user registration, authentication, and profile management
- **Task Service**: Handles task CRUD operations, assignments, and status tracking
- **Notification Service**: Delivers real-time notifications via WebSockets and email

### Data Storage
- **PostgreSQL**: Relational database for persistent data storage with transaction support
- **Redis**: In-memory data structure store for caching and real-time features
- **Database per Service**: Each microservice has its own database for independence

### DevOps & Infrastructure
- **Docker**: Containerization for consistent development and deployment environments
- **Kubernetes**: Container orchestration for automated deployment, scaling, and management
- **GitHub Actions**: CI/CD pipelines for automated testing and deployment
- **Prometheus & Grafana**: Monitoring and visualization of service metrics
- **Config Server**: Spring Cloud Config server for centralized, version-controlled configuration management

## Stack
- **Backend**: Java 17, Spring Boot 3.x
- **Frontend**: Vue.js 3, Vuex
- **Database**: PostgreSQL 14
- **Infrastructure**: Docker, Kubernetes, GitHub Actions, Kafka, Redis

## Run
### Local
- You need to have Docker and Docker Compose installed on your machine.
- Create a `.env` file in the root directory with same variables as `.env.example`
- Build and run the Docker containers using Docker Compose:
```bash
docker compose --env-file .env -f compose.yml up -d
```
- Access the application at `http://localhost:8080`

Check container readiness:
```bash
docker compose --env-file .env -f compose.yml ps
docker compose --env-file .env -f compose.yml logs -f config-server eureka-server user-service gateway
```

Expected local ports:
- `8080` API Gateway
- `8085` User Service
- `8888` Config Server
- `8761` Eureka Server
- `5432` PostgreSQL
- `6379` Redis
- `8025` MailHog UI
- `9411` Zipkin

Useful health checks:
```bash
curl -fsS http://localhost:8888/actuator/health
curl -fsS http://localhost:8761
```

## Development Workflow

Repository workflow documentation:
- [Development workflow](doc/development-workflow.md)
- [Development checklist](doc/development-checklist.md)
- [Technical debt tracking](doc/technical-debt.md)
- [Environment variables](doc/configuration/env-variables.md)

Pull requests should use the GitHub pull request template and target `dev` during MVP development unless the change is urgent repository maintenance for `main`.

Run Maven tests locally before opening a pull request:

```bash
mvn -B -f backend/user-service/pom.xml test
mvn -B -f infrastructure/api-gateway/pom.xml test
mvn -B -f infrastructure/config-server/pom.xml test
mvn -B -f infrastructure/eureka-server/pom.xml test
```

GitHub Actions runs the same Maven test matrix on pull requests and pushes to `main` and `dev`.

## Pre-commit

This repository uses `pre-commit` for basic file hygiene and secret scanning before commits.

Run all hooks manually:
```bash
pre-commit run --all-files
```

Configured checks include trailing whitespace cleanup, end-of-file fixing, YAML and JSON validation, merge conflict marker detection, private key detection, and GitGuardian secret scanning.
