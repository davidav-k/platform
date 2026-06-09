# Environment Variables

This document is the authoritative environment variable contract for Platform
local development. Copy `.env.example` to `.env` before starting Docker
Compose. Never commit production or shared-environment values.

## Scope

The inventory covers `compose.yml`, Dockerfiles, Spring `application*.yml` and
`bootstrap.yml` files, the native Config Server repository in `config/`, Java
configuration lookups, and repository startup documentation.

`Required` means the variable must be present in the root `.env` file for the
documented Docker Compose startup path unless a more specific note says
otherwise. Docker Compose passes `.env` to several containers with `env_file`;
that does not mean every receiving container consumes every variable.

## Local Startup Minimum

Create `.env` from `.env.example`, then run:

```bash
docker compose --env-file .env -f compose.yml up -d
```

Required local values:

- `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`
- `JWT_SECRET`, `ADMIN_PASSWORD`
- `EMAIL_HOST`, `EMAIL_PORT`, `EMAIL_ID`, `EMAIL_PASSWORD`, `EMAIL_VERIFY_HOST`

Optional local overrides:

- `APPLICATION_PORT`
- `ACTIVE_PROFILE`
- `CONFIG_SERVER_URI`
- `EUREKA_URL`
- `JWT_EXPIRATION`

Never commit production values for passwords, JWT signing keys, admin
credentials, mail credentials, or externally managed service credentials.
`.env.example` contains development-only placeholders.

## Infrastructure

### PostgreSQL

| Name | Required | Default | Example | Consumed by | Description |
| --- | --- | --- | --- | --- | --- |
| `POSTGRES_HOST` | Yes | None | `postgres` | user-service | PostgreSQL hostname in the JDBC URL. |
| `POSTGRES_PORT` | Yes | None | `5432` | user-service | PostgreSQL port in the JDBC URL. |
| `POSTGRES_DB` | Yes | None | `users_db` | postgres, user-service, Compose healthcheck | Local user-service database name. |
| `TASK_POSTGRES_DB` | No | `tasks_db` | `tasks_db` | postgres init, task-service | Local task-service database name. |
| `NOTIFICATION_POSTGRES_DB` | No | `notifications_db` | `notifications_db` | postgres init, notification-service | Local notification-service database name. |
| `POSTGRES_USER` | Yes | None | `user` | postgres, user-service, Compose healthcheck | PostgreSQL username. |
| `POSTGRES_PASSWORD` | Yes | None | `dev_example_postgres_password_change_me` | postgres, user-service | Development-only PostgreSQL password placeholder. |

### Redis

Redis is started by Docker Compose without custom environment variables.

### Config Server

| Name | Required | Default | Example | Consumed by | Description |
| --- | --- | --- | --- | --- | --- |
| `SPRING_CLOUD_CONFIG_SERVER_NATIVE_SEARCH_LOCATIONS` | No; Compose sets it inline | `file:../../config/` in bundled configuration | `file:/config/` | config-server | Overrides the native Config Server repository path inside its container. |

Config Server clients default to the Docker-network URL
`http://config-server:8888`. Set `CONFIG_SERVER_URI=http://localhost:8888`
when running user-service from an IDE against the local Config Server
container.

| Name | Required | Default | Example | Consumed by | Description |
| --- | --- | --- | --- | --- | --- |
| `CONFIG_SERVER_URI` | No | `http://config-server:8888` | `http://localhost:8888` | user-service | Overrides the Config Server client URL for IDE launches. |

### Eureka

User-service defaults to the Docker-network Eureka URL
`http://eureka-server:8761/eureka`. Set `EUREKA_URL=http://localhost:8761/eureka`
for an IDE launch.

| Name | Required | Default | Example | Consumed by | Description |
| --- | --- | --- | --- | --- | --- |
| `EUREKA_URL` | No | `http://eureka-server:8761/eureka` | `http://localhost:8761/eureka` | user-service | Overrides the Config Server-owned Eureka URL for IDE launches. |

## Security

### JWT

| Name | Required | Default | Example | Consumed by | Description |
| --- | --- | --- | --- | --- | --- |
| `JWT_SECRET` | Yes | None | Base64 development-only signing key from `.env.example` | user-service, task-service, api-gateway | Base64-encoded JWT signing key. All services decode it before constructing HMAC keys. Replace it outside local development. |
| `JWT_EXPIRATION` | No | `432000` | `432000` | user-service | Optional JWT expiration override in seconds. Config Server defaults development to five days. |
| `ADMIN_PASSWORD` | Yes | None | `dev_example_admin_password_change_me` | user-service | Password used when the local bootstrap admin account is created. |

### Cookies

Cookie names are fixed in application code. Cookie attributes and lifetimes
have explicit configuration defaults and optional environment overrides.

| Name | Required | Default | Description |
| --- | --- | --- | --- |
| `AUTH_COOKIE_SECURE` | No | `false` | Set to `true` for HTTPS deployments. |
| `AUTH_COOKIE_SAME_SITE` | No | `Lax` | Explicit `SameSite` value for authentication cookies. |
| `AUTH_ACCESS_COOKIE_MAX_AGE_SECONDS` | No | `600` | Access-cookie browser lifetime in seconds. |
| `AUTH_REFRESH_COOKIE_MAX_AGE_SECONDS` | No | `7200` | Refresh-cookie browser lifetime in seconds. |

