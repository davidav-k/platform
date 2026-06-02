# Health Checks

## Available Endpoints

Each Spring Boot service exposes only the Actuator `health` and `info`
endpoints. Sensitive Actuator endpoints such as `env`, `beans`, `mappings`,
`configprops`, `heapdump`, and `threaddump` are not exposed.

| Service | Health URL | Info URL | Purpose |
| --- | --- | --- | --- |
| Config Server | `http://localhost:8888/actuator/health` | `http://localhost:8888/actuator/info` | Confirms application health and native configuration-repository availability |
| Eureka Server | `http://localhost:8761/actuator/health` | `http://localhost:8761/actuator/info` | Confirms service-discovery application readiness |
| User Service | `http://localhost:8085/actuator/health` | `http://localhost:8085/actuator/info` | Confirms application health, PostgreSQL connectivity, and SMTP availability |
| API Gateway | `http://localhost:8080/actuator/health` | `http://localhost:8080/actuator/info` | Confirms gateway application readiness |

A healthy service returns HTTP `200` with an aggregate response:

```json
{"status":"UP"}
```

Health details remain intentionally limited. Built-in indicators still
contribute to the aggregate status.

## Startup Verification Gap Analysis

Before `scripts/check-local-stack.sh`, local startup verification was a manual
procedure. Compose already defined health checks and health-aware startup
ordering, and the documentation listed direct Actuator calls, but developers
had to run separate commands and interpret the results themselves.

The local stack expects eight containers:

| Container | Published port | Docker health check |
| --- | --- | --- |
| `tsp_postgres` | `5432` | `pg_isready` |
| `tsp_redis` | `6379` | `redis-cli ping` |
| `tsp_zipkin` | `9411` | none |
| `mailhog` | `1025`, `8025` | none |
| `tsp_config` | `8888` | `/actuator/health` |
| `tsp_eureka` | `8761` | `/actuator/health` |
| `tsp_user_service` | `8085` | `/actuator/health` |
| `tsp_gateway` | `8080` | `/actuator/health` |

Gateway health proves that the gateway process is ready, but does not prove a
route to a downstream service. The automated verification adds a read-only
public route probe for `user-service`.

## Dependency Coverage

The strategy uses built-in Spring health indicators where active integrations
already exist:

| Dependency | Validation |
| --- | --- |
| PostgreSQL | User Service datasource health indicator |
| SMTP | User Service mail health indicator |
| Config repository | Spring Cloud Config Server repository health indicator |
| Redis | Docker `redis-cli ping` health check |
| Eureka | Eureka Server health endpoint plus dependent service registration |

User Service currently uses an in-process Guava cache and has no Spring Data
Redis integration. Redis is therefore validated independently by Docker
Compose rather than reported as a User Service health component.

## Startup Validation

Docker Compose uses health-aware startup dependencies:

1. PostgreSQL and Redis start with native probes.
2. Config Server starts and must report `UP`.
3. Eureka Server starts after Config Server and must report `UP`.
4. User Service starts after PostgreSQL, Config Server, and Eureka Server. Its
   health endpoint reports `UP` only when its datasource and configured SMTP
   server are healthy.
5. API Gateway starts after Config Server, Eureka Server, and User Service. It
   must report `UP`.

Services continue to register with Eureka as before. Config Server continues
to serve the native repository mounted from `./config`.

## Docker Validation

Start the stack:

```bash
docker compose --env-file .env -f compose.yml up -d --build
```

Run the read-only local verification script from the repository root:

```bash
./scripts/check-local-stack.sh
```

Windows PowerShell:

```powershell
.\scripts\check-local-stack.ps1
```

The script fails immediately when a required dependency is unavailable and
prints an `ERROR:` line with a diagnostic hint. A successful run ends with:

```text
Local platform stack verification passed.
```

The script verifies:

1. Docker CLI, Docker Compose v2, the Docker daemon, `.env`, and `compose.yml`.
2. All eight expected Compose containers are running.
3. The six containers with Docker health checks report `healthy`.
4. PostgreSQL accepts connections and Redis responds with `PONG`.
5. Config Server health and mounted-repository access.
6. Eureka Server health and registry access.
7. User Service and API Gateway health.
8. API Gateway routing to User Service through a public account-verification
   request with an intentionally invalid non-UUID key.

The routing probe does not create users, mutate confirmation data, send email,
or require credentials. `user-service` currently may return HTTP `200` with an
error envelope for the invalid key; a corrected implementation should return
HTTP `400`. The script accepts both responses as evidence that routing worked.

For manual inspection, use:

Inspect health status:

```bash
docker compose --env-file .env -f compose.yml ps
docker inspect --format '{{.Name}} {{.State.Health.Status}}' \
  tsp_config tsp_eureka tsp_user_service tsp_gateway tsp_postgres tsp_redis
```

Verify endpoints directly:

```bash
curl -fsS http://localhost:8888/actuator/health
curl -fsS http://localhost:8761/actuator/health
curl -fsS http://localhost:8085/actuator/health
curl -fsS http://localhost:8080/actuator/health
```

Verify that Config Server can serve the mounted repository:

```bash
curl -fsS http://localhost:8888/user-service/dev
```

Verify Eureka registration:

```bash
curl -fsS http://localhost:8761/eureka/apps
```

## Troubleshooting

### Docker daemon unavailable

The verification script exits before inspecting containers when Docker Desktop
or the Docker service is stopped. Start Docker, then run:

```bash
./scripts/check-local-stack.sh
```

### PostgreSQL unavailable

User Service stays unhealthy when PostgreSQL is unavailable or its
credentials, host, port, or database name are incorrect. Check:

```bash
docker compose --env-file .env -f compose.yml logs postgres user-service
```

### Redis unavailable

Redis is independently unhealthy when `redis-cli ping` does not return
`PONG`. Check:

```bash
docker compose --env-file .env -f compose.yml logs redis
```

### SMTP unavailable

User Service stays unhealthy when its configured SMTP server is unavailable.
For Docker Compose development, `EMAIL_HOST` must be `mailhog`, not
`localhost`. Check:

```bash
docker compose --env-file .env -f compose.yml logs mailhog user-service
```

### Config Server unavailable

Confirm that the Config Server container reports healthy and that `./config`
is mounted at `/config`. Check:

```bash
docker compose --env-file .env -f compose.yml logs config-server
curl -fsS http://localhost:8888/user-service/dev
```

### Eureka unavailable

User Service and API Gateway may start but fail service registration or
discovery if Eureka is unavailable. Check:

```bash
docker compose --env-file .env -f compose.yml logs eureka-server user-service gateway
curl -fsS http://localhost:8761/eureka/apps
```
