# AI Service

AI Service provides authenticated task-assistance operations through the
platform API Gateway. Its application layer depends on the provider abstraction;
the `dev` and `test` profiles currently fall back to the temporary no-op
provider when no concrete provider bean is configured.

## Audit Events

Each successful AI operation writes an event to the service-owned
`outbox_events` table. The existing Outbox poller publishes the platform
envelope to Kafka topic `platform.ai-events`, and Audit Service normalizes and
stores it.

Event types:

- `AI_TASK_DESCRIPTION_IMPROVED`
- `AI_SUBTASKS_SUGGESTED`
- `AI_TASK_SUMMARIZED`
- `AI_PRIORITY_SUGGESTED`

The payload is intentionally limited to `operationId`, `operationType`,
`actorUserId`, `occurredAt`, `providerName`, and `modelName`. The
`actorUserId` value comes from the authenticated JWT subject. AI Service does
not publish `actorEmail` because email is not reliably available in its current
SecurityContext. Task text, generated responses, prompts, tokens, and secrets
are not published. The current REST contract has no task identifier, so
`operationId` is also used as the Outbox aggregate ID.

Provider and model audit labels are configured with `AI_PROVIDER_NAME` and
`AI_MODEL_NAME`. Their development defaults describe the temporary provider.

## Running

From the repository root:

```bash
docker compose --env-file .env -f compose.yml up -d --build \
  postgres kafka config-server eureka-server audit-service ai-service
```

AI REST endpoints require a valid platform access JWT via
`Authorization: Bearer <token>` or the `access-token` cookie. Actuator
health/info remain public.

```bash
curl -fsS http://localhost:8089/actuator/health
curl -fsS http://localhost:8761/eureka/apps/AI-SERVICE
```

Run the service build locally with:

```bash
mvn -f backend/ai-service/pom.xml clean test
mvn -f backend/ai-service/pom.xml clean package
```
