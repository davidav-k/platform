# Docker Infrastructure

Docker Compose at the repository root manages the current local MVP stack.
Application Dockerfiles live next to each implemented Java module.

## Current Services

| Service | Purpose |
| --- | --- |
| `postgres` | User-service persistence |
| `redis` | Independently running Redis container; not used for JWT storage |
| `mailhog` | Local SMTP capture |
| `zipkin` | Local tracing infrastructure container |
| `config-server` | Native Spring Cloud Config repository |
| `eureka-server` | Service discovery |
| `user-service` | User management and authentication |
| `gateway` | External API entry point |

`task-service` and `notification-service` are planned and are not Compose
services.

## Running The Project

From the repository root:

```bash
cp .env.example .env
docker compose --env-file .env -f compose.yml up -d --build
./scripts/check-local-stack.sh
```

See [Health checks](../../doc/operations/health-checks.md) and
[Environment variables](../../doc/configuration/env-variables.md).
