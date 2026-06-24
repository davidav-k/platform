# Platform MVP Postman Verification

This directory contains an end-to-end Postman flow for the locally started MVP. All application requests use API Gateway at `http://localhost:8080`; the collection does not call service ports directly.

## Files

- `platform-mvp-e2e.postman_collection.json` - ordered MVP verification flow
- `platform-local.postman_environment.json` - local environment template without credentials or tokens

## Start The Stack

From the repository root:

```bash
cp .env.example .env
```

Set a local `ADMIN_PASSWORD` and replace the other `*_change_me` values in `.env`. Then start and verify the stack:

```bash
docker compose --env-file .env -f compose.yml up -d --build
./scripts/check-local-stack.sh
```

The API Gateway is `http://localhost:8080`. MailHog is `http://localhost:8025`.

## Import And Configure

1. Import both JSON files into Postman.
2. Select **Platform Local - Postman Template** as the active environment.
3. Set `adminPassword` to the same value as `ADMIN_PASSWORD` in the local `.env` file.
4. Set `userEmail` and `userPassword` for a disposable regular user. The password must contain uppercase, lowercase, and numeric characters and be at least eight characters long.
5. Keep `baseUrl=http://localhost:8080` unless the documented Gateway port changes.

Do not export or commit an environment after a run. Postman populates token, user, task, and notification variables with local runtime values.

## Prepare The Regular User

The bootstrap admin is created automatically with email `admin@mail.com` and the configured `ADMIN_PASSWORD`.

A newly registered regular user is disabled until email verification:

1. Run **Login as Admin**.
2. Run **Register Regular User (idempotent setup)**.
3. Open MailHog at `http://localhost:8025` and open the registration email.
4. Copy the `key` query-parameter value from the verification link into `verificationKey`.
5. Run **Verify Regular User Account**.
6. Run **Login as Regular User** to confirm activation.

If an active disposable user already exists, leave `verificationKey` blank. The verification request skips itself, and registration may report that the email already exists. If the existing account is disabled, obtain and submit its valid verification key before running the full collection.

## Run The Collection

After user preparation, use Postman's Collection Runner and run folders in their numbered order. Keep cookie persistence enabled. The scripts also extract `access-token` and `refresh-token` from `Set-Cookie` headers and support body-based token fields if the auth response contract later exposes them.

The collection verifies:

- Gateway health and admin/regular-user login
- regular-user task create, get, update, status change, assignment, and list
- regular-user ownership denial for an admin-owned task as `404 NOT_FOUND`
- administrator visibility across task owners
- creator and administrator delete permissions
- soft-deleted task exclusion from get and list
- assigned-task notification list and get for the admin recipient
- refresh-token cookie rotation

## Kafka Notification Flow

The default Postman flow verifies MVP behavior and does not require Kafka
notification cutover settings. To verify notification creation through Kafka
while keeping the existing REST code in place, enable the local Kafka E2E
environment values first and then run the assigned-task portion of this
collection. See
[Kafka notification E2E verification](../kafka-notification-e2e-verification.md).

## Current Contract Limits

- There is no public logout endpoint. The logout request is disabled and marked pending.
- Notification list/get endpoints are implemented, but notification ownership authorization and mark-as-read are not. The collection filters by the recipient and verifies the returned recipient ID; it does not claim isolation that the backend does not enforce.
- Task assignment notification creation is best-effort. A missing notification fails the collection and should be investigated in task-service and notification-service logs.
- Registration is not fully automatable through the Gateway because account verification requires the key delivered to MailHog.

## Manual Verification Checklist

- [ ] `docker compose ... up -d --build` completes and `./scripts/check-local-stack.sh` passes.
- [ ] Both Postman files import without errors and the local environment is selected.
- [ ] Local admin and disposable-user passwords are filled; no real secrets are stored in repository files.
- [ ] The regular user is active, or registration and MailHog verification are completed first.
- [ ] The collection runs in numbered order with no unexpected assertion failures.
- [ ] The regular user receives `404` for the unrelated admin task.
- [ ] Admin task listing includes tasks from both owners.
- [ ] Deleted task get returns `404` and list excludes the deleted ID.
- [ ] The assignment notification is found for `adminUserId` and can be read by `notificationId`.
- [ ] Refresh returns replacement access and refresh cookies.
- [ ] Exported runtime environments containing tokens or passwords are not committed.