### Encryption Secrets

No separate encryption-secret environment variable is active.

## Application

| Name | Required | Default | Example | Consumed by | Description |
| --- | --- | --- | --- | --- | --- |
| `ACTIVE_PROFILE` | No | `dev` | `dev` | user-service, task-service | Active Spring profile override. |
| `APPLICATION_PORT` | No | service-specific | `8085` (user), `8086` (task), `8087` (notification) | user-service, task-service, notification-service | HTTP port override. Compose sets `APPLICATION_PORT=8086` for task-service explicitly. Changing this alone breaks routing and health checks. |

No logging environment variable is active. Infrastructure service ports are
fixed in configuration and Compose:

| Component | Port |
| --- | --- |
| API Gateway | `8080` |
| User Service | `8085` |
| Task Service | `8086` |
| Notification Service | `8087` |
| Config Server | `8888` |
| Eureka Server | `8761` |
| PostgreSQL | `5432` |
| Redis | `6379` |
| MailHog SMTP | `1025` |
| MailHog UI | `8025` |
| Zipkin | `9411` |

## External Integrations

### Mail

| Name | Required | Default | Example | Consumed by | Description |
| --- | --- | --- | --- | --- | --- |
| `EMAIL_HOST` | Yes | None | `mailhog` | user-service | SMTP host for verification and password-reset mail. |
| `EMAIL_PORT` | Yes | None | `1025` | user-service | SMTP port. |
| `EMAIL_ID` | Yes | None | `dev_sender@example.local` | user-service | SMTP username and message sender identity. |
| `EMAIL_PASSWORD` | Yes | None | `dev_example_email_password_change_me` | user-service | Development-only SMTP password placeholder. |
| `EMAIL_VERIFY_HOST` | Yes | None | `http://localhost:8085` | user-service | Base URL for verification and password-reset links. |

### Future Integrations

No active environment variable contract exists for Kafka, Prometheus, Grafana,
production mail providers, or OpenAI integration.

## Commented Fallback Toggles

These names occur only in commented Compose examples. They are not part of the
required `.env` contract:

| Name | Required | Default | Example | Consumed by | Description |
| --- | --- | --- | --- | --- | --- |
| `SPRING_CLOUD_CONFIG_ENABLED` | No | Spring default | `false` | Spring applications if uncommented | Disables Config Server loading for a classpath-only fallback startup. |
| `SPRING_CONFIG_IMPORT` | No | Bootstrap configuration | `optional:classpath:/application.yml` | Spring applications if uncommented | Selects classpath configuration for the fallback startup path. |

## Source Map

| Source | Variables |
| --- | --- |
| `.env.example` | All required local values and optional service overrides |
| `compose.yml` | `POSTGRES_USER`, `POSTGRES_DB`, `SPRING_CLOUD_CONFIG_SERVER_NATIVE_SEARCH_LOCATIONS`, `APPLICATION_PORT` for task-service; commented fallback toggles |
| `config/user-service-dev.yml` | PostgreSQL, mail, JWT, admin, and `APPLICATION_PORT` variables |
| `config/task-service-dev.yml` | PostgreSQL (`TASK_POSTGRES_DB`), JWT, Eureka, `APPLICATION_PORT`, and `NOTIFICATION_SERVICE_ENABLED`, `NOTIFICATION_SERVICE_BASE_URL`, `NOTIFICATION_SERVICE_CONNECT_TIMEOUT`, `NOTIFICATION_SERVICE_READ_TIMEOUT` variables |
| `config/notification-service-dev.yml` | PostgreSQL (`NOTIFICATION_POSTGRES_DB`), Eureka, and `APPLICATION_PORT` variables |
| `backend/user-service/src/main/resources/application.yml` | `spring.application.name` only |
| `backend/user-service/src/main/resources/bootstrap.yml` | `ACTIVE_PROFILE` and optional `CONFIG_SERVER_URI` override |
| `backend/user-service/src/main/resources/application-dev.yml` | Retained development-profile marker only |
| `backend/task-service/src/main/resources/application.yml` | `spring.application.name` only |
| `backend/task-service/src/main/resources/bootstrap.yml` | `ACTIVE_PROFILE` and optional `CONFIG_SERVER_URI` override |
| `backend/notification-service/src/main/resources/application.yml` | `spring.application.name` only |
| `backend/notification-service/src/main/resources/bootstrap.yml` | `ACTIVE_PROFILE` and optional `CONFIG_SERVER_URI` override |
| `infrastructure/api-gateway/.../JwtUtil.java` | Direct `JWT_SECRET` lookup |
| Dockerfiles | No environment variables |

Config Server is the primary source of truth for user-service runtime
configuration. The bundled resources contain only bootstrap metadata and safe
developer convenience defaults. See
[Configuration management](../architecture/configuration-management.md).
