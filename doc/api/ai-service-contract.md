# AI Service API Contract

AI Service exposes task-assistance operations internally through
`/api/v1/ai/tasks`. External clients and the frontend must call the API Gateway
route `/api/ai/tasks`; Gateway rewrites `/api/ai/**` to `/api/v1/ai/**`.
Do not call `/ai-service/**` or direct service URLs from clients.

Successful responses use the platform response envelope with operation payloads
under `data.result`. All operations accept a JSON body with `title` and
`description`; validation errors return `400 BAD_REQUEST` with field errors
directly under `data` such as `data.title` and `data.description`.

## Improve Task Description

Gateway route: `POST /api/ai/tasks/description/improve`

Internal route: `POST /api/v1/ai/tasks/description/improve`

Request:

```json
{
  "title": "Improve onboarding",
  "description": "Make onboarding clearer."
}
```

Response data:

```json
{
  "result": {
    "improvedDescription": "Add acceptance criteria and rollout notes."
  }
}
```

## Suggest Subtasks

Gateway route: `POST /api/ai/tasks/subtasks/suggest`

Internal route: `POST /api/v1/ai/tasks/subtasks/suggest`

Response data:

```json
{
  "result": {
    "subtasks": [
      "Draft acceptance criteria",
      "Review with product"
    ]
  }
}
```

## Summarize Task

Gateway route: `POST /api/ai/tasks/summary`

Internal route: `POST /api/v1/ai/tasks/summary`

Response data:

```json
{
  "result": {
    "summary": "Improve onboarding clarity."
  }
}
```

## Suggest Priority

Gateway route: `POST /api/ai/tasks/priority/suggest`

Internal route: `POST /api/v1/ai/tasks/priority/suggest`

Response data:

```json
{
  "result": {
    "priority": "HIGH",
    "reason": "Blocks onboarding release."
  }
}
```

## Authentication

All AI endpoints require a valid platform access JWT. The service accepts the
same `Authorization: Bearer <token>` header and `access-token` HttpOnly cookie
used by the other backend services. Authenticated platform roles `USER`,
`MANAGER`, `ADMIN`, and `SUPER_ADMIN` are authorized.

Unauthenticated or invalid-token requests return `401 UNAUTHORIZED`.
Authenticated tokens without an authorized platform role return
`403 FORBIDDEN`.

## Validation

All operations require:

- `title`: non-blank, maximum 200 characters.
- `description`: non-blank, maximum 5000 characters.

Invalid requests return `400 BAD_REQUEST`. The request DTOs do not accept task
priority, status, assignee, task ID, prompt options, or provider options.

## Provider Errors

- Provider unavailable: `503 SERVICE_UNAVAILABLE`.
- Provider timeout: `504 GATEWAY_TIMEOUT`.
- Provider failure: `502 BAD_GATEWAY`.

## Audit Events

Successful AI operations create Outbox-backed audit events in AI Service and
publish them to `platform.ai-events`. Audit Service stores normalized records
with `sourceService=ai-service` and `aggregateType=AI_OPERATION`.

AI event payloads include only operation metadata: `operationId`,
`operationType`, `actorUserId`, `actorEmail`, `occurredAt`, `providerName`, and
`modelName`. Prompts, task text, generated text, JWTs, cookies, authorization
headers, passwords, and secrets must not be published.