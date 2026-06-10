# Database Migration Strategy

## Scope

Each persistence service owns its PostgreSQL database and Flyway migrations:

| Service | Default database | Migration location | Baseline |
| --- | --- | --- | --- |
| `user-service` | `users_db` | `backend/user-service/src/main/resources/db/migration` | `V1__user_service_baseline.sql` |
| `task-service` | `tasks_db` | `backend/task-service/src/main/resources/db/migration` | `V1__task_service_baseline.sql` |
| `notification-service` | `notifications_db` | `backend/notification-service/src/main/resources/db/migration` | `V1__notification_service_baseline.sql` |

These migrations are the schema source of truth. Hibernate validates migrated
schemas and must not create or update them. The local Docker Compose stack and
integration tests use PostgreSQL `16.1`.

## Principles

- Every schema change goes through Flyway.
- Entity mappings do not create or update database objects.
- Every schema change requires a new migration.
- Migrations execute automatically when each persistence service starts.
- A clean PostgreSQL database must be sufficient; no manual SQL execution is
  required for a new environment.

## Naming

Use Flyway versioned migration names:

```text
V2__add_user_last_login.sql
V3__create_refresh_token_table.sql
```

Use the next unused integer version and a concise snake-case description.

## Rules

- Never edit a migration after it has been executed in a shared environment.
- Always create a new migration for follow-up changes.
- Keep migrations scoped to the owning microservice.
- Test migrations against PostgreSQL locally before committing.
- Keep Hibernate configured with `spring.jpa.hibernate.ddl-auto=validate`.
- PostgreSQL initialization scripts may create service databases, but must not
  create service-owned tables. Flyway owns those schemas.

## Existing Databases

Each `V1` baseline is intended to create a clean service database. For an
existing populated database created outside Flyway:

1. Back up the database.
2. Compare its schema with the owning service's baseline migration.
3. Apply any required reconciliation SQL as an explicit reviewed operation.
4. Baseline Flyway at version `1`.
5. Start the owning service and confirm Hibernate validation succeeds.

Do not enable automatic `baseline-on-migrate` in application configuration. An
automatic baseline could silently accept an unknown or incomplete schema.

## Rollback Strategy

Use forward fixes. If a deployed migration is incorrect, restore from a tested
database backup when required and add a new versioned migration that corrects
the schema. Do not modify an executed migration. Destructive migrations require
a backup and a reviewed deployment plan before rollout.

## Local Verification

For a clean local environment:

```bash
docker compose --env-file .env -f compose.yml down
rm -rf infrastructure/docker/pgdata
docker compose --env-file .env -f compose.yml up -d --build
docker compose --env-file .env -f compose.yml logs user-service
docker exec tsp_postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
  -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
```

Repository and integration tests also validate migrations against disposable
PostgreSQL 16.1 databases through Testcontainers. See
`doc/development-workflow.md` for Docker Engine compatibility requirements.

Removing `infrastructure/docker/pgdata` deletes local PostgreSQL data. Use that
command only when a clean local database is intended.
