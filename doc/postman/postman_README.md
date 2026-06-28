# Postman MVP End-to-End Checks

1. API Gateway readiness.
2. Administrator authorization.
3. Regular user registration and activation.
4. Regular user login.
5. Regular user task creation.
6. Retrieval, update, status change, and soft delete of a task.
7. Ownership/RBAC check.
8. Administrator task creation with regular user assignment.
9. Kafka/Outbox notification flow check.
10. Refresh token flow.
11. Current logout contract.

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

## How to run
1. Import collection
2. Import environment
3. `Platform Local - Postman Template` environment.
4. `adminPassword` and `userPassword`.