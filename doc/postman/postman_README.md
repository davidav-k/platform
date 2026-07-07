# Postman MVP End-to-End Checks

1. API Gateway readiness.
2. Administrator authorization.
3. Regular user registration and activation.
4. Regular user login.
5. Regular user task creation.
6. Retrieval, update, status change, and soft delete of a task.
7. Ownership/RBAC check.
8. Administrator task creation with regular user assignment.
9. Kafka/Outbox notification flow check for `TASK_CREATED` / `IN_APP`.
10. Refresh token flow.
11. Current logout contract.
12. Audit API security, filters, pagination, and record lookup.
13. Task, user, and notification event delivery into Audit Service.
14. AI Service MVP operations through API Gateway and AI audit verification.

## Postman Environment Variables

| Variable | Purpose |
|---|---|
| `baseUrl` | API Gateway URL. Defaults to `http://localhost:8080`. |
| `adminEmail` | Administrator email. Defaults to `admin@mail.com`. |
| `adminPassword` | Administrator password from `.env` / `ADMIN_PASSWORD`. Manually populated. |
| `userEmail` | Email of the test standard user. |
| `userPassword` | Password of the test standard user. Manually populated. |
| `accessToken` | Current access token. Automatically populated. |
| `refreshToken` | Current refresh token. Automatically populated. |
| `adminAccessToken` | Administrator access token. Automatically populated. |
| `userAccessToken` | Standard user access token. Automatically populated. |
| `adminUserId` | Administrator UUID. Automatically populated. |
| `userId` | Standard user UUID. Automatically populated. |
| `taskId` | UUID of the main test task. Automatically populated. |
| `assignedTaskId` | UUID of the task assigned to a regular user. Automatically populated. |
| `unrelatedTaskId` | UUID of the administrator task for RBAC/ownership checks. Automatically populated. |
| `notificationId` | UUID of the found notification. Automatically populated. |
| `notificationWaitMillis` | Kafka/outbox processing wait time. Recommended: 7000. |
| `auditBaseUrl` | Direct Audit Service URL. Defaults to `http://localhost:8088`. |
| `auditId` | Public audit UUID captured from an Audit API response. |
| `auditEventType` | Captured event type used by the filter request. |
| `auditAggregateType` | Captured aggregate type used by the filter request. |
| `auditAggregateId` | Captured aggregate UUID used by the filter request. |
| `auditSourceService` | Captured source service used by the filter request. |
| `auditAction` | Captured action used by the filter request. |
| `auditCreatedTaskId` | Task UUID created by the dedicated Audit E2E flow. |
| `auditNotificationId` | Notification UUID created by the Audit E2E flow. |
| `auditRunStartedAt` | Timestamp used to isolate user and notification events from the current E2E run. |
| `auditWaitMillis` | Delay between retryable Audit API checks. Defaults to 3000 ms. |
| `aiTaskTitle` | AI E2E task title. Automatically populated. |
| `aiTaskDescription` | AI E2E task description. Automatically populated. |
| `aiRunStartedAt` | Timestamp used to isolate AI audit events from the current E2E run. |
| `aiAuditId` | Public audit UUID captured from an AI Audit API response. |

The notification check creates a task with `assigneeUserId`, waits
`notificationWaitMillis`, then reads `GET /api/notifications` through the
Gateway and expects a `TASK_CREATED` notification for the assigned recipient.
The wait is required because task-service writes an outbox event and
notification-service creates the notification asynchronously after Kafka
delivery.
That notification creation also writes `NOTIFICATION_SYSTEM_CREATED` to the
notification-service outbox for publication to
`platform.notification-events`. Audit Service consumes that event and stores
it with `sourceService=notification-service`.

The existing registration and login requests also exercise user-service audit
event production. Verify those events through the user database
`outbox_events` table, Kafka topic `platform.user-events`, and Audit Service
read requests. The resulting records use `sourceService=user-service`.

The Vue Audit Log reads through API Gateway at `GET /api/audit` and
`GET /api/audit/{auditId}`. The collection's Audit folder continues to target
Audit Service directly at `{{auditBaseUrl}}/api/v1/audit` so its service-level
security checks remain independent of Gateway routing.

The AI Service folder calls only Gateway routes under `{{baseUrl}}/api/ai`.
It reuses `adminAccessToken` from the existing login requests and validates AI
responses structurally because generated text is non-deterministic. The folder
also verifies AI audit records through the Gateway Audit API at
`{{baseUrl}}/api/audit`, expecting `sourceService=ai-service` records for
`AI_TASK_DESCRIPTION_IMPROVED`, `AI_SUBTASKS_SUGGESTED`,
`AI_TASK_SUMMARIZED`, and `AI_PRIORITY_SUGGESTED`.

## How to run

1. Start the complete Docker Compose stack and verify it:

   ```bash
   docker compose --env-file .env -f compose.yml up -d --build
   ./scripts/check-local-stack.sh
   ```

2. Import `platform-mvp-e2e.updated.postman_collection.json` and
   `platform-local.updated.postman_environment.json`.
3. Select `Platform Local - Postman Template` and populate `adminPassword` and
   `userPassword`.
4. Run the complete collection to retain the existing MVP checks.
5. Run `05 - Audit Service` after the setup folders to verify 401, 403, 200,
   filters, sorting, detail lookup, and 404 behavior.
6. Run `06 - Audit E2E Flow` with Collection Runner. It uses the existing
   active admin account, creates and mutates a task, creates a notification,
   and retries Audit API queries while Kafka/outbox processing completes.
7. Run `07 - AI Service` after admin login/setup. It exercises all AI MVP
   operations through `/api/ai/**`, checks unauthenticated and invalid-payload
   failures, then retries `/api/audit` until AI events are visible.

Expected Audit E2E events are `TASK_CREATED`, `TASK_UPDATED`, `TASK_ASSIGNED`,
`TASK_STATUS_CHANGED`, `TASK_DELETED`, `USER_LOGIN_SUCCESS`,
`USER_LOGIN_FAILED`, `NOTIFICATION_CREATED`, and
`NOTIFICATION_SYSTEM_CREATED`.

Expected AI audit events are `AI_TASK_DESCRIPTION_IMPROVED`,
`AI_SUBTASKS_SUGGESTED`, `AI_TASK_SUMMARIZED`, and `AI_PRIORITY_SUGGESTED`.

The equivalent dependency-free local check is:

```bash
./scripts/verify-audit-flow.sh
```

It reads `ADMIN_PASSWORD` from `.env`. Optional overrides are `BASE_URL`,
`AUDIT_BASE_URL`, `ADMIN_EMAIL`, `AUDIT_POLL_ATTEMPTS`, and
`AUDIT_POLL_DELAY_SECONDS`.
