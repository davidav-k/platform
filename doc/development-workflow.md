# Development Workflow

This repository is being stabilized for the MVP. Keep changes small, explicit, and easy to review.

## Branching

- `main` is the stable integration branch.
- `dev` is the active development integration branch.
- Feature and fix branches should branch from the intended target branch and use short descriptive names, for example `fix/user-service-startup` or `docs/ci-workflow`.
- Open pull requests into `dev` during MVP development unless the change is an urgent repository maintenance fix for `main`.

## Pull Requests

Every pull request should:

- Use the repository pull request template.
- Describe the scope and validation performed.
- Keep unrelated refactoring out of MVP stabilization changes.
- Call out API contract, security, configuration, startup, or documentation impact.
- Include tests or a clear reason why tests are not applicable.

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

Install local pre-commit tooling:

```bash
pipx install pre-commit
pipx install ggshield
ggshield auth login
pre-commit install
```

Run all repository hooks manually:

```bash
pre-commit run --all-files
```

Run Maven tests for each Java service:

```bash
mvn -B -f backend/user-service/pom.xml test
mvn -B -f infrastructure/api-gateway/pom.xml test
mvn -B -f infrastructure/config-server/pom.xml test
mvn -B -f infrastructure/eureka-server/pom.xml test
```

## Secret Handling

- Keep secrets in local `.env`, environment variables, or external secret managers.
- Do not commit `.env`, generated credentials, private keys, JWT tokens, passwords, or API keys.
- Treat values in `.env.example` as local placeholders only. Replace them before running anything outside local development.
- GitGuardian `ggshield` is configured as a pre-commit hook. GitHub secret scanning and push protection should also be enabled in repository settings.

## CI

GitHub Actions runs Maven tests for each existing Maven module on pull requests and pushes to `main` and `dev`.

The repository does not currently have a root Maven aggregator. Add new Maven services to `.github/workflows/maven-tests.yml` when they become buildable modules.
