# AI Service

AI Service provides authenticated task-assistance operations for the Task
Management Platform. It exposes a thin REST controller, delegates business
work to use cases, calls an `AiProvider` abstraction, and writes audit events
through its service-owned Outbox table after successful operations.

External clients must use API Gateway routes under `/api/ai/**`. The internal
service contract remains `/api/v1/ai/**` and is reached through Gateway route
rewriting.

## Supported MVP Operations

All MVP operations accept the same request body:

```json
{
  "title": "Improve onboarding",
  "description": "Make onboarding clearer."
}
```

| Operation | Gateway endpoint | Response payload |
| --- | --- | --- |
| Improve Task Description | `POST /api/ai/tasks/description/improve` | `data.result.improvedDescription` |
| Suggest Subtasks | `POST /api/ai/tasks/subtasks/suggest` | `data.result.subtasks[]` |
| Summarize Task | `POST /api/ai/tasks/summary` | `data.result.summary` |
| Suggest Priority | `POST /api/ai/tasks/priority/suggest` | `data.result.priority`, `data.result.reason` |

Request validation is enforced at the API boundary. `title` is required and
limited to 200 characters. `description` is required and limited to 5000
characters. The request body does not accept task `priority`, status, assignee,
or task identifiers.

## Authentication

AI endpoints require the same platform access JWT used by the other backend
services. AI Service accepts either:

- `Authorization: Bearer <access-token>`
- the `access-token` HttpOnly cookie

Authorized roles are `ROLE_USER`, `ROLE_MANAGER`, `ROLE_ADMIN`, and
`ROLE_SUPER_ADMIN`. Unauthenticated or invalid-token requests return `401`;
authenticated users without an allowed role return `403`. Actuator
`/actuator/health` and `/actuator/info` are public.

## Provider Abstraction

The application layer depends on `AiProvider`, which defines one method per MVP
operation. This keeps controller/use-case code independent from the concrete AI
provider.

Current code in this checkout includes:

- `AiProvider` abstraction;
- `TemporaryNoOpAiProvider` for `dev` and `test` profiles when no other
  provider bean exists;
- provider metadata labels used only for audit payloads.

This checkout does not contain an Ollama provider implementation, an Ollama
base URL property, an Ollama timeout property, or a Compose-managed Ollama
container. Do not configure or document Ollama runtime behavior for this branch
until a concrete provider bean and its configuration properties are added to
code.

## Configuration

AI Service receives runtime configuration from Config Server
`config/ai-service-dev.yml` and standard Spring bootstrap files.

| Property / env variable | Purpose |
| --- | --- |
| `APPLICATION_PORT` | AI Service HTTP port. Compose sets `8089`. |
| `POSTGRES_HOST`, `POSTGRES_PORT`, `AI_POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | PostgreSQL datasource for the service-owned AI database. |
| `JWT_SECRET` | JWT validation key shared with the platform. Do not log or expose it. |
| `EUREKA_URL` | Eureka registration URL when overriding Config Server defaults. |
| `AI_PROVIDER_NAME` | Non-sensitive provider label included in AI audit payloads. Default: `temporary-noop`. |
| `AI_MODEL_NAME` | Non-sensitive model label included in AI audit payloads. Default: `not-configured`. |
| `AI_OUTBOX_PUBLISHER_ENABLED` | Enables AI outbox polling. Default: `true`. |
| `AI_OUTBOX_PUBLISHER_ADAPTER` | `kafka` or `logging`. Default: `kafka`. |
| `AI_OUTBOX_PUBLISHER_BATCH_SIZE` | Maximum outbox rows claimed per poll. Default: `20`. |
| `AI_OUTBOX_PUBLISHER_MAX_RETRIES` | Maximum publish attempts. Default: `3`. |
| `AI_OUTBOX_PUBLISHER_FIXED_DELAY_MILLIS` | Delay between publisher polling cycles. Default: `5000`. |
| `AI_OUTBOX_PUBLISHER_KAFKA_BOOTSTRAP_SERVERS` | Kafka bootstrap servers for AI event publication. Falls back to `KAFKA_BOOTSTRAP_SERVERS`. |
| `AI_OUTBOX_PUBLISHER_KAFKA_TOPIC` | Kafka topic for AI audit events. Falls back to `KAFKA_AI_EVENTS_TOPIC`, default `platform.ai-events`. |

## Audit Events

Each successful AI operation writes an event to `outbox_events` in the AI
Service database. The outbox poller publishes the platform event envelope to
Kafka topic `platform.ai-events`. Audit Service consumes that topic, normalizes
AI events, and stores audit records.

Event types:

- `AI_TASK_DESCRIPTION_IMPROVED`
- `AI_SUBTASKS_SUGGESTED`
- `AI_TASK_SUMMARIZED`
- `AI_PRIORITY_SUGGESTED`

Outbox aggregate type is `AI_OPERATION`. Because the REST contract does not
include a task ID, the generated `operationId` is also used as the aggregate
ID.

Payload fields are intentionally limited to:

- `operationId`
- `operationType`
- `actorUserId`
- `actorEmail`
- `occurredAt`
- `providerName`
- `modelName`

Prompts, task text, generated AI text, JWTs, cookies, passwords, authorization
headers, and secrets must not be written to the AI outbox event payload or
published to Kafka.

## Running Locally

From the repository root, start AI Service with its required infrastructure:

```bash
docker compose --env-file .env -f compose.yml up -d --build \
  postgres kafka config-server eureka-server audit-service ai-service
```

Verify health and service registration:

```bash
curl -fsS http://localhost:8089/actuator/health
curl -fsS http://localhost:8761/eureka/apps/AI-SERVICE
```

Run tests/build locally:

```bash
mvn -f backend/ai-service/pom.xml clean test
mvn -f backend/ai-service/pom.xml clean package
```

## Local LLM / Ollama Notes

Ollama is not managed by `compose.yml` in this checkout, and the current AI
Service code does not read Ollama connection settings. If a future provider is
implemented, install and start Ollama separately from the platform stack, pull
the configured model with `ollama pull <model>`, then add the provider bean and
real configuration properties before documenting exact connection behavior.

Until then, the `dev`/`test` fallback provider returns deterministic local
responses and does not call an external LLM.