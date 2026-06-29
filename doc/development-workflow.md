# Development Workflow

This repository is being stabilized for the MVP. Keep changes small, explicit, and easy to review.

## Branching

- `main` is the stable integration branch.
- `develop` is the active development integration branch.
- Feature and fix branches should branch from the intended target branch and use short descriptive names, for example `fix/user-service-startup` or `docs/ci-workflow`.
- Open pull requests into `develop` during MVP development unless the change is an urgent repository maintenance fix for `main`.

## Local Setup

Create a local environment file from the example:

```bash
cp .env.example .env
```

Replace all placeholder values in `.env`. Do not commit `.env`.

Start the local stack from the repository root:

```bash
docker compose --env-file .env -f compose.yml up -d --build
./scripts/check-local-stack.sh
```

Stop the local stack:

```bash
docker compose --env-file .env -f compose.yml down
```

## Local Quality Gates


Run all repository hooks manually:

```bash
pre-commit run --all-files
```

Run Maven tests for each Java service:

```bash
mvn -B -f backend/user-service/pom.xml test
mvn -B -f backend/task-service/pom.xml test
mvn -B -f backend/notification-service/pom.xml test
mvn -B -f infrastructure/api-gateway/pom.xml test
mvn -B -f infrastructure/config-server/pom.xml test
mvn -B -f infrastructure/eureka-server/pom.xml test
```
