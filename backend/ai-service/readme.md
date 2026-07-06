# AI Service

AI Service is currently an infrastructure-only Spring Boot microservice. It
registers with Eureka, loads configuration from Config Server, connects to its
own PostgreSQL database, runs Flyway, and exposes Actuator health information.

The service defines internal AI task-assistance use cases and a provider
abstraction for future provider integrations. No real AI provider integration,
public REST API, Kafka integration, or domain schema is included in this
bootstrap.

From the repository root, start the required services with:

```bash
docker compose --env-file .env -f compose.yml up -d --build \
  postgres config-server eureka-server ai-service
```

Verify the service:

AI REST endpoints require a valid platform access JWT via `Authorization: Bearer <token>` or the `access-token` cookie. Actuator health/info remain public.

```bash
curl -fsS http://localhost:8089/actuator/health
curl -fsS http://localhost:8761/eureka/apps/AI-SERVICE
```

Run its build locally with:

```bash
mvn -f backend/ai-service/pom.xml clean test
mvn -f backend/ai-service/pom.xml clean package
```
