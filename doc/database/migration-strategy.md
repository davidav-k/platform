# Database Migration Strategy

## Scope

`user-service` owns its PostgreSQL schema. Flyway migrations under
`backend/user-service/src/main/resources/db/migration` are the source of truth.
Hibernate validates the migrated schema and must not create or update it.
The local Docker Compose stack uses PostgreSQL `16.1`.

The baseline migration is `V1__user_service_baseline.sql`. It reproduces the
current user-service schema and the existing local bootstrap seed data.

## Principles

- Every schema change goes through Flyway.
- Entity mappings do not create or update database objects.
- Every schema change requires a new migration.
- Migrations execute automatically when `user-service` starts.
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
- Do not reintroduce SQL files under PostgreSQL's
  `/docker-entrypoint-initdb.d` path for the user-service schema.

## Existing Databases

`V1__user_service_baseline.sql` is intended to create a clean database. For an
existing populated database created by the retired Docker initialization SQL:

1. Back up the database.
2. Compare its schema with `V1__user_service_baseline.sql`.
3. Apply any required reconciliation SQL as an explicit reviewed operation.
4. Baseline Flyway at version `1`.
5. Start `user-service` and confirm Hibernate validation succeeds.

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

Removing `infrastructure/docker/pgdata` deletes local PostgreSQL data. Use that
command only when a clean local database is intended.
