# AI Service API Contract

AI Service exposes task-assistance operations through `/api/v1/ai/tasks`.
Successful responses use the platform response envelope with operation payloads under `data.result`.
All operations accept a JSON body with `title` and `description`; validation errors return `400 BAD_REQUEST` with field errors directly under `data` (e.g., `data.title`, `data.description`).

## Improve Task Description

`POST /api/v1/ai/tasks/description/improve`

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

`POST /api/v1/ai/tasks/subtasks/suggest`

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

`POST /api/v1/ai/tasks/summary`

Response data:

```json
{
  "result": {
    "summary": "Improve onboarding clarity."
  }
}
```

## Suggest Priority

`POST /api/v1/ai/tasks/priority/suggest`

Response data:

```json
{
  "result": {
    "priority": "HIGH",
    "reason": "Blocks onboarding release."
  }
}
```

## Validation

All operations require:

- `title`: non-blank, maximum 200 characters.
- `description`: non-blank, maximum 5000 characters.

Invalid requests return `400 BAD_REQUEST`.

## Provider Errors

- Provider unavailable: `503 SERVICE_UNAVAILABLE`.
- Provider timeout: `504 GATEWAY_TIMEOUT`.
- Provider failure: `502 BAD_GATEWAY`.